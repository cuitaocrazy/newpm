-- 更新立项申请菜单的 path 和 component
UPDATE sys_menu 
SET path = 'project/apply', component = 'project/project/apply' 
WHERE menu_name = '立项申请' 
  AND parent_id IN (
    SELECT menu_id FROM (
      SELECT menu_id FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0
    ) AS temp
  );

-- 更新项目列表菜单的 path
UPDATE sys_menu 
SET path = 'project' 
WHERE menu_name = '项目列表' 
  AND parent_id IN (
    SELECT menu_id FROM (
      SELECT menu_id FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0
    ) AS temp
  );

-- 查询验证更新结果
SELECT menu_id, menu_name, path, component, parent_id 
FROM sys_menu 
WHERE menu_name IN ('立项申请', '项目列表') 
  AND parent_id IN (
    SELECT menu_id FROM (
      SELECT menu_id FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0
    ) AS temp2
  )
ORDER BY order_num;
