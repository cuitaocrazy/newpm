# Phase 1 Data Model: 日报保存的工时保护与项目归属校验

**Feature**: 015-daily-report-ownership-check
**Date**: 2026-07-30

**本特性无 schema 变更。** 下述为涉及表的既有结构与新增的读取／删除口径。

## 1. 涉及的既有表

### `pm_daily_report_detail`（日报明细，硬删除表）

| 字段 | 本特性中的作用 |
|---|---|
| `detail_id` | 主键。本特性不向前端暴露 |
| `report_id` | 所属日报（某人某天）。删除范围的第一层限定 |
| `project_id` | 所属项目。**判定作用范围的关键字段**：`NULL` 表示非项目工时 |
| `sub_project_id` | 所挂任务（FK → `pm_task.task_id`），可空。任务归属校验的对象 |
| `entry_type` | `work` = 项目工时（`project_id` 必有值）；其余 7 种为假期类（`project_id` 恒为 `NULL`） |
| `work_hours` | 工时数 |
| `work_content` / `work_category` | 保留明细时须原样保持（FR-003） |
| `del_flag` | 恒为 `'0'`——该表用硬删除，不走软删 |

**实测取值分布**（生产 2026-07-30）：

```
work 15661 条（project_id 全部有值）
comp 911 / leave 92 / annual 128 / marriage 40 / bereavement 9 / maternity 53 / '8' 49
  └ 上述 7 种共 1282 条，project_id 全部为 NULL
```

### `pm_project_member`（项目成员关系）

| 字段 | 本特性中的作用 |
|---|---|
| `project_id` + `user_id` | **「曾是成员」的判据**。存在行即为凭据，不限状态 |
| `is_active` | `'1'` 在册 / `'0'` 已离场。**归属校验不看此字段**（离场者仍可维护历史工时） |
| `del_flag` | 同上，归属校验不看 |

> 该表已于 Issue #5 加上 `uk_project_user (project_id, user_id)` 唯一索引，
> 故「曾是成员」查询最多命中一行，无重复风险。

### `pm_project`（项目）

| 字段 | 本特性中的作用 |
|---|---|
| `project_id` | 关联键 |
| `project_name` | 拒绝提示中呈现给填报人（FR-008，不暴露 ID） |
| `project_stage` | `'11'` = 项目结项。**结项校验的唯一判据** |
| `approval_status` / `project_status` | **本特性不作为判据**（沿用现状，见 spec Assumptions） |
| `actual_workload` | 由工时重算写入；本特性须保证其与明细汇总严格相等（SC-008） |

### `pm_task`（任务）

| 字段 | 本特性中的作用 |
|---|---|
| `task_id` | 对应明细的 `sub_project_id` |
| `project_id` | **任务归属校验**：须等于明细声明的 `project_id` |

## 2. 核心概念：作用范围（Submission Scope）

本特性的全部行为围绕一个概念展开：

> **作用范围** = 本次保存请求「有能力表达」的明细集合
> = 填报人当前可填项目的工时 ∪ 全部非项目工时

形式化：

```
visibleProjectIds := { p.project_id | p ∈ selectMyProjects(当前用户) }

明细 d ∈ 作用范围  ⟺  d.project_id IS NULL  OR  d.project_id ∈ visibleProjectIds
```

**范围内**的既有明细：按提交内容替换（未提交 = 填报人主动删除 → 删）
**范围外**的既有明细：原样保留（未提交 ≠ 删除，因为填报人根本看不到它）

`selectMyProjects()` 的现有口径（`ProjectMapper.selectProjectsByUserId`）：

```sql
where p.del_flag = '0'
  and p.approval_status = '1'
  and p.project_status = '0'
  and (p.project_stage is null or p.project_stage != '11')   -- 排除已结项
  and ( p.project_manager_id = #{userId}
        or p.market_manager_id = #{userId}
        or p.team_leader_id = #{userId}
        or FIND_IN_SET(#{userId}, p.participants)
        or p.project_id in (select pm.project_id from pm_project_member pm
                            where pm.user_id = #{userId} and pm.is_active='1' and pm.del_flag='0') )
```

> ⚠️ 该口径**只用于界定作用范围**，**不得**用作归属校验的判据——它混入了项目生命周期状态，
> 会误拒 126 组历史组合（见 research.md Decision 3）。两个用途必须严格区分。

## 3. 校验规则

对提交明细中每条 `entry_type='work'` 的记录（非 work 记录跳过全部校验）：

| 编号 | 规则 | 判据 | 违规处理 |
|---|---|---|---|
| V1 | 填报人曾参与该项目 | `pm_project_member` 存在 `(project_id, user_id)` 行（不限状态） | 拒绝整次保存，提示「项目《名称》不在您参与的项目范围内」 |
| V2 | 项目未结项 | `pm_project.project_stage != '11'` | 拒绝整次保存，提示「项目《名称》已结项，不能新增或修改其工时」 |
| V3 | 任务归属正确 | `sub_project_id` 为空，或 `pm_task.project_id = 明细.project_id` | 拒绝整次保存，提示「任务与所选项目不匹配」 |

**批量化**：取提交明细的 `projectId` 去重集（实测最多 6 个）与 `subProjectId` 去重集，
三次批量查询完成全部判定，无 N+1。

## 4. 保存流程（改造后）

```
输入：report（含 reportDate、detailList）
      当前用户 userId

1. 白名单校验（既有，不变）
      isInWhitelist(userId) → 抛 ServiceException

2. 【新增】提交内容校验
   2.1 workDetails := detailList 中 entry_type='work' 且 projectId≠null 的记录
   2.2 projectIds  := workDetails 的 projectId 去重集
       若非空：
         a) 查 selectProjectStatesIn(projectIds)  → {id: (name, stage)}
            · 缺失的 id（项目不存在/已删）→ V1 违规（无成员关系）
            · stage='11' 的 → V2 违规
         b) 查 selectEverMemberProjectIds(userId, projectIds) → 合法子集
            · projectIds - 合法子集 → V1 违规
   2.3 taskIds := workDetails 中非空 subProjectId 去重集
       若非空：查 selectTaskProjectPairs(taskIds) → {taskId: projectId}
            · 映射缺失 或 映射值 ≠ 明细声明的 projectId → V3 违规
   2.4 任一违规 → 抛 ServiceException（含项目名称与原因），事务未开始写入，
       实际人天与既有明细均不受影响（FR-009）

3. 计算作用范围
      visibleProjectIds := selectMyProjects(userId) 的 projectId 集合

4. 定位/创建日报主记录（既有逻辑）
      existingReportId := selectReportIdByUserAndDate(userId, dateStr)

5. 【改造】替换明细
   若 existingReportId 存在：
     5.1 oldDetails := selectByReportId(existingReportId)       ← 既有调用，用于 rollup
     5.2 deleteByReportIdInScope(existingReportId, visibleProjectIds)   ← 【改造点】
         原为 deleteByReportId(existingReportId)（删该日全部）
     5.3 batchInsert(detailList)
   否则：insertDailyReport + batchInsert（既有逻辑，无既有明细可丢）

6. 工时重算（既有逻辑 + 微调）
      affectedProjectIds := 提交明细的 projectId
                          ∪ oldDetails 的 projectId              （既有）
                          ∪ 受影响任务的父项目                    （既有，Issue #5 已改为 addAll）
      对每个 projectId：actual_workload := SUM(明细 work_hours)   （Issue #5 口径，不变）
```

**关键点**：步骤 5.2 是唯一的行为改变点。步骤 6 的重算口径已于 Issue #5 改为「按明细全量汇总」，
因此**被保留的明细会自动被算进其项目的实际人天**，无需额外处理——这也是保留语义能与
实际人天保持一致（SC-008）的原因。

## 4bis. 删除整条日报的流程（改造后）

```
输入：reportIds[]，当前用户 userId

1. 收集受影响范围（必须在删除【之前】完成——既有逻辑已如此）
     对每个 reportId：oldDetails := selectByReportId(reportId)
     affectedProjectIds  := oldDetails 的 projectId
     affectedSubProjectIds := oldDetails 的 subProjectId

2. 计算作用范围
     visibleProjectIds := selectMyProjects(userId) 的 projectId 集合

3. 【改造】按范围删除明细
     deleteByReportIdInScope(reportId, visibleProjectIds)
     原为 deleteByReportIds(reportIds)（删全部）

4. 【新增】判断是否有残留，决定主记录去留
     remaining := countByReportId(reportId)
     若 remaining > 0：
        · 【不删主记录】——否则被保留的明细将无主记录可归属
        · updateTotalWorkHours(reportId, 剩余 work 类型明细的工时之和)
     若 remaining = 0：
        · 软删主记录（既有行为：deleteDailyReportByIds）

5. 工时重算（既有逻辑，不变）
     对 affectedSubProjectIds 重算 pm_task.actual_workload
     对 affectedProjectIds（∪ 受影响任务的父项目）重算 pm_project.actual_workload
```

**为什么第 4 步不能省**：`pm_daily_report_detail` 靠 `report_id` 归属主记录，而除
`selectByReportId` 之外的查询都要经 `pm_daily_report r ... WHERE r.del_flag='0'` 过滤。
若主记录被软删而明细仍在，这些明细将无法通过任何业务查询到达——等于换一种方式把工时弄丢
（SC-010 不成立）。

**汇总工时重算口径**：`SUM(work_hours) WHERE report_id = ? AND entry_type='work' AND del_flag='0'`
（与 `saveDailyReport` 计算 `totalWorkHours` 的口径一致：只累加 work 类型）。

## 5. 状态迁移：一条既有明细在保存后的归宿

| 该明细在作用范围内？ | 出现在本次提交中？ | 结果 | 对应需求 |
|---|---|---|---|
| 是（可见） | 是 | 按提交值更新（先删后插） | 正常编辑 |
| 是（可见） | 否 | **删除** | FR-002（填报人主动清零） |
| 否（不可见） | 否 | **原样保留** | FR-001（防丢失，核心） |
| 否（不可见） | 是 | 不可能通过前端发生；若经 API 直接提交，则该项目必已结项 → 被 V2 拒绝 | FR-010 / FR-011 |

第 3 行是本特性要修复的缺陷；第 4 行说明「保护」与「拒绝」两条规则各管一侧、不会互相抵消
（spec Edge Cases 的显式要求）。

### 5bis. 删除整条日报时的归宿

| 该明细在作用范围内？ | 结果 | 主记录 |
|---|---|---|
| 是（可见） | **删除** | — |
| 否（不可见） | **原样保留** | **必须保留**并重算当日汇总工时 |
| （该日报全部明细都可见） | 全部删除 | 软删主记录（既有行为） |

对应 FR-013 / FR-014 / SC-009 / SC-010。

## 6. 新增的数据访问方法

| Mapper | 方法 | 签名与语义 |
|---|---|---|
| `DailyReportDetailMapper` | `deleteByReportIdInScope` | `(reportId, Set<Long> visibleProjectIds)` → 删除该日报中 `project_id IS NULL OR project_id IN (集合)` 的明细。集合为空时退化为仅删非项目工时（`<if>` 守卫） |
| `ProjectMemberMapper` | `selectEverMemberProjectIds` | `(userId, Collection<Long> projectIds)` → 返回该用户曾参与的项目 id 子集（**不限** `is_active` / `del_flag`） |
| `ProjectMapper` | `selectProjectStatesIn` | `(Collection<Long> projectIds)` → 返回 `[{projectId, projectName, projectStage}]`，供 V1 缺失判定、V2 结项判定与错误提示取名 |
| `TaskMapper` | `selectTaskProjectPairs` | `(Collection<Long> taskIds)` → 返回 `[{taskId, projectId}]` |
| `DailyReportDetailMapper` | `countByReportId` | `(reportId)` → 该日报剩余明细条数，用于判断主记录去留 |
| `DailyReportDetailMapper` | `sumWorkHoursByReportId` | `(reportId)` → 该日报剩余 work 类型明细的工时之和，用于重算当日汇总 |
| `DailyReportMapper` | `updateTotalWorkHours` | `(reportId, hours)` → 更新主记录的当日汇总工时（须带 `update_time = update_time` 以免触碰审计字段） |

全部为 PM 表内查询，不跨字符集，无需 `COLLATE`（宪法 V）。
