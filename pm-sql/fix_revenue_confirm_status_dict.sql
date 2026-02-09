-- ========================================
-- 添加收入确认状态字典
-- 创建时间: 2026-02-06
-- 说明: 收入确认状态（0=未确认 1=已确认）
-- ========================================

-- 1. 添加字典类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
VALUES ('收入确认状态', 'sys_srqrzt', '0', 'admin', sysdate(), '收入确认状态列表')
ON DUPLICATE KEY UPDATE dict_name = '收入确认状态';

-- 2. 添加字典数据
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
VALUES
(1, '未确认', '0', 'sys_srqrzt', '', 'warning', 'N', '0', 'admin', sysdate(), '收入未确认'),
(2, '已确认', '1', 'sys_srqrzt', '', 'success', 'N', '0', 'admin', sysdate(), '收入已确认')
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label);

-- 验证结果
SELECT d.dict_type, d.dict_name, dd.dict_label, dd.dict_value, dd.list_class
FROM sys_dict_type d
LEFT JOIN sys_dict_data dd ON d.dict_type = dd.dict_type
WHERE d.dict_type = 'sys_srqrzt'
ORDER BY dd.dict_sort;
