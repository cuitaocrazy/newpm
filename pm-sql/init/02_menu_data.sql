-- ========================================
-- PM 模块菜单数据（完整版）
-- 更新时间: 2026-03-04
-- ========================================

-- ========================================
-- 第一步：清理旧菜单数据（从叶到根）
-- ========================================

-- 清理所有 PM 权限按钮（type=F）
DELETE FROM sys_menu WHERE menu_type = 'F' AND (perms LIKE 'project:%' OR perms LIKE 'revenue:%');

-- 清理所有 PM 业务菜单（type=C），含隐藏路由
DELETE FROM sys_menu WHERE menu_type = 'C' AND (perms LIKE 'project:%' OR perms LIKE 'revenue:%');

-- 清理 PM 顶层目录（type=M）
DELETE FROM sys_menu WHERE menu_type = 'M' AND parent_id = 0 AND path IN ('project', 'htkx', 'revenue', 'market', 'dailyReport');

-- ========================================
-- 项目管理目录
-- ========================================

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('项目管理', 0, 3, 'project', NULL, 1, 0, 'M', '0', '0', '', 'guide', 'admin', sysdate(), '', NULL, '项目管理目录', 'ProjectManage');
SELECT @projectRootMenuId := LAST_INSERT_ID();

-- ---- 立项申请 ----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('立项申请', @projectRootMenuId, 1, 'apply', 'project/project/apply', 1, 0, 'C', '0', '0', 'project:project:add', 'edit', 'admin', sysdate(), '', NULL, '立项申请菜单', 'ProjectApply');

-- ---- 项目管理 ----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('项目管理', @projectRootMenuId, 2, 'list', 'project/project/index', 1, 0, 'C', '0', '0', 'project:project:list', 'list', 'admin', sysdate(), '', NULL, '项目管理菜单', 'ProjectList');
SELECT @projectMenuId := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查询', @projectMenuId, 0, '#', '', 1, 0, 'F', '0', '0', 'project:project:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('导出', @projectMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:project:export', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('详情', @projectMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:project:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('编辑', @projectMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'project:project:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查看合同', @projectMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'project:contract:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('添加合同', @projectMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'project:contract:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('收入确认', @projectMenuId, 6, '#', '', 1, 0, 'F', '0', '0', 'revenue:company:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('附件管理', @projectMenuId, 7, '#', '', 1, 0, 'F', '0', '0', 'project:project:attachment', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('删除', @projectMenuId, 8, '#', '', 1, 0, 'F', '0', '0', 'project:project:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 项目管理 - 隐藏路由
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name, active_menu)
VALUES ('编辑项目', @projectRootMenuId, 10, 'list/edit/:projectId(\\d+)', 'project/project/edit', 1, 0, 'C', '1', '0', 'project:project:edit', '#', 'admin', sysdate(), '', NULL, '编辑项目页面', 'ProjectEdit', '/project/list');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name, active_menu)
VALUES ('项目详情', @projectRootMenuId, 11, 'list/detail/:projectId(\\d+)', 'project/project/detail', 1, 0, 'C', '1', '0', 'project:project:query', '#', 'admin', sysdate(), '', NULL, '项目详情页面', 'ProjectDetail', '/project/list');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name, active_menu)
VALUES ('项目附件管理', @projectRootMenuId, 12, 'list/attachment/:projectId(\\d+)', 'project/project/attachment', 1, 0, 'C', '1', '0', 'project:project:attachment', '#', 'admin', sysdate(), '', NULL, '项目附件管理页面', 'ProjectAttachment', '/project/list');

-- ---- 立项审核 ----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('立项审核', @projectRootMenuId, 3, 'review', 'project/review/index', 1, 0, 'C', '0', '0', 'project:review:list', 'edit', 'admin', sysdate(), '', NULL, '立项审核菜单', 'ProjectReview');
SELECT @reviewMenuId := LAST_INSERT_ID();

-- 退回使用 project:review:reject，审核使用 project:review:approve；后端接口同时接受两种权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查询', @reviewMenuId, 0, '#', '', 1, 0, 'F', '0', '0', 'project:review:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('退回', @reviewMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:review:reject', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('审核', @reviewMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:review:approve', '#', 'admin', sysdate(), '', NULL, '');

-- ---- 项目经理变更 ----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目经理变更', @projectRootMenuId, 4, 'managerChange', 'project/managerChange/index', 1, 0, 'C', '0', '0', 'project:managerChange:list', 'user', 'admin', sysdate(), '', NULL, '项目经理变更菜单');
SELECT @managerChangeMenuId := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查询', @managerChangeMenuId, 0, '#', '', 1, 0, 'F', '0', '0', 'project:managerChange:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('修改', @managerChangeMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:managerChange:change', '#', 'admin', sysdate(), '', NULL, '');

-- ---- 项目人员管理 ----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目人员管理', @projectRootMenuId, 5, 'projectMember', 'project/projectMember/index', 1, 0, 'C', '0', '0', 'project:member:list', 'peoples', 'admin', sysdate(), '', NULL, '项目人员管理菜单');
SELECT @memberMenuId := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('详情', @memberMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:member:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('编辑', @memberMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:member:edit', '#', 'admin', sysdate(), '', NULL, '');

-- ---- 项目阶段变更 ----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('项目阶段变更', @projectRootMenuId, 6, 'projectStageChange', 'project/projectStageChange/index', 1, 0, 'C', '0', '0', 'project:projectStageChange:list', 'log', 'admin', sysdate(), '', NULL, '项目阶段变更菜单');
SELECT @stageChangeMenuId := LAST_INSERT_ID();

-- 变更使用 project:projectStageChange:add，批量变更使用 project:projectStageChange:batchChange
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查询', @stageChangeMenuId, 0, '#', '', 1, 0, 'F', '0', '0', 'project:projectStageChange:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('变更', @stageChangeMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:projectStageChange:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('批量变更', @stageChangeMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:projectStageChange:batchChange', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('变更记录', @stageChangeMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'project:projectStageChange:query', '#', 'admin', sysdate(), '', NULL, '');

-- ========================================
-- 收入确认目录
-- ========================================

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('收入确认管理', 0, 6, 'revenue', NULL, 1, 0, 'M', '0', '0', '', 'money', 'admin', sysdate(), '', NULL, '收入确认管理目录');
SELECT @revenueMenuId := LAST_INSERT_ID();

-- ---- 公司收入确认 ----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('公司收入确认', @revenueMenuId, 1, 'company', 'revenue/company/index', 1, 0, 'C', '0', '0', 'revenue:company:list', 'money', 'admin', sysdate(), '', NULL, '公司收入确认菜单');
SELECT @companyRevenueMenuId := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查询', @companyRevenueMenuId, 0, '#', '', 1, 0, 'F', '0', '0', 'revenue:company:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('导出', @companyRevenueMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'revenue:company:export', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('收入查看', @companyRevenueMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'revenue:company:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('收入确认', @companyRevenueMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'revenue:company:edit', '#', 'admin', sysdate(), '', NULL, '');

-- 公司收入确认 - 隐藏路由
-- 注意：该路由供 revenue:company:query（查看）和 revenue:company:edit（编辑）两类用户使用
-- 为角色分配 revenue:company:edit 按钮时，必须同时分配此隐藏路由（perms=revenue:company:query）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name, active_menu)
VALUES ('收入确认详情', @revenueMenuId, 10, 'company/detail/:projectId(\\d+)', 'revenue/company/detail', 1, 0, 'C', '1', '0', 'revenue:company:query', '#', 'admin', sysdate(), '', NULL, '收入确认详情页面', 'RevenueCompanyDetail', '/revenue/company');

-- ---- 团队收入确认 ----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('团队收入确认', @revenueMenuId, 2, 'team', 'revenue/team/index', 1, 0, 'C', '0', '0', 'revenue:team:list', 'peoples', 'admin', sysdate(), '', NULL, '团队收入确认菜单');
SELECT @teamRevenueMenuId := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查询', @teamRevenueMenuId, 0, '#', '', 1, 0, 'F', '0', '0', 'revenue:team:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('新增', @teamRevenueMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'revenue:team:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('导出', @teamRevenueMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'revenue:team:export', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查看', @teamRevenueMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'revenue:team:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('编辑', @teamRevenueMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'revenue:team:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('删除', @teamRevenueMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'revenue:team:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 团队收入确认 - 隐藏路由
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('团队收入确认详情', @revenueMenuId, 11, 'team/detail/:projectId(\\d+)', 'revenue/team/detail', 1, 0, 'C', '1', '0', 'revenue:team:query', '#', 'admin', sysdate(), '', NULL, '团队收入确认详情页面', 'TeamRevenueDetail');

-- ========================================
-- 合同款项目录
-- ========================================

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('合同款项', 0, 5, 'htkx', NULL, 1, 0, 'M', '0', '0', '', 'form', 'admin', sysdate(), '', NULL, '合同款项目录');
SELECT @htkxMenuId := LAST_INSERT_ID();

-- ---- 合同管理 ----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('合同管理', @htkxMenuId, 1, 'contract', 'project/contract/index', 1, 0, 'C', '0', '0', 'project:contract:list', 'edit', 'admin', sysdate(), '', NULL, '合同管理菜单');
SELECT @contractMenuId := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查询', @contractMenuId, 0, '#', '', 1, 0, 'F', '0', '0', 'project:contract:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('新增合同', @contractMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:contract:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('导出', @contractMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:contract:export', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查看详情', @contractMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'project:contract:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('附件', @contractMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'project:contract:attachment', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('删除', @contractMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'project:contract:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 合同管理 - 隐藏路由
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name, active_menu)
VALUES ('新增合同', @htkxMenuId, 10, 'contract/add', 'project/contract/add', 1, 0, 'C', '1', '0', 'project:contract:add', '#', 'admin', sysdate(), '', NULL, '新增合同页面', 'ContractAdd', '/htkx/contract');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name, active_menu)
VALUES ('编辑合同', @htkxMenuId, 11, 'contract/edit/:contractId(\\d+)', 'project/contract/edit', 1, 0, 'C', '1', '0', 'project:contract:edit', '#', 'admin', sysdate(), '', NULL, '编辑合同页面', 'ContractEdit', '/htkx/contract');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name, active_menu)
VALUES ('合同详情', @htkxMenuId, 12, 'contract/detail/:contractId(\\d+)', 'project/contract/detail', 1, 0, 'C', '1', '0', 'project:contract:query', '#', 'admin', sysdate(), '', NULL, '合同详情页面', 'ContractDetail', '/htkx/contract');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name, active_menu)
VALUES ('合同附件', @htkxMenuId, 13, 'contract/attachment/:contractId(\\d+)', 'project/contract/attachment', 1, 0, 'C', '1', '0', 'project:contract:attachment', '#', 'admin', sysdate(), '', NULL, '合同附件页面', 'ContractAttachment', '/htkx/contract');

-- ---- 付款里程碑 ----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('付款里程碑', @htkxMenuId, 2, 'payment', 'project/payment/index', 1, 0, 'C', '0', '0', 'project:payment:list', 'money', 'admin', sysdate(), '', NULL, '付款里程碑菜单');
SELECT @paymentMenuId := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查询', @paymentMenuId, 0, '#', '', 1, 0, 'F', '0', '0', 'project:payment:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('新增付款里程碑', @paymentMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:payment:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('导出', @paymentMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:payment:export', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查看', @paymentMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'project:payment:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('编辑', @paymentMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'project:payment:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('删除', @paymentMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'project:payment:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('附件', @paymentMenuId, 6, '#', '', 1, 0, 'F', '0', '0', 'project:payment:attachment', '#', 'admin', sysdate(), '', NULL, '');

-- 付款里程碑 - 隐藏路由
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name, active_menu)
VALUES ('新增款项', @htkxMenuId, 20, 'payment/add', 'project/payment/form', 1, 0, 'C', '1', '0', 'project:payment:add', '#', 'admin', sysdate(), '', NULL, '新增款项页面', 'PaymentAdd', '/htkx/payment');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name, active_menu)
VALUES ('编辑款项', @htkxMenuId, 21, 'payment/edit/:paymentId(\\d+)', 'project/payment/form', 1, 0, 'C', '1', '0', 'project:payment:edit', '#', 'admin', sysdate(), '', NULL, '编辑款项页面', 'PaymentEdit', '/htkx/payment');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name, active_menu)
VALUES ('款项详情', @htkxMenuId, 22, 'payment/detail/:paymentId(\\d+)', 'project/payment/detail', 1, 0, 'C', '1', '0', 'project:payment:query', '#', 'admin', sysdate(), '', NULL, '款项详情页面', 'PaymentDetail', '/htkx/payment');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name, active_menu)
VALUES ('款项附件', @htkxMenuId, 23, 'payment/attachment/:paymentId(\\d+)', 'project/payment/attachment', 1, 0, 'C', '1', '0', 'project:payment:attachment', '#', 'admin', sysdate(), '', NULL, '款项附件页面', 'PaymentAttachment', '/htkx/payment');

-- ========================================
-- 市场管理目录
-- ========================================

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('市场管理', 0, 4, 'market', NULL, 1, 0, 'M', '0', '0', '', 'chart', 'admin', sysdate(), '', NULL, '市场管理目录');
SELECT @marketMenuId := LAST_INSERT_ID();

-- ---- 客户信息 ----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('客户信息', @marketMenuId, 1, 'customer', 'project/customer/index', 1, 0, 'C', '0', '0', 'project:customer:list', 'peoples', 'admin', sysdate(), '', NULL, '客户信息菜单');
SELECT @customerMenuId := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查询', @customerMenuId, 0, '#', '', 1, 0, 'F', '0', '0', 'project:customer:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('新增', @customerMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:customer:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('导出', @customerMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:customer:export', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查看', @customerMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'project:customer:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('编辑', @customerMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'project:customer:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('删除', @customerMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'project:customer:remove', '#', 'admin', sysdate(), '', NULL, '');

-- ========================================
-- 日报管理目录
-- ========================================

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('日报管理', 0, 7, 'dailyReport', NULL, 1, 0, 'M', '0', '0', '', 'date', 'admin', sysdate(), '', NULL, '日报管理目录', 'DailyReportRoot');
SELECT @dailyReportRootId := LAST_INSERT_ID();

-- ---- 日报填写 ----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('日报填写', @dailyReportRootId, 1, 'write', 'project/dailyReport/write', 1, 0, 'C', '0', '0', 'project:dailyReport:list', 'edit', 'admin', sysdate(), '', NULL, '日报填写菜单', 'DailyReportWrite');
SELECT @dailyReportWriteId := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('提交', @dailyReportWriteId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('删除', @dailyReportWriteId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:remove', '#', 'admin', sysdate(), '', NULL, '');

-- ---- 工作日报动态（无按钮）----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('工作日报动态', @dailyReportRootId, 2, 'activity', 'project/dailyReport/activity', 1, 0, 'C', '0', '0', 'project:dailyReport:activity', 'peoples', 'admin', sysdate(), '', NULL, '工作日报动态菜单', 'DailyReportActivity');

-- ---- 工作日历（无按钮）----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('工作日历', @dailyReportRootId, 3, 'workCalendar', 'project/workCalendar/index', 1, 0, 'C', '0', '0', 'project:workCalendar:list', 'date-range', 'admin', sysdate(), '', NULL, '工作日历菜单', 'WorkCalendar');

-- ---- 团队日报（无按钮）----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('团队日报', @dailyReportRootId, 4, 'teamReport', 'project/dailyReport/teamReport', 1, 0, 'C', '0', '0', 'project:dailyReport:activity', 'peoples', 'admin', sysdate(), '', NULL, '团队日报菜单', 'TeamDailyReport');

-- ---- 项目人天统计 ----
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark, route_name)
VALUES ('项目人天统计', @dailyReportRootId, 5, 'stats', 'project/dailyReport/stats', 1, 0, 'C', '0', '0', 'project:dailyReport:list', 'chart', 'admin', sysdate(), '', NULL, '项目人天统计菜单', 'ProjectStats');
SELECT @statsMenuId := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查询', @statsMenuId, 0, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('编辑', @statsMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:dailyReport:edit', '#', 'admin', sysdate(), '', NULL, '');

-- ========================================
-- 二级区域管理（挂在系统管理 parent_id=1 下）
-- ========================================

DELETE FROM sys_menu WHERE path = 'secondaryRegion' AND parent_id = 1;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('二级区域管理', 1, 10, 'secondaryRegion', 'project/secondaryRegion/index', 1, 0, 'C', '0', '0', 'project:secondaryRegion:list', 'tree', 'admin', sysdate(), '', NULL, '二级区域管理菜单');
SELECT @secondaryRegionMenuId := LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('查询', @secondaryRegionMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'project:secondaryRegion:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('新增', @secondaryRegionMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'project:secondaryRegion:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('修改', @secondaryRegionMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'project:secondaryRegion:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('删除', @secondaryRegionMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'project:secondaryRegion:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('导出', @secondaryRegionMenuId, 5, '#', '', 1, 0, 'F', '0', '0', 'project:secondaryRegion:export', '#', 'admin', sysdate(), '', NULL, '');

-- ========================================
-- 清理孤立的角色菜单关联
-- ========================================
DELETE FROM sys_role_menu WHERE menu_id NOT IN (SELECT menu_id FROM sys_menu);

-- ========================================
-- 隐藏路由角色分配
-- 说明：隐藏路由（visible=1 的 C 类菜单）需要显式加入 sys_role_menu，
--       否则 Vue Router 不会注册该路由，点击会报 404。
--       生产环境各角色请通过 系统管理->角色管理 重新分配。
--       以下仅为测试角色(role_id=104)的默认配置。
-- ========================================
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 104, m.menu_id FROM sys_menu m
WHERE m.menu_type = 'C' AND m.visible = '1'
  AND m.perms IN ('project:project:edit');


-- 项目经理变更 - 补充缺失的按钮菜单
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
(2246, '批量变更', 2163, 2, '#', '', '', '', 1, 0, 'F', '0', '0', 'project:managerChange:batchChange', '#', 'admin', NOW(), '', NULL, ''),
(2247, '变更记录', 2163, 3, '#', '', '', '', 1, 0, 'F', '0', '0', 'project:managerChange:query', '#', 'admin', NOW(), '', NULL, '');

-- 项目人员管理 - 补充缺失的查询按钮
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2248, '查询', 2165, 0, '#', '', '', '', 1, 0, 'F', '0', '0', 'project:member:list', '#', 'admin', NOW(), '', NULL, '');
