-- ====================================================
-- 项目审核权限配置SQL
-- 功能：为项目管理模块添加审核权限按钮
-- 日期：2026-02-12
-- ====================================================

-- 查询项目管理菜单ID（假设是项目列表菜单）
SET @project_menu_id = (SELECT menu_id FROM sys_menu WHERE path = '/project/project' AND menu_type = 'C' LIMIT 1);

-- 添加审核权限按钮
INSERT INTO sys_menu (
    menu_name,
    parent_id,
    order_num,
    path,
    component,
    is_frame,
    is_cache,
    menu_type,
    visible,
    status,
    perms,
    icon,
    create_by,
    create_time,
    update_by,
    update_time,
    remark
)
VALUES (
    '项目审核',                    -- 菜单名称
    @project_menu_id,              -- 父菜单ID（项目管理）
    6,                             -- 排序
    '',                            -- 路由地址（空，因为是按钮）
    '',                            -- 组件路径（空）
    1,                             -- 非外链
    0,                             -- 不缓存
    'F',                           -- 类型：按钮（F）
    '0',                           -- 显示
    '0',                           -- 正常
    'project:approval:approve',    -- 权限标识
    '#',                           -- 图标
    'admin',                       -- 创建者
    NOW(),                         -- 创建时间
    '',                            -- 更新者
    NULL,                          -- 更新时间
    '项目审核权限'                  -- 备注
);

-- 查看新增的权限
SELECT menu_id, menu_name, perms, menu_type FROM sys_menu WHERE perms = 'project:approval:approve';

-- 提示：需要在角色管理中将此权限分配给相应的角色（如项目主管、审核员等）
