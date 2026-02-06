-- 更新立项申请菜单的 component 路径
UPDATE sys_menu 
SET component = 'project/project/apply' 
WHERE menu_name = '立项申请' 
  AND path = 'apply' 
  AND parent_id IN (
    SELECT menu_id FROM (
      SELECT menu_id FROM sys_menu WHERE menu_name = '项目管理' AND parent_id = 0
    ) AS temp
  );

-- 查询验证更新结果
SELECT menu_id, menu_name, path, component, parent_id 
FROM sys_menu 
WHERE menu_name = '立项申请' AND path = 'apply';
