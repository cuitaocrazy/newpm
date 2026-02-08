-- 重建 pm_project 表
-- 注意：此操作会删除表中的所有数据，请先备份！

-- 备份现有数据（可选）
-- CREATE TABLE pm_project_backup AS SELECT * FROM pm_project;

-- 删除旧表
DROP TABLE IF EXISTS `pm_project`;

-- 创建新表
CREATE TABLE `pm_project` (
  `project_id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `project_code` varchar(50) NOT NULL COMMENT '项目编号(格式:行业-一级区域-二级区域-简称-年份)',
  `project_name` varchar(200) NOT NULL COMMENT '项目名称',
  `project_full_name` varchar(500) DEFAULT NULL COMMENT '项目全称',
  `industry` varchar(50) DEFAULT NULL COMMENT '行业',
  `region` varchar(50) DEFAULT NULL COMMENT '一级区域',
  `province_id` bigint DEFAULT NULL COMMENT '二级区域ID（关联pm_province表 ）',
  `short_name` varchar(50) DEFAULT NULL COMMENT '简称',
  `established_year` int DEFAULT NULL COMMENT '立项年份',
  `project_category` varchar(50) DEFAULT NULL COMMENT '项目分类',
  `project_dept` varchar(100) DEFAULT NULL COMMENT '项目部门',
  `project_status` varchar(50) DEFAULT NULL COMMENT '项目状态',
  `acceptance_status` varchar(50) DEFAULT NULL COMMENT '验收状态',
  `estimated_workload` decimal(10,2) DEFAULT NULL COMMENT '预估工作量(人天)',
  `actual_workload` decimal(10,2) DEFAULT '0.00' COMMENT '实际工作量(人天)',
  `project_address` varchar(500) DEFAULT NULL COMMENT '项目地址',
  `project_plan` text COMMENT '项目计划',
  `project_description` text COMMENT '项目描述',
  `project_manager_id` bigint DEFAULT NULL COMMENT '项目经理ID',
  `market_manager_id` bigint DEFAULT NULL COMMENT '市场经理ID',
  `participants` varchar(500) DEFAULT NULL COMMENT '参与人员ID列表(逗号分隔)',
  `sales_manager_id` bigint DEFAULT NULL COMMENT '销售负责人ID',
  `sales_contact` varchar(50) DEFAULT NULL COMMENT '销售联系方式',
  `team_leader_id` bigint DEFAULT NULL COMMENT '团队负责人ID',
  `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
  `customer_contact_id` bigint DEFAULT NULL COMMENT '客户联系人ID',
  `merchant_contact` varchar(100) DEFAULT NULL COMMENT '商户联系人',
  `merchant_phone` varchar(50) DEFAULT NULL COMMENT '商户联系方式',
  `start_date` date DEFAULT NULL COMMENT '启动日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `production_date` date DEFAULT NULL COMMENT '投产日期',
  `acceptance_date` date DEFAULT NULL COMMENT '验收日期',
  `project_budget` decimal(15,2) DEFAULT NULL COMMENT '项目预算(元)',
  `project_cost` decimal(15,2) DEFAULT NULL COMMENT '项目费用(元)',
  `expense_budget` decimal(15,2) DEFAULT NULL COMMENT '费用预算(元)',
  `cost_budget` decimal(15,2) DEFAULT NULL COMMENT '成本预算(元)',
  `labor_cost` decimal(15,2) DEFAULT NULL COMMENT '人力费用(元)',
  `purchase_cost` decimal(15,2) DEFAULT '0.00' COMMENT '采购成本',
  `approval_status` varchar(20) DEFAULT '0' COMMENT '审批状态(0待审核/1已通过/2已拒绝)',
  `approval_reason` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `industry_code` varchar(50) DEFAULT NULL COMMENT '行业代码',
  `region_code` varchar(50) DEFAULT NULL COMMENT '区域代码(字典:sys_yjqy)',
  `approval_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approver_id` varchar(64) DEFAULT NULL COMMENT '审批人',
  `remark` text COMMENT '备注',
  `tax_rate` decimal(5,2) DEFAULT NULL COMMENT '税率(%)',
  `confirm_user_id` bigint DEFAULT NULL COMMENT '确认人ID',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `reserved_field1` varchar(64) DEFAULT NULL COMMENT '备用域1',
  `reserved_field2` varchar(64) DEFAULT NULL COMMENT '备用域2',
  `reserved_field3` varchar(64) DEFAULT NULL COMMENT '备用域3',
  `reserved_field4` varchar(64) DEFAULT NULL COMMENT '备用域4',
  `reserved_field5` varchar(64) DEFAULT NULL COMMENT '备用域5',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `revenue_confirm_status` char(1) DEFAULT '0' COMMENT '收入确认状态（0=未确认 1=已确认）',
  `revenue_confirm_year` varchar(20) DEFAULT NULL COMMENT '收入确认年度',
  `confirm_amount` decimal(15,2) DEFAULT NULL COMMENT '确认金额（含税）',
  `after_tax_amount` decimal(15,2) DEFAULT NULL COMMENT '税后金额',
  `company_revenue_confirmed_by_name` varchar(64) DEFAULT NULL COMMENT '公司收入确认人姓名',
  PRIMARY KEY (`project_id`),
  UNIQUE KEY `uk_project_code` (`project_code`),
  KEY `idx_approval_status` (`approval_status`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目管理表';

-- 如果之前有备份，可以恢复数据（根据实际情况调整字段）
-- INSERT INTO pm_project SELECT * FROM pm_project_backup;
