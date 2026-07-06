# Data Model: 日报手机端 H5 适配（一期）

**Date**: 2026-07-04
**Feature**: 014-daily-report-mobile

> 本特性**零新增实体、零 schema 变更**。本文档定义的是移动端**视图模型（view model）**与既有实体/接口 payload 的映射关系——这是双端一致性（SC-002）的唯一基准，移动端实现必须逐字段对齐。
> 基准来源：`ruoyi-ui/src/views/project/dailyReport/write.vue` 的 `handleSave()` / `loadDayReport()` 现行代码（非文档记忆）。

## 1. 既有持久化实体（只读参考，不改）

| 实体 | 表 | 移动端用途 |
|------|-----|-----------|
| DailyReport | `pm_daily_report` | 日报主表；`reportDate` + 当前用户唯一，保存为按日覆盖更新 |
| DailyReportDetail | `pm_daily_report_detail` | 明细行；`entryType` 区分 work/leave/comp/annual；task 条目 `sub_project_id` 存 `pm_task.task_id` |
| 字典 `sys_rbtype` | `sys_dict_data` | 条目类型（work=项目工时 / leave=请假 / comp=倒休 / annual=年假）；移动端假期 Picker 数据源（过滤掉 work） |
| 字典 `sys_gzlb` | `sys_dict_data` | 工作任务类别；工时>0 时必填、**可多选**（逗号拼接存储） |
| Whitelist | `pm_daily_report_whitelist` | 豁免名单；经**独立接口** `GET /project/whitelist/checkSelf`（`src/api/project/whitelist.js` 的 `checkSelfInWhitelist()`）判定，`data === true` 时移动端整页显示豁免提示并**短路**后续数据加载（对齐桌面 write.vue onMounted 行为） |

## 2. 移动端视图模型

### 2.1 MobileProjectItem（项目卡片，源：`GET /project/dailyReport/myProjects`）

```text
projectId          number    项目 ID
projectName        string    项目名（卡片标题；关键字过滤字段）
projectCode        string    项目编码
projectStage       string    项目阶段值（保存时回传）
projectStageName   string    阶段名（卡片右上角 tag）
projectManagerName string    项目经理名（展示）
revenueConfirmYear string?   确认年份（展示 tag，可选）
hasSubProject      boolean   true → 卡片为折叠模式，展开后按任务分行
--- 普通项目（hasSubProject=false）持有 ---
workHours          number    0–24，步进 0.5（Stepper）
workContent        string    工作内容（textarea）
workCategory       string[]  工作类别多选（保存时 join(',')）
--- 含子任务项目（hasSubProject=true）持有 ---
taskRows           TaskRow[] | null   null=未加载（展开时经 GET /project/task/options 懒加载）
```

### 2.2 TaskRow（任务行，源：`GET /project/task/options?projectId=`）

```text
subProjectId   number    任务 ID（pm_task.task_id；接口字段沿用旧名）
taskName       string    任务名
workHours      number    0–24，步进 0.5
workContent    string    工作内容
workCategory   string[]  工作类别多选（工时>0 必填）
```

### 2.3 LeaveItem（假期条目）

```text
entryType    'leave' | 'comp' | 'annual'   类型（sys_rbtype 非 work 项，Picker）
leaveHours   number                        小时数（>0 才参与保存）
remark       string                        备注（可选）
```

## 3. 保存 payload 组装规则（与桌面 handleSave 逐条对齐）

`POST /project/dailyReport`，body：

```json
{
  "reportDate": "2026-07-04",
  "detailList": [ ...workDetails, ...leaveDetails ]
}
```

**workDetail（含子任务项目，每个工时>0 的任务行一条）**：

```json
{
  "projectId": 123, "projectStage": "5",
  "workHours": 4, "workContent": "…", "entryType": "work",
  "subProjectId": 456, "workCategory": "1,3"
}
```

**workDetail（普通项目）**：同上但 `subProjectId: null`；仅当 `workHours > 0` **且** `workContent` 非空白时生成。

**leaveDetail**：

```json
{
  "projectId": null, "workHours": 8, "workContent": "",
  "entryType": "annual", "leaveHours": 8, "remark": "…"
}
```

### 校验规则（保存前，与桌面一致）

| # | 规则 | 提示 |
|---|------|------|
| V1 | 含子任务项目已展开但 taskRows 尚未加载完成 → 阻止保存 | "任务列表尚未加载，请稍后再试" |
| V2 | 任一工时>0 的行（任务行或普通项目）`workCategory` 为空 → 阻止保存 | 分支文案与桌面一致——任务行：`项目"X"的任务"Y"工时已填写，请选择工作任务类别`；普通项目：`项目"X"工时已填写，请选择工作任务类型`（注意"类别/类型"二词桌面即不同，保持原样） |
| V3 | `detailList` 为空 → 阻止保存 | "请至少填写一个项目的工时或假期记录" |
| V4 | 假期条目 `leaveHours <= 0` 或未选类型 → 该条**静默不参与**保存（filter，与桌面一致，不报错） |

## 4. 状态与派生值

| 派生值 | 规则 |
|--------|------|
| `totalHours` | Σ 所有工时>0 的 work 行 + Σ 有效假期条目小时；≥8 绿色 / >0 且 <8 橙色 |
| `isEditable`（work 区） | `selectedDate ∈ [本周一, 本周日]`；移动端一期日期入口仅本周 7 天 chip，该式恒可满足，但只读态仍须实现（防 URL 直达携带越界日期） |
| 假期区可编辑性 | 桌面为恒可编辑（任意日期）；移动端一期随所选日期同 work 区（本周内），提前填未来假期不在一期范围（见 spec Out of Scope） |
| `isWhitelisted` | 来源 `checkSelfInWhitelist()`（进入页面时最先调用，接口异常时按 false 兜底）；true → 整页豁免提示，不渲染表单与保存栏，且不再请求 myProjects/my/{date} |
| 回显 | `GET /project/dailyReport/my/{date}` → `data.detailList` 按 `projectId + subProjectId + entryType` 分发回填至对应视图模型 |

## 5. 字段陷阱备忘

- `workCategory`：**接口存逗号字符串，视图层是数组**——加载时 `split(',')`，保存时 `join(',')`
- `subProjectId`：语义是 `pm_task.task_id`，字段名是历史兼容产物，勿改名
- 假期 detail 的 `workHours` 与 `leaveHours` **同值双写**（桌面如此，保持一致）
- 保存成功后须重新 `GET my/{date}` 刷新 `currentReportId`（后端可能新建了 report 主记录）
