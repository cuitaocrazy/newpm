-- ----------------------------
-- 修复项目管理菜单层级结构
-- 日期: 2026-02-09
-- 说明: 将"编辑项目"和"项目详情"从一级菜单下移到"项目管理"二级菜单下
-- ----------------------------

USE `ry-vue`;

-- 将编辑项目和项目详情的父菜单从 2059（一级菜单）改为 2061（项目管理二级菜单）
UPDATE sys_menu SET parent_id = 2061 WHERE menu_id IN (2043, 2044);

-- 修复路径：移除 list/ 前缀，因为父菜单已经是 list 了
UPDATE sys_menu SET path = 'edit/:projectId(\\d+)' WHERE menu_id = 2043;
UPDATE sys_menu SET path = 'detail/:projectId(\\d+)' WHERE menu_id = 2044;

-- 验证修复结果
SELECT '=== 项目管理菜单层级结构 ===' AS info;
SELECT
    menu_id,
    menu_name,
    parent_id,
    order_num,
    path,
    visible,
    CASE
        WHEN parent_id = 0 THEN '一级菜单'
        WHEN parent_id = 2059 THEN '二级菜单'
        WHEN parent_id = 2061 THEN '三级菜单（项目管理下）'
        WHEN parent_id = 2069 THEN '三级菜单（立项审核下）'
        ELSE CONCAT('子菜单(parent=', parent_id, ')')
    END AS menu_level
FROM sys_menu
WHERE menu_name LIKE '%项目%' OR menu_name LIKE '%立项%'
ORDER BY
    CASE WHEN parent_id = 0 THEN 1
         WHEN parent_id = 2059 THEN 2
         WHEN parent_id IN (2061, 2069) THEN 3
         ELSE 4 END,
    parent_id,
    order_num;
