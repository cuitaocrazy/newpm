-- ========================================
-- 删除重复的"市场管理"菜单
-- ========================================
-- 说明：之前手动添加了一个 path='scgl' 的市场管理菜单
-- 现在使用 path='market' 的新菜单，需要删除旧的重复菜单

-- 删除旧的"市场管理"菜单（path='scgl'）及其所有子菜单
DELETE FROM sys_menu
WHERE menu_name = '市场管理'
  AND path = 'scgl'
  AND parent_id = 0;

-- 如果有子菜单挂在旧的市场管理下，也需要删除
-- 先查找旧菜单的ID（如果还存在的话）
SET @oldMarketMenuId = (SELECT menu_id FROM sys_menu WHERE menu_name = '市场管理' AND path = 'scgl' LIMIT 1);

-- 删除挂在旧菜单下的子菜单
DELETE FROM sys_menu WHERE parent_id = @oldMarketMenuId AND @oldMarketMenuId IS NOT NULL;

-- 再次确认删除旧的市场管理菜单
DELETE FROM sys_menu WHERE menu_id = @oldMarketMenuId AND @oldMarketMenuId IS NOT NULL;
