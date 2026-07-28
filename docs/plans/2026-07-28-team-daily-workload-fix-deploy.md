# 团队日报工时缺陷修复 — 上线方案

**Issue**：[#5](https://github.com/cuitaocrazy/newpm/issues/5)　**日期**：2026-07-28　**分支**：`fix/team-daily-workload-defects`

业务侧影响说明见 `docs/pm/2026-07-28-项目实际人天修正说明.md`（可直接发业务方）。

---

## 1. 修复内容

| # | 缺陷 | 修复 |
|---|---|---|
| ① | 项目一旦建任务，`actual_workload` 只按 `SUM(pm_task)` 汇总，**建任务前直挂父项目的工时被永久抹掉**（生产 22 项目 / 4341.5h / 542.69 人天） | `DailyReportServiceImpl` 两处 `affectedProjectIds.remove(parentProjectId)` → `addAll(...)`，父项目统一按明细全量汇总 |
| ② | `pm_project_member` 缺唯一约束，重复活跃行使日历工时与个人人天**成倍虚增** | 加 `uk_project_user` 唯一索引 + `batchInsert` 加 `ON DUPLICATE KEY UPDATE`（并发幂等）+ SQL 侧 `DISTINCT` 兜底 |
| ③ | 已离场填报人的工时计入实际人天，但**页面一行都不显示**，个人人天与实际人天对不上账（11 对 / 777h） | `selectTeamMonthlyRaw` 成员来源改为「在册 `UNION` 本月有工时的离场者」，输出 `isFormer`；前端灰字 +「已离场」标签 + 第三条图例 |
| ④ | `updateProject` 可直接改 `adjust_workload`，**绕过补正接口的审计留痕** | 从 `updateProject` 的 `<set>` 中移除该字段 |
| ⑤ | 离场成员被重新加回项目时**不会被激活**，`is_active` 永久停在 `'0'` | `syncMembers` 差集基准改为「在册成员」；移出改为软离场（保留行 + `leave_date`），加回时经 `ON DUPLICATE KEY UPDATE` 激活 |

**防复发**：`TaskMapper.sumActualWorkloadByProjectId` 加了警示 javadoc；新增回归测试 `saveDailyReport_projectWithTask_keepsHoursDirectlyOnParent`，一旦有人改回旧口径立即失败。

---

## 2. 执行顺序（不可调换）

```
备份 → 阶段1 SQL(schema) → 部署代码 → 阶段2 SQL(data) → 验证
```

### 为什么是这个顺序

**为什么 schema 必须先于代码**
新版 `syncMembers` 依赖 `batchInsert` 的 `ON DUPLICATE KEY UPDATE` 激活回归成员，而该子句**只在唯一索引存在时才触发**。若先部署代码、后加索引，回归成员会被插入第二行造成重复，之后再加索引会直接失败。

反向兼容性已确认：加索引后、新代码上线前，**旧版 `syncMembers` 不会触发唯一键冲突**——它以「表内全部行」为基准算差集，回归成员不进 `toAdd`，不会 insert。唯一冲突场景是并发重复提交，此时保存失败并回滚，属正确的拒绝。

**为什么 data 必须后于代码**
旧代码每次保存涉及任务的日报，都会用 `SUM(pm_task)` 覆盖父项目工时。若先重算、后部署，期间任何一次日报保存都会把刚修正好的值再次抹掉。

---

## 3. 分步执行清单

### Step 0 — 备份生产数据库

```bash
ssh k3s001 "sudo /usr/local/bin/backup-newpm-db.sh"
ssh k3s001 "ls -lah /backup/newpm-mysql/ ; sudo /usr/local/bin/ossutil ls oss://yada-newpm-backup/newpm-mysql/ | tail -3"
```

**验证点**：备份文件生成且已上传 OSS（纯 OSS 模式下本地不保留，以 OSS 列表为准）。
**不通过则中止。**

### Step 1 — 阶段1 SQL：清理重复行 + 补历史成员行 + 加唯一索引

```bash
cat pm-sql/fix_team_daily_workload_1_schema_20260728.sql | ssh k3s001 \
  "kubectl exec -i mysql-0 -n newpm -- mysql -u root -p<数据库密码> --default-character-set=utf8mb4 ry-vue -t"
```

**验证点**（三项都必须满足）：
- `Part 1 已删除重复成员行：2`（项目 308 的 member_id 7243、7244）
- `Part 1.5 已补回历史成员行：9`（详见下方说明）
- 索引列表中出现 `uk_project_user`（`NON_UNIQUE=0`，列为 `project_id,user_id`），且 `仍重复的成员组数 = 0`

**Part 1.5 是安全修复的配套动作**，不可省略。团队日报的「离场成员」分支带有安全约束
「必须曾是该项目成员」（见 §7）。而旧 `syncMembers` 移出成员时是**硬删**，抹掉了
「某人曾参与某项目」的事实——生产有 9 对 (项目, 人) / 494 小时属于这种情况。
若不补回这些成员行，这批历史离场者在团队日报中仍不可见，缺陷 ③ 只修了一半。
补回的行为 `is_active='0'`（离场态），不会进入任何「在册成员」查询。

### Step 2 — 部署代码

```bash
# 在 worktree 内提交
cd .claude/worktrees/team-daily-workload-fix
git add <本特性文件>
git commit
# 合并回 main 并推送（触发 GitHub Actions → 构建镜像 → kubectl rollout restart）
cd /Users/kongli/ws-claude/PM/newpm
git merge fix/team-daily-workload-defects
git push origin main
```

**验证点**：
```bash
gh run list --limit 3                                     # CI 成功
ssh k3s001 "kubectl rollout status deployment/ruoyi-app -n newpm --timeout=300s"
ssh k3s001 "kubectl get pods -n newpm | grep ruoyi-app"   # 新 pod Running
```

冒烟：登录 → 日报管理/团队日报 任选部门查询能出数据。

### Step 3 — 阶段2 SQL：重算 22 个项目工时

```bash
cat pm-sql/fix_team_daily_workload_2_data_20260728.sql | ssh k3s001 \
  "kubectl exec -i mysql-0 -n newpm -- mysql -u root -p<数据库密码> --default-character-set=utf8mb4 ry-vue -t"
```

**验证点**：
- 执行前对账显示 **22 个项目 / 4341.50 小时 / 542.69 人天**（与核查一致；若数字变大属正常——期间可能又有项目新建首个任务）
- `已重算项目数 = 22`
- `仍不一致的项目数 = 0`
- 抽查项目 69 → `123.00`（15.375 人天）、67 → `21.00`（2.625 人天），不再是 0

### Step 4 — 生产功能验证

| 验证项 | 位置 | 预期 |
|---|---|---|
| ① 工时找回 | 项目管理→项目列表，搜「24年专项分期监管及审计整改项目」 | 实际人天 **15.38d**（原 0.00d） |
| ① 工时找回 | 同上，搜「客户经理二维码需求」 | 实际人天 **2.63d**（原 0.00d） |
| ② 不再翻倍 | 日报管理→团队日报，开发二组 / 2026-07，看项目308 的张震宇、李梁羽 | 日历格 8h（原显示 16h） |
| ③ 已离场行 | 团队日报，机动组 / 2026-03 | 于鹤铭(已离职) 灰字 +「已离场」标签，工时 80h |
| ③ 图例 | 团队日报页顶部 | 出现第三条「人员为灰色 = …」 |
| ④ 后门已堵 | 项目管理→编辑任一项目→保存 | 调整人天不变 |

### Step 5 — 收尾

- 关闭 Issue #5
- 把 `docs/pm/2026-07-28-项目实际人天修正说明.md` 发各组负责人复核（重点：开发二组 289.06 人天）
- 删除特性分支与 worktree

---

## 4. 回滚方案

| 场景 | 操作 | 说明 |
|---|---|---|
| 代码有问题 | `ssh k3s001 "kubectl rollout undo deployment/ruoyi-app -n newpm"` | 秒级回退上一版镜像 |
| 代码需彻底撤回 | `git revert <merge-commit>` + push | 触发 CI 重新部署 |
| 唯一索引需撤 | `ALTER TABLE pm_project_member DROP INDEX uk_project_user;` | 无损；但需同时回滚代码，否则新 `syncMembers` 会产生重复行 |
| 数据重算需撤 | 从 Step 0 的备份恢复 | **会丢失备份后产生的全部业务数据**，代价高 |

**重要**：Step 3 的重算结果本身是**正确值**。即使代码回滚，也**不需要**回滚这批数据——只是旧代码会重新把它们抹掉（回到修复前状态），不会产生错误的新值。因此实际上几乎不会用到最后一行的数据恢复。

**回滚窗口**：Step 1（索引）与 Step 2（代码）之间若中止，需先 drop 索引再放弃，否则旧代码在并发提交时会报错。

---

## 5. 已完成的验证

| 层级 | 内容 | 结果 |
|---|---|---|
| 单元测试 | `ruoyi-project` 全量 | **179/179 通过** |
| 回归有效性 | 故意把 ① 的修复回退、重新打包 | 测试**精确失败**：`Expected: 12, Received: 4`，复现出 8 小时被抹掉；恢复后通过 |
| E2E | 新增 `tests/e2e-team-daily-workload.spec.js`（覆盖 ①②③④⑤ + 越权安全回归） | **9/9 通过** |
| E2E 回归 | 既有 `e2e-daily-report`(10) + `project-create-audit-fields`(3) | 全部通过 |
| UI | 团队日报「已离场」灰行 + 标签 + 图例 | 截图确认，92px 列宽无溢出 |
| 迁移脚本 | 在**生产同等规模数据副本**上演练 | 17→0 不一致；重复执行零变更（幂等） |
| 本地全流程演练 | 按生产顺序（schema→部署→data）在**生产镜像库**上跑通 | 阶段1 删 2 补 9 加索引 → e2e 9/9 → 阶段2 重算 22 → 不一致 0 |
| **安全审查** | 对全部改动做专项审查（SQL 注入 / 越权 / XSS / 数据泄露） | 发现并已修复 1 个 MEDIUM 越权（见 §7）；其余攻击面判定安全 |
| **越权攻击实证** | 按审查给出的利用路径实际打一次 | 注入工时后**未**获得越权可见性；合法离场者仍正常显示 |

---

## 6. 风险与注意事项

1. **数字变大会影响引用方**：项目列表、团队日报、项目人天统计、收入确认、项目阶段变更、合同详情等 6 处显示的实际人天都会变。已在业务说明文档中列明并提示复核。

2. **缺陷至今仍在发生**：最近一例是项目 106（2026-07-01 建首个任务，当场丢 95h）。**每延迟一天上线，就可能再新增受害项目**。若 Step 3 执行时对账数字大于 22 个 / 4341.5h，属正常，直接按实际值执行即可。

3. **不在本次范围内**：44 条历史脏明细（216.5 小时，日期集中在 2026-03）指向已硬删项目或迁移前旧子项目 id，两边都统计不到。需业务确认归属，另行处理。

4. **`adjust_workload` 无审计的历史遗留**：项目 19 的 109 人天补正在 `pm_workload_correct_log` 中无记录（绕过补正接口落库）。本次堵住了后门，但这笔历史记录仍无法追溯来源，需业务确认其合法性。

5. **执行窗口**：Step 1 与 Step 3 均为秒级完成（22 行 UPDATE、2 行 DELETE、1 个索引）。主要耗时在 Step 2 的 CI 构建（约 5–10 分钟）。建议在业务低峰期执行。

---

## 7. 安全审查结论（含一处已修复的越权）

本次改动经专项安全审查，发现并修复 **1 个 MEDIUM 越权漏洞——由缺陷 ③ 的修复自身引入**。

### 漏洞：离场成员分支的准入条件可被自助伪造

缺陷 ③ 的修复把团队日报的成员来源从「在册成员」放宽为「在册成员 UNION 本月有工时的离场者」。
问题在于**新分支的准入条件最初完全由请求者自己可写的数据决定**：

- 分支只要求 `pm_daily_report_detail` 中存在一行该项目、该用户、本月的 `work` 明细；
- 而 `saveDailyReport` **不校验 `detail.projectId` 是否属于填报人**（项目归属校验只在前端下拉框），
  `pm_daily_report_detail.project_id` 也无外键约束。

`@DataScope` 拦不住这条路：其 `deptAlias = "d"` 绑定的是 `d.dept_id = u.dept_id`，即
**成员本人的部门**，而非项目所属部门（对比 `ProjectStatsMapper.xml:11` 绑的是 `p.project_dept`）；
外层唯一与项目部门相关的 `deptId` 过滤是 `<if test="deptId != null">` 的**可选参数**，
调用 API 时省略即可。

**攻击路径**：任意持有「填日报 + 团队日报」权限组合的账号（组长/项目经理常见组合），
批量给任意 `projectId` 提交 0.01 小时工时，即可自助获得「离场成员」身份，
再查 `teamMonthly` 且不传 `deptId`，跨部门读出全公司项目的
`projectBudget` / `contractAmount` / `confirmAmount` / `revenueConfirmStatus`。

### 修复

`DailyReportMapper.xml` 的离场分支增加**「曾是该项目成员」**约束：

```sql
AND EXISTS (SELECT 1 FROM pm_project_member pm3
            WHERE pm3.project_id = rd.project_id AND pm3.user_id = r.user_id)
```

成员行只能由持项目编辑权限者写入，**攻击者无法自助伪造**，门槛因此不可绕过。

配套 `Part 1.5` 补回被旧 `syncMembers` 硬删的 9 对历史成员行——否则这批合法离场者
会因缺少成员行而仍不可见，使缺陷 ③ 只修一半。硬删这个源头已随缺陷 ⑤ 改为软离场，
今后不再产生此类数据。

### 实证

在生产镜像库上按上述路径实际攻击：给 3 个 admin 从未参与的项目注入工时后，
admin **均未**作为离场成员出现在这些项目下；同时项目 327 的合法离场者匡杰
仍以 `isFormer=true` / 61h 正常显示。**安全门槛立住，业务效果无损。**

已新增 e2e 用例
`安全回归：给「从未参与过的项目」注入工时，不得因此出现在团队日报中`——
一旦有人删掉该 `EXISTS` 约束，测试立即失败。

### 未修复的既有问题（不在本次范围）

1. **`saveDailyReport` 不校验 `detail.projectId` 归属**：任何账号可给任意项目写入工时，
   进而污染该项目的 `actual_workload`。这是**写入侧的既有缺陷**，本次仅收口了读取侧的放大。
   建议后续在 `saveDailyReport` 中校验 `projectId ∈ selectMyProjects()`。
2. **`selectTeamMonthlyRaw` 的 `@DataScope` 绑成员部门而非项目部门**，与
   `ProjectStatsMapper` 的口径不一致。属既有设计，改动会影响现有用户的可见范围，
   需业务确认后另行处理。

### 审查通过的其他攻击面

新增 SQL 全部使用 `#{}` 参数化，无 `${}` 拼接；`UNION` 两分支互斥不产生重复行；
`isFormer` 不构成 PII 泄露（同行本就返回姓名/部门/工时）；前端仅 `{{ }}` 插值无 `v-html`；
`deactivateByProjectIdAndUserIds` 与 `batchInsert` 的作用域均受 `project_id + user_id` 限定；
`updateProject` 移除 `adjust_workload` 属纯收紧；本次未新增或放宽任何接口权限。
