---
description: "Task list for 非批次任务问题单及缺陷"
---
# Tasks: 非批次任务问题单及缺陷

镜像 ④ 批次问题单(spec 010)，砍任务联动、任务字段实存手填。

## Phase 1: Setup
- [ ] T001 00_tables_ddl 建 pm_nobatch_prolist_defect(④字段+冗余手填任务字段;problem_no varchar(160)唯一;软删)
- [ ] T002 02_menu_data 项目质量管理下二级"非批次任务问题单及缺陷"+4权限(list/query/edit/remove/file)。复用已有3字典(不新增)
- [ ] T003 migration_011 可移植脚本(幂等); 应用本地

## Phase 2: 后端
- [ ] T004 domain/NobatchProlistDefect(任务字段实存; batch/dept/审计名 JOIN展示字段; 多值查询字段)
- [ ] T005 mapper(+xml): selectList(多维+JOIN批次/部门/用户加COLLATE,任务字段读本表,部门走ancestors) + CRUD + checkProblemNoUnique + batchByYear + selectPlanProductionDate
- [ ] T006 service(+impl): 派生算法 + 查重(编辑排除自己) + 联动转发。**无 syncDeptIdFromTask**
- [ ] T007 controller NobatchProlistDefectController(/project/nobatchProlist): CRUD+导出+batchByYear/tcDate/checkProblemNo
- [ ] T008 附件: AttachmentController 白名单加 nobatchProlist; AttachmentServiceImpl.getBusinessFolder 加 nobatch_prolist 分支
- [ ] T009 段验证: 重建jar+API冒烟(查重/派生/CRUD/软删坑13)

## Phase 3: 前端
- [ ] T010 api/project/nobatchProlist.ts
- [ ] T011 add.vue/edit.vue(任务号/任务名 input手填; 二级产品dict-select sys_product; 排期状态dict-select sys_pqzt; 三测试日期手填; 年份→批次→计划投产日期联动不联动任务; 查重失焦)
- [ ] T012 index.vue(多维查询+列表布尔颜色高亮+操作列)
- [ ] T013 detail.vue(全字段+附件区复用)
- [ ] T014 router 隐藏路由(add/edit/detail)

## Phase 4: 测试 + 收尾
- [ ] T015 单测 NobatchProlistDefectServiceImplTest(派生各分支+查重+CRUD+转发,覆盖≥90%)
- [ ] T016 E2E e2e-nobatch-prolist-defect-crud.spec.js(全端点+查重+派生+软删坑13)
- [ ] T017 浏览器UI验证(手填字段/批次联动计划投产日期/列表/详情/附件)
- [ ] T018 Code Review + 全量回归 + 前端build + 回写需求文档 + 恢复验证码 + 提交

## Notes
- dept_id 直接存手选(非批次无真实任务,不派生)。
- 附件 business_type=nobatch_prolist。
- 二级产品 sys_product、排期状态 sys_pqzt 字典。
