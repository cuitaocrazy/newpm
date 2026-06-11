# Phase 0 Research: 批次版本管理

研究输入已在前期完成：老系统全量盘点（`docs/plans/2026-06-10-yadapm-migration-requirements.md`）+ 版本号算法源码提取（`reference-version-algorithm.md`）。本文件汇总落地 newpm 的关键技术决策。

---

## D1. 版本号生成算法的移植策略

- **Decision**: 把 Scala `getOutLibVersion` 端口为独立 Java 类 `VersionNumberGenerator`，输入版本类型/子系统/基准版本号/升级包初级版本号/编辑标志，依赖注入 `VersionOutMapper` 的 4 个查询方法（getMaxVersionCode、getMaxVersionCodeByYear、getCodeByOutVersion、getCodeByBaseVersion）。算法纯逻辑部分（拼接/补零/进位）与 DAO 查询分离。
- **Rationale**: 算法是最高风险点，分离后可用 JUnit + Mockito 对 6 种类型逐一写特征测试，无需 DB；与老系统行为逐条比对锁定。
- **Alternatives considered**: 写进 ServiceImpl（拒绝：难单测）；用数据库存储过程（拒绝：违背 newpm 全 Java 习惯、难版本控制）。
- **关键移植细节**（务必保留）：
  - 类型 1/2/3：`(maxCode+1)` 两位补零，前缀 SP/PTF/B。
  - 类型 4：`(maxCodeByYear+1)` 三位补零，格式 `T_{年}_{NNN}_{产品}`；编辑时仅当子产品或类型变更才重算。
  - 类型 5/6：调 `getCodeByOutVersion`，空则回退查基线类型（**5→3、6→1**）；`.` 切分，单段→`{major:%02d}.01`，双段→`{major}.{minor+1:%02d}`（数值进位解决 .9 问题）。
  - 首条记录 maxCode 为空/null → 视作 0。

## D2. 版本号唯一性与并发

- **Decision**: `pm_version_out.out_lib_version` 加普通索引（非全局唯一，因不同子系统可重名）；唯一性约束落在 `(sys_name, version_type, out_lib_version)` 组合唯一键。生成→插入若触发唯一冲突，捕获后重试（最多 N 次）重新取 maxCode+1。
- **Rationale**: 老系统"查最大+1"裸奔有竞态；组合唯一键 + 重试在不引入分布式锁的前提下保证正确性，符合宪法"业务完整性"。
- **Alternatives considered**: Redis 分布式锁（拒绝：过重，单实例够用且重试更简单）；数据库序列（拒绝：MySQL 无原生序列，且版本号非简单自增）。

## D3. 子系统（sys_name）建配置表而非字典

- **Decision**: 新建 `pm_sys_name` 表（子系统名称、基准版本号 base_version_code、一级产品 p_id、产品 product），提供下拉 + 联动查询。版本类型/组包方式/版本状态用标准字典。
- **Rationale**: 子系统含"基准版本号""一级产品""产品"等多个附加属性（对应老 `T_C_SYS_NAME`：id/sysName/baseVersioncode/pId/product），字典的 label/value 两列放不下；其余三者是纯枚举，适合字典（对应宪法 VI"字典展示用 dict 组件"）。
- **product 归属澄清**（修正 analyze I1）：`product`/`p_id` 是**子系统的属性**，不是主表独立维护的字段。级联 `sysNameByProduct?product=` 按 product 过滤子系统；主表 `pm_version_out.product` 选定子系统后冗余带出；类型4 版本号后缀 `T_{年}_{NNN}_{product}` 取自子系统的 product。主表另存 `sub_version_code`（子产品ID），用于类型4 的"子产品+年份"序号维度。
- **Alternatives considered**: 全部塞字典（拒绝：子系统属性多）；全部建表（拒绝：版本类型等纯枚举建表过度）。
- **新增字典**：
  - `sys_version_type`（1=SP升级包 2=PTF补丁包 3=B测试包 4=临时版本包 5=B包升级包 6=SP包升级包）
  - `sys_package_mode`（1=A1-本批次全量 2=A2-本批次增量 3=B1-单任务全量 4=B2-单任务增量 5=C1-多任务全量 6=C2-多任务增量）—— 已从老 `组包方式表.sql` insert 核准
  - `sys_version_status`（**修正 analyze A1**：老 `T_C_VERSION_STATUS` 仅有建表无 insert，枚举值是生产库运行时数据，源码无从提取）→ 建成**空字典，由管理员/迁移数据填充**；version_status 字段本轮**可选**，待生产值到位后灌入并可收紧为必填。
- **缓存**：插入字典后须刷 Redis `sys_dict:sys_version_type` 等，否则前端下拉用旧值。

## D4. 多任务关联：子表 vs 逗号分隔

- **Decision**: 建关联子表 `pm_version_out_task`（version_id、task_id、task_no、task_name、prj_name、demand_name），主从级联插入/删除，`@Transactional`。
- **Rationale**: 老系统用逗号分隔 `task_no` 难查询、难关联 `pm_task`；newpm 主从模式（`<collection>` resultMap）更规范，支持按任务号反查版本。
- **Alternatives considered**: 保留逗号分隔（拒绝：违背关系规范、无法 JOIN pm_task 做数据权限）。

## D5. 投产批次/年份/产品/任务的实体映射

- **Decision**: 复用 newpm 既有资产 —— 投产批次→`pm_production_batch`（批次号、投产日期来源）；投产年份→字典 `sys_ndgl`；产品→字典 `sys_product`；软件中心任务→`pm_task`（`software_demand_no`、`task_name`）。级联接口查这些既有表/字典。
- **Rationale**: 宪法 IV + 复用，避免重建任务/批次体系；版本投产日期从 `pm_production_batch` 带出（对应老 getVersionPDate）。
- **Alternatives considered**: 新建独立批次/年份表（拒绝：重复造轮子、数据割裂）。

## D6. 软删除策略

- **Decision**: `pm_version_out` 用 newpm 默认软删除 `del_flag='1'`（不进硬删除例外清单）。
- **Rationale**: 宪法 I"软删除是默认策略"，硬删除例外仅 pm_project/pm_task/pm_daily_report*，本表不在列。

## D7. 级联/联动接口设计

- **Decision**: 老系统的 7 个 ajax 端点端口为 REST GET，挂 `/project/versionOut/**`：
  - `GET /batchByYear?year=` 年份→批次
  - `GET /sysNameByProduct?product=` 产品→子系统
  - `GET /outVersionOptions?sysName=&versionType=` 子系统+类型→升级包初级版本号候选
  - `GET /generateOutLibVersion?...` 生成出入库版本号（核心，前端实时调用回填只读框）
  - `GET /versionPDate?batchId=` 批次→投产日期
  - `GET /taskInfo?taskNo=` 任务号→任务名/项目/需求回显
- **Rationale**: Thymeleaf POST ajax → SPA 下统一 REST GET，前端 Vue 调用；权限并入模块权限点。

## D8. 测试策略

- **Decision**: ① `VersionNumberGeneratorTest` 对 6 类型 + 边界（空 maxCode、.9 进位、编辑重算）写特征测试；② `VersionOutServiceImplTest` 覆盖 CRUD + 多任务级联（Mockito）；③ E2E `e2e-version-out-*.spec.js` 覆盖新增→生成版本号→查询→编辑→删除→导出主流程（跑前临时关验证码）。
- **Rationale**: 与 newpm 现有特征测试约定一致（service 层 JUnit5+Mockito，无需 MySQL/Redis）。
