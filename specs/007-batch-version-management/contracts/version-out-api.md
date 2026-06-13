# API 契约：批次版本管理 `/project/versionOut/**`

Controller `VersionOutController` extends `BaseController`。所有列表接口首行 `startPage()`。权限点 `project:versionOut:{action}`。本 spec 仅处理批次（`manualInput=0`，查询自动加该条件）。

返回约定：列表 `TableDataInfo{code,msg,total,rows}`，单对象/操作 `AjaxResult{code,msg,data}`。

## CRUD 端点

| 方法 | 路径 | 权限 | 入参 | 出参 | 说明 |
|---|---|---|---|---|---|
| GET | `/project/versionOut/list` | `project:versionOut:list` | VersionOut（query：production_year/batch_id/sub_version_code/task_no/version_type/comm_name/base_version_code/package_mode/version_status/version_p_date）+ 分页/排序 | TableDataInfo | 分页查询，自动加 manual_input=0 |
| GET | `/project/versionOut/{id}` | `project:versionOut:query` | path id | AjaxResult\<VersionOut\>（含 taskList） | 详情 |
| POST | `/project/versionOut` | `project:versionOut:add` | VersionOut（含 taskList，**不含 out_lib_version**） | AjaxResult | 新增；服务端生成版本号 + 级联存任务。`@Log(新增)` |
| PUT | `/project/versionOut` | `project:versionOut:edit` | VersionOut（含 id、taskList） | AjaxResult | 编辑；关键字段变更则重算版本号。`@Log(修改)` |
| DELETE | `/project/versionOut/{ids}` | `project:versionOut:remove` | path ids（逗号分隔） | AjaxResult | 批量软删除。`@Log(删除)` |
| POST | `/project/versionOut/export` | `project:versionOut:export` | VersionOut（query） | Excel 流 | 导出。`@Log(导出)` |

## 级联/联动端点（GET，归 `project:versionOut:list` 权限）

| 路径 | 入参 | 出参 | 说明 |
|---|---|---|---|
| `/project/versionOut/batchByYear` | year | AjaxResult\<List\<批次\>\> | 年份→批次列表（查 pm_production_batch） |
| `/project/versionOut/sysNameByProduct` | product | AjaxResult\<List\<SysName\>\> | 产品→子系统列表 |
| `/project/versionOut/outVersionOptions` | sysName, versionType | AjaxResult\<List\<String\>\> | 子系统+类型→升级包初级版本号候选（类型 5/6） |
| `/project/versionOut/generateOutLibVersion` | sysName, subVersionCode, versionType, outVersion, addFlag, id, oldSubVersionCode, oldVersionType | AjaxResult\<{outLibVersion, versionCode}\> | **核心**：实时生成版本号，前端回填只读框 |
| `/project/versionOut/versionPDate` | batchId | AjaxResult\<String\> | 批次→投产日期 |
| `/project/versionOut/taskInfo` | taskNo | AjaxResult\<{taskName,prjName,demandName}\> | 任务号→回显信息（查 pm_task，保留备用） |
| `/project/versionOut/taskOptions` | productionYear, batchId, product | AjaxResult\<List\<{taskId,taskNo,taskName,prjName,demandName}\>\> | **软件中心任务号下拉数据源**：按 年份+批次+产品 查 pm_task（JOIN pm_project），一次带回回显。对应老系统 ajax_getCenterTaskNo |

## 关键契约约束

1. **out_lib_version 服务端生成**：POST/PUT 即便前端传了该字段也忽略，由 `VersionNumberGenerator` 重新生成；PUT 时若 sys_name/version_type/sub_version_code 未变则沿用原值。
2. **唯一性**：新增/编辑落库命中 `uk_sys_type_outlib` 冲突时，服务端重试重新生成（最多 3 次），仍失败返回 `AjaxResult.error("版本号生成冲突，请重试")`。
3. **必填校验**：`@Validated` + 实体 `@NotBlank/@NotNull`，违例由 GlobalExceptionHandler 统一返回。
4. **数据权限**：列表查询如涉及部门隔离，Mapper 注入 `${params.dataScope}`，JOIN sys_user 加 COLLATE。
5. **manual_input 隔离**：所有查询硬编码 `manual_input='0'`，确保不串到非批次数据（未来非批次 spec 用 '1'）。

## 前端 API（`ruoyi-ui/src/api/project/versionOut.ts`）

```typescript
import request from '@/utils/request'
export function listVersionOut(query) { return request({ url: '/project/versionOut/list', method: 'get', params: query }) }
export function getVersionOut(id)      { return request({ url: `/project/versionOut/${id}`, method: 'get' }) }
export function addVersionOut(data)    { return request({ url: '/project/versionOut', method: 'post', data }) }
export function updateVersionOut(data) { return request({ url: '/project/versionOut', method: 'put', data }) }
export function delVersionOut(ids)     { return request({ url: `/project/versionOut/${ids}`, method: 'delete' }) }
export function generateOutLibVersion(params) { return request({ url: '/project/versionOut/generateOutLibVersion', method: 'get', params }) }
// + batchByYear / sysNameByProduct / outVersionOptions / versionPDate / taskInfo
```
