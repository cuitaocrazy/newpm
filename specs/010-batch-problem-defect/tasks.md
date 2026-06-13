---
description: "Task list for 批次任务问题单及缺陷"
---
# Tasks: 批次任务问题单及缺陷

最复杂迁移功能(含附件)。分5段实现，每段验证。

## Phase 1: Setup（数据库）
- [ ] T001 `00_tables_ddl.sql` 建 `pm_prolist_defect` 主表（字段见 plan 映射表 + del_flag 软删 + 审计）；唯一键 problem_no
- [ ] T002 `01_tables_data.sql` 加3字典：`sys_problem_state`(已定位/待验证/未受理/已受理/问题已解决/问题再现)、`sys_problem_level`(高/中/低优先级:N天内解决)、`sys_prolist_file_type`(相关材料)
- [ ] T003 `02_menu_data.sql` 一级菜单「项目质量管理」+ 二级「批次任务问题单及缺陷」+ 4权限(list/edit/remove/file) + query
- [ ] T004 `migration_010_batch_problem_defect.sql` 可移植脚本(建表+字典+菜单，幂等)；应用本地+刷字典缓存

## Phase 2: 后端 Foundational + US1（MVP 新增）
- [ ] T005 domain/ProlistDefect.java（主表字段+关联展示字段+多值查询字段）
- [ ] T006 mapper/ProlistDefectMapper.java + xml：insert/selectById/checkProblemNo + 联动查询(taskOptions按部门ancestors / taskInfo / batchByYear / tcDate)
- [ ] T007 service/IProlistDefectService + impl：insertProlistDefect(算 solutionTimeOverOneDay) + 查重 + 联动转发
- [ ] T008 controller/ProlistDefectController：POST(add) + 6联动端点(batchByYear/tcDate/deptOptions/taskOptions/taskInfo/checkProblemNo)，权限 edit/list
- [ ] T009 段验证：重建jar重启+关验证码+API冒烟(联动+查重+保存+算法)

## Phase 3: 后端 US2（查询列表）
- [ ] T010 mapper selectProlistDefectList：多维查询(年份/批次/产品一二级/任务/5布尔/状态/部门/编号/创建人/日期范围) + JOIN(pm_task/sys_dept/sys_user 加COLLATE) 取展示字段
- [ ] T011 controller GET /list(startPage)；段验证

## Phase 4: 后端 US3（改/删/导出）+ 附件
- [ ] T012 update(查重排除自己+重算派生) / 软删除deleteByIds / export(enrichForExport)
- [ ] T013 AttachmentController @PreAuthorize 白名单加 project:prolistDefect:query/file/edit；段验证

## Phase 5: 前端 US1-US4
- [ ] T014 api/project/prolistDefect.ts（CRUD+导出+6联动）
- [ ] T015 add.vue/edit.vue（年份→批次→部门→任务号联动+回显只读+问题单编号查重+5布尔下拉+字典；add≡edit）
- [ ] T016 index.vue（多维查询+约30列+布尔颜色高亮+操作列；搜索缓存）
- [ ] T017 detail.vue（全字段+审计 + 附件区复用 FileUpload/附件列表/日志）
- [ ] T018 菜单路由

## Phase 6: 测试三件套 + 收尾
- [ ] T019 单元测试 ProlistDefectServiceImplTest：solutionTimeOverOneDay 各分支 + 查重(新增/编辑排除自己) + CRUD + 联动转发，逻辑覆盖≥90%
- [ ] T020 E2E e2e-prolist-defect-crud.spec.js：全端点(CRUD+6联动+导出) 真跑通
- [ ] T021 浏览器UI验证：联动链/查重/保存/列表高亮/详情/附件，截图
- [ ] T022 Code Review + 全量回归 + 前端build + 回写需求文档 + 恢复验证码 + 提交

## Notes
- 任务审核状态**不校验**(老系统核实)。
- 问题单编号查重编辑排除自己(修老bug)。
- 部门过滤走 ancestors。
- 附件复用 pm_attachment(business_type=prolist)，前端文档类型用 sys_prolist_file_type 字典。
