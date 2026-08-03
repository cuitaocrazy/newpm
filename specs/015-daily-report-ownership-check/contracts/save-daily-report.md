# 行为契约：保存日报

**Feature**: 015-daily-report-ownership-check
**接口**: `POST /project/dailyReport`
**权限**: `@ss.hasAnyPermi('project:dailyReport:add,project:dailyReport:edit')`（既有，不变）
**审计**: `@Log(title="日报管理", businessType=INSERT)`（既有，不变）

> 本契约**不改变**请求／响应的数据结构，只改变**行为语义**。前端零改动。

## 请求（结构不变）

```json
{
  "reportDate": "2026-07-21",
  "detailList": [
    { "projectId": 19, "subProjectId": null, "entryType": "work",
      "workHours": 6, "workContent": "...", "workCategory": "1" },
    { "projectId": null, "entryType": "annual", "leaveHours": 8 }
  ]
}
```

## 核心语义变更

### 变更前

> 「这一天的工时**就是** `detailList`」——服务端删除该日全部既有明细，再按 `detailList` 重建。

后果：`detailList` 未涵盖的既有明细一律被删除，包括填报人**在界面上看不到、因而无从提交**的明细。

### 变更后

> 「在**作用范围**内，这一天的工时是 `detailList`；范围外的既有明细不受本次请求影响。」

**作用范围** = 填报人当前可填项目的工时 ∪ 全部非项目工时（`projectId` 为 null 的记录）

## 行为矩阵

| 既有明细的状态 | 是否出现在 `detailList` | 服务端行为 |
|---|---|---|
| 所属项目在填报人可填列表中 | 出现 | 按提交值更新 |
| 所属项目在填报人可填列表中 | 未出现 | **删除**（视为填报人主动清零） |
| 所属项目**不在**可填列表中（如已结项） | 未出现 | **原样保留**（工时数、工作内容、所属任务、工作类别均不变） |
| 非项目工时（`projectId` 为 null） | 未出现 | 删除（这类记录在界面上始终可见） |

## 拒绝规则

以下任一条件命中，**整个请求被拒绝**，不做任何写入（既有明细不变、任何项目的实际人天不变）：

| 条件 | HTTP / 响应 | 提示内容 |
|---|---|---|
| `detailList` 中某条 `entryType='work'` 的 `projectId`，填报人从未以任何身份参与过该项目 | `200` + `{code:500, msg}` | `项目《XXX》不在您参与的项目范围内` |
| 同上，且该项目已结项（`project_stage='11'`） | `200` + `{code:500, msg}` | `项目《XXX》已结项，不能新增或修改其工时` |
| 某条明细的 `subProjectId` 所属任务不隶属于该条明细声明的 `projectId` | `200` + `{code:500, msg}` | `任务与所选项目不匹配` |
| 填报人在「无需填写日报」白名单中 | `200` + `{code:500, msg}` | `您已被设置为无需填写日报…`（既有行为，不变） |

响应沿用框架统一格式（`ServiceException` → `GlobalExceptionHandler` → `AjaxResult.error`），
HTTP 状态码保持 200，业务错误由 `code=500` 表达——与项目既有约定一致（宪法 III）。

**「曾参与」的判据**：`pm_project_member` 中存在该 `(project_id, user_id)` 行，**不限** `is_active`
与 `del_flag`。即离场／被移出的成员仍可维护自己在该项目上的历史工时；而从未参与者一律拒绝。
成员行只能由持项目编辑权限者写入，填报人无法自助获得，故该门槛不可伪造。

## 不变式（实现必须保证）

- **INV-1**：请求被拒绝时，数据库状态与请求前完全一致（明细、日报主记录、所有项目的 `actual_workload`）
- **INV-2**：保存成功后，涉及项目的 `actual_workload` 严格等于其全部日报明细的 `work_hours` 汇总
  （口径见 Issue #5：`SUM WHERE project_id = 本项目 AND entry_type='work' AND del_flag='0'`）
- **INV-3**：作用范围外的既有明细，其 `work_hours` / `work_content` / `sub_project_id` /
  `work_category` 在保存前后逐字节相同
- **INV-4**：填报人把可见项目的工时清零后保存，该明细被删除（不得因防丢失逻辑而变得无法删除）

## 兼容性

| 调用方 | 影响 |
|---|---|
| 桌面填写页 `write.vue` | 无需改动。正常填报路径行为不变；此前会静默丢失的场景现在自动保护 |
| 移动端 H5 `views/m/dailyReport/write` | 无需改动，同上 |
| 直接调用 API 的脚本／工具 | 若曾依赖「提交空 `detailList` 清空整天工时」的行为，现在只会清空作用范围内的部分。经核查无此类调用方 |

## 相关的非契约变更

工时重算（`actual_workload`）口径不变，仍为 Issue #5 确立的「按明细全量汇总」。被保留的明细
仍在库中，因此会被自动计入其项目的实际人天——这是保留语义与 INV-2 能同时成立的原因。
