# Quickstart: 批次版本管理

面向实现者的最短上手 + 验证路径。完整设计见 plan.md / data-model.md / contracts/。

## 实现顺序（建议）

1. **DDL**：在 `pm-sql/init/00_tables_ddl.sql` 追加 `pm_version_out`、`pm_version_out_task`、`pm_sys_name`；本地建表。
2. **字典 + 菜单**：`01_tables_data.sql` 加 3 个字典；`02_menu_data.sql` 加一级/二级菜单 + 6 个权限点（list/query/add/edit/remove/export）。导入后刷 Redis 字典缓存。
3. **后端**：domain → mapper(+xml) → service（先 `VersionNumberGenerator`，配特征测试）→ serviceImpl → controller。
4. **后端自测**：`mvn test -pl ruoyi-project -am -Dtest=VersionNumberGeneratorTest`。
5. **前端**：`api/project/versionOut.ts` → `views/project/versionOut/{index,add,edit,detail}.vue`。
6. **E2E**：`e2e-version-out-crud.spec.js` 跑通主流程（先临时关验证码）。

## 本地环境（来自项目记忆）

- 后端端口 **8085**；前端 vite 80；登录 `admin` / `123456789`。
- 本地 MySQL 容器 `newpm-mysql-1`，Redis 容器 `newpm-redis-1`，密码 `password`，库 `ry-vue`。
- 建表 SQL：`cat xxx.sql | docker exec -i newpm-mysql-1 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue`
- 刷字典缓存：`docker exec -i newpm-redis-1 redis-cli DEL sys_dict:sys_version_type`（三个字典各一次）

## 版本号生成验证用例（特征测试必覆盖）

| 场景 | 输入 | 期望版本号 |
|---|---|---|
| 类型1 首条 | base=ABC, maxCode 空 | `ABC_SP01` |
| 类型1 续号 | base=ABC, maxCode=5 | `ABC_SP06` |
| 类型3 续号 | base=ABC, maxCode=9 | `ABC_B10` |
| 类型4 新增 | year=2026, product=P1, maxByYear=2 | `T_2026_003_P1` |
| 类型4 编辑(关键字段未变) | 原号 005 | `T_2026_005_P1`（沿用） |
| 类型5 首个升级包 | 回退查B测试包 base 段=02 | `ABC_B02.01` |
| 类型5 已有升级包 | 现有 02.09 | `ABC_B02.10`（数值进位，非字符串） |
| 类型6 首个 | 回退查SP升级包 | `ABC_SP{major}.01` |

## 验收对照（spec → 验证动作）

- US1：新增一条批次版本，确认 6 类型版本号正确、投产日期自动带出、多任务可增删 → SC-001/002/003。
- US2：按各条件查询、进详情、返回保留筛选 → SC-004。
- US3：编辑（改备注号不变 / 改子系统号重算）、删除留审计 → SC-006。
- US4：导出 Excel 含全部列。
- US5（延后）：数据迁移待旧库数据到位，本轮不验证。

## 注意事项（踩坑预警）

- 新表与 sys_user/sys_dict_data JOIN **必加** `COLLATE utf8mb4_unicode_ci`。
- 版本号生成的"取最大+1"在 mapper 中对空值/非数字防御（COALESCE / 视作 0）。
- 类型 5→3、6→1 的回退基线类型映射别漏。
- `manual_input='0'` 条件所有查询都要带，避免串非批次数据。
- 提交人员/部门下拉用 `/project/project/users`、`deptTree` 代理 API，勿直连 system/。
