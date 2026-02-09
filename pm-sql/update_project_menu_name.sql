-- ========================================
-- 更新项目列表菜单名称为项目管理
-- 执行日期：2026-02-08
-- ========================================

-- 更新二级菜单名称
UPDATE sys_menu
SET menu_name = '项目管理', remark = '项目管理菜单'
WHERE menu_name = '项目列表' AND path = 'list';

-- 更新按钮权限名称
UPDATE sys_menu SET menu_name = '项目管理查询' WHERE menu_name = '项目列表查询';
UPDATE sys_menu SET menu_name = '项目管理新增' WHERE menu_name = '项目列表新增';
UPDATE sys_menu SET menu_name = '项目管理修改' WHERE menu_name = '项目列表修改';
UPDATE sys_menu SET menu_name = '项目管理删除' WHERE menu_name = '项目列表删除';
UPDATE sys_menu SET menu_name = '项目管理导出' WHERE menu_name = '项目列表导出';

-- 查看更新结果
SELECT menu_id, menu_name, parent_id, path, perms, remark
FROM sys_menu
WHERE menu_name LIKE '%项目管理%' OR menu_name LIKE '%项目列表%'
ORDER BY parent_id, order_num;
