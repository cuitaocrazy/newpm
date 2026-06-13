# Phase 0 Research: 非批次版本管理

源码已逐文件核实（storageManual/*）。关键技术决策：

## D1. 复用 pm_version_out + manual_input 区分
- **Decision**: 不建新表，复用 `pm_version_out`，`manual_input='1'` 标识非批次；查询全部硬编码该条件。
- **Rationale**: 旧系统就是共用 `T_B_VERSION_OUT`；批次查询已硬编码 `manual_input='0'`，对称即可。

## D2. 手填任务信息加 2 列（决策 A）
- **Decision**: `pm_version_out` 加 `manual_task_no varchar(64)` + `manual_task_name varchar(255)`，专供非批次手填。
- **Rationale**: 非批次单个手填任务，不进 `pm_version_out_task`（那是批次存 task_id 用）。主表自带直观。
- **TASK_NAME 一列两义陷阱**：旧系统该列批次=版本简介、非批次=任务名称。newpm 已拆：批次用 version_brief，非批次用 manual_task_name。数据迁移时按 manual_input 分别映射。

## D3. 版本号生成完全复用 VersionNumberGenerator
- **Decision**: 直接复用批次的 `VersionNumberGenerator`，逻辑逐字相同（6 类型、.9 进位、5→3/6→1 回退、编辑重算）。
- **Rationale**: 旧系统 storageManual 的 getOutLibVersion 与 storage 相同。输入来源（子系统/基准版本号从 pm_sys_name 带出）也相同。

## D4. 独立权限 + 独立 Controller
- **Decision**: 新建 `VersionOutManualController` → `/project/versionOutManual/**`，权限 `project:versionOutManual:*`。
- **Rationale**: 用户决策独立权限便于分别授权（旧系统实为共用 storageManagement）。Service 可复用 VersionOut 的，加 manualInput 参数或 manual 专用方法。

## D5. 字段裁剪（非批次无）
- 去掉：组包方式(package_mode)、版本状态(version_status)、版本简介(version_brief)、项目名/需求名、多任务行、任务下拉联动、任务审核校验。
- 保留并改手填：软件中心任务号、任务名称。
- 来源：旧 storageManual/create.html 已核实无上述被裁字段。

## D6. 联动接口复用批次
- batchByYear / sysNameByProduct / outVersionOptions / versionPDate / generateOutLibVersion 全部复用批次端点（或在 manual controller 暴露同样的）。
- **不需要** taskByProduct/taskInfo（非批次手填，无任务联动）。

## D7. 测试策略（skill 强制三件套）
- 单元：版本号生成器已有 10 测试（复用，无需重写）；新增 manual 的 service 测试（insert/update/查询带 manual_input、手填任务存取）。
- E2E：`tests/e2e-version-out-manual-crud.spec.js` 覆盖 manual controller 全端点。
- JaCoCo：已配 pom，自动出报告。
