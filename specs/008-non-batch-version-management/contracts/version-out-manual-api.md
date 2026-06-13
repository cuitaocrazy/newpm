# API 契约：非批次版本管理 `/project/versionOutManual/**`

`VersionOutManualController` extends `BaseController`。权限 `project:versionOutManual:{action}`。
所有查询硬编码 `manual_input='1'`。

## CRUD 端点

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/list` | list | 分页查询（manual_input='1'）。条件：production_year/pro_batch_no/manual_task_no/version_type/comm_name/base_version_code/version_p_date/product/out_lib_version |
| GET | `/{id}` | query | 详情（全字段+审计） |
| POST | `/` | add | 新增（生成版本号+存手填任务+manual_input='1'）。`@Log(新增)` |
| PUT | `/` | edit | 编辑（关键字段变更重算版本号）。`@Log(修改)` |
| DELETE | `/{ids}` | remove | 软删除。`@Log(删除)` |
| POST | `/export` | export | 导出 Excel。`@Log(导出)` |

## 级联端点（复用批次逻辑，归 list 权限）

| 路径 | 入参 | 说明 |
|---|---|---|
| `/batchByYear` | year | 年份→批次（同批次） |
| `/sysNameByProduct` | product | 产品→子系统（同批次） |
| `/outVersionOptions` | sysName, versionType | 子系统+类型→初级版本号（类型5/6） |
| `/versionPDate` | batchId | 批次→投产日期 |
| `/generateOutLibVersion` | sysName,subVersionCode,versionType,outVersion,addFlag,id,oldSub,oldType | 实时生成版本号 |

**不提供**（非批次手填，无任务联动）：taskOptions、taskInfo。

## 关键约束

1. **manual_input 隔离**：所有查询/新增硬编码 `manual_input='1'`，与批次零串扰。
2. **手填任务**：新增/编辑把 manualTaskNo/manualTaskName 存主表 2 列，不进 pm_version_out_task。
3. **版本号服务端生成**：复用 VersionNumberGenerator，前端传值忽略，编辑关键字段变更才重算。
4. **唯一性**：复用 uk_sys_type_outlib + 重试。

## 前端 API（`ruoyi-ui/src/api/project/versionOutManual.ts`）

```typescript
listVersionOutManual / getVersionOutManual / addVersionOutManual /
updateVersionOutManual / delVersionOutManual / exportVersionOutManual /
generateOutLibVersion / getBatchByYear / getSysNameByProduct /
getOutVersionOptions / getVersionPDate
```
