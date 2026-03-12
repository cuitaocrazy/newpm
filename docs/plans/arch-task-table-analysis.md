# 架构分析：任务管理独立建表 vs 复用 pm_project 自引用

**分析日期：** 2026-03-11
**背景：** 任务管理改造期间，评估是否应将任务从 pm_project 自引用结构中分离为独立表

---

## 现状描述

当前任务（子项目）通过 `pm_project` 表自引用实现：
- `project_level = 0`：主项目
- `project_level = 1`：任务（子项目）
- `parent_id`：任务指向所属主项目

所有任务记录与主项目记录共用同一张表，通过 `project_level` 字段区分。

---

## 现有实现规模（评估迁移成本的基础）

| 维度 | 数量 |
|---|---|
| ProjectMapper.xml 中 task 相关查询 | 13 个 |
| 主项目查询中需要的 project_level 守卫 | 每个查询都有 `(project_level IS NULL OR project_level = 0)` |
| DailyReportMapper 中对 pm_project 的 task JOIN | 直接通过 `sub_project_id → pm_project.project_id` |
| 专属 subproject Vue 页面 | 4 个（index/add/edit/detail） |
| 其他引用 task 的模块 | ~19 个（日报、收入、合同、支付等） |
| Java 核心文件 | 5 个（domain/mapper/service/impl/controller） |

---

## 独立建表的优点

| 问题 | 改善效果 |
|---|---|
| 任务专属字段（batch_id, production_year, bank_demand_no, software_demand_no, product, task_code 等）加在 pm_project 上，污染主项目结构 | 任务字段单独存储，主项目表保持干净 |
| 每个主项目查询都需要 `project_level` 过滤守卫，认知负担高，容易遗漏 | 两张表物理隔离，查哪张就是哪张，无需守卫 |
| 任务与项目业务逻辑持续分叉，未来扩展成本高 | 独立表支持任务独立演进（如独立审批流、独立成员管理） |
| Schema 变更时互相影响 | 任务 schema 变更不影响主项目 |

---

## 独立建表的代价

| 代价 | 影响评估 |
|---|---|
| **字段重叠 ~60-70%**：project_name、project_dept、project_stage、project_status、project_manager_id、start_date、end_date、estimated_workload、actual_workload、remark 等 | 独立表等于复制大量结构，不简洁 |
| **日报 FK 需要变更**：DailyReportMapper 的 `sub_project_id → pm_project.project_id`，迁移后 FK 指向改变，日报写入/查询/工时归集全部要改 | 影响核心业务功能 |
| **迁移范围极大**：5 个 Java 核心文件 + 20+ Vue 文件 + mapper XML 全部改写 + 存量数据迁移 | 本次改造计划工作量至少 ×3 |
| **当前无冲突点**：任务无独立审批流、无独立成员管理（继承父项目）、无独立收入确认 | 分离的核心驱动力暂不存在 |

---

## 结论与建议

### 当前决策：保持现有自引用结构，继续推进改造计划

**理由：**
1. 自引用模式不是反模式，许多成熟系统（Azure DevOps、Jira）都用类型字段区分层级
2. 字段重叠度高（60-70%），分离收益有限
3. 日报深度耦合，迁移成本极高
4. 任务尚无独立生命周期管理需求，分离驱动力不足

### 未来触发分离的信号

当出现以下情况时，应重新评估独立建表：

- [ ] 任务需要自己的审批流（独立于项目审批）
- [ ] 任务需要独立的成员管理（不再继承父项目成员）
- [ ] 任务专属字段数量**超过**与项目共享的字段数量
- [ ] 任务需要多层级嵌套（任务下再有子任务）
- [ ] 出现任务需要关联到多个主项目的需求

### 迁移时的注意事项（供未来参考）

若未来决定分离，需要关注：
1. **DailyReportDetail.sub_project_id** FK 需同步迁移，日报历史数据不能丢
2. **selectSubProjectOptions** 接口（日报写入时调用）需切换数据源
3. **pm_project 主项目查询**无需再加 `project_level` 守卫（可以清理）
4. 建议先建新表 + 双写，再逐步切流，最后删旧数据，而非一次性迁移
