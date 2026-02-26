-- =============================================
-- 状态字典颜色标识统一优化
-- 日期: 2026-02-26
-- 说明: 为所有状态类字典添加颜色标识，便于在列表中快速识别
--       颜色语义：info=初始/中性，primary=进行中，warning=待处理/关键节点，success=完成/正向，danger=异常/拒绝
-- =============================================

-- ----------------------------
-- sys_spzt 审批状态
-- ----------------------------
UPDATE sys_dict_data SET list_class = 'warning' WHERE dict_code = 200;  -- 待审核
UPDATE sys_dict_data SET list_class = 'success' WHERE dict_code = 201;  -- 审核通过
UPDATE sys_dict_data SET list_class = 'danger'  WHERE dict_code = 202;  -- 审核拒绝

-- ----------------------------
-- sys_fkzt 付款状态
-- ----------------------------
UPDATE sys_dict_data SET list_class = 'info'    WHERE dict_code = 104;  -- 1-未开未付
UPDATE sys_dict_data SET list_class = 'primary' WHERE dict_code = 105;  -- 2-已提交验收材料
UPDATE sys_dict_data SET list_class = 'primary' WHERE dict_code = 106;  -- 3-验收材料已审核
UPDATE sys_dict_data SET list_class = 'warning' WHERE dict_code = 107;  -- 4-待通知开票
UPDATE sys_dict_data SET list_class = 'warning' WHERE dict_code = 108;  -- 5-已通知开票
UPDATE sys_dict_data SET list_class = 'warning' WHERE dict_code = 109;  -- 6-已提交发票
UPDATE sys_dict_data SET list_class = 'success' WHERE dict_code = 110;  -- 7-已收到回款
UPDATE sys_dict_data SET list_class = 'danger'  WHERE dict_code = 352;  -- 不再回款（原 warning）

-- ----------------------------
-- sys_htzt 合同状态（修正 2-未签 和 3-待变更 的颜色）
-- ----------------------------
UPDATE sys_dict_data SET list_class = 'success' WHERE dict_code = 133;  -- 1-已签
UPDATE sys_dict_data SET list_class = 'info'    WHERE dict_code = 134;  -- 2-未签（原 warning）
UPDATE sys_dict_data SET list_class = 'warning' WHERE dict_code = 135;  -- 3-待变更（原 info）
UPDATE sys_dict_data SET list_class = 'primary' WHERE dict_code = 136;  -- 4-已变更
UPDATE sys_dict_data SET list_class = 'danger'  WHERE dict_code = 164;  -- 5-合同作废

-- ----------------------------
-- sys_xmjd 项目阶段
-- ----------------------------
UPDATE sys_dict_data SET list_class = 'info'    WHERE dict_code = 358;  -- 0-其他
UPDATE sys_dict_data SET list_class = 'info'    WHERE dict_code = 143;  -- 1-售前支持
UPDATE sys_dict_data SET list_class = 'primary' WHERE dict_code = 144;  -- 2-需求及设计
UPDATE sys_dict_data SET list_class = 'primary' WHERE dict_code = 145;  -- 3-开发及自测
UPDATE sys_dict_data SET list_class = 'warning' WHERE dict_code = 146;  -- 4-验收测试
UPDATE sys_dict_data SET list_class = 'success' WHERE dict_code = 147;  -- 5-系统投产
UPDATE sys_dict_data SET list_class = 'success' WHERE dict_code = 148;  -- 6-免维期维护
UPDATE sys_dict_data SET list_class = 'success' WHERE dict_code = 161;  -- 7-项目结项

-- ----------------------------
-- sys_qrzt 确认状态
-- ----------------------------
UPDATE sys_dict_data SET list_class = 'info'    WHERE dict_code = 204;  -- 0-未确认（原 warning）
UPDATE sys_dict_data SET list_class = 'info'    WHERE dict_code = 113;  -- 1-未确认（原 primary）
UPDATE sys_dict_data SET list_class = 'warning' WHERE dict_code = 114;  -- 2-待确认收入（原 success）
-- dict_code 115: 3-已确认收入 已是 success，无需修改
-- dict_code 116: 4-无法确认   已是 danger，无需修改

-- ----------------------------
-- sys_srqrzt 收入确认状态
-- ----------------------------
UPDATE sys_dict_data SET list_class = 'info'    WHERE dict_code = 210;  -- 0-未确认（原 warning）
-- dict_code 211: 1-已确认 已是 success，无需修改

-- sys_yszt 验收状态 已正确（未验收=warning，已验收=success），无需修改
