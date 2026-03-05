-- 立项审核状态新增"退回待审核"
-- value=3, label=退回待审核, listClass=warning
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark)
VALUES (5, '退回待审核', '3', 'sys_spzt', NULL, 'warning', 'N', '0', 'admin', NOW(), 'admin', NOW(), NULL);
