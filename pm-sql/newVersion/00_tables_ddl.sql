CREATE TABLE `pm_customer` (
  `customer_id` bigint NOT NULL AUTO_INCREMENT COMMENT '客户主键ID',
  `customer_simple_name` varchar(200) NOT NULL COMMENT '客户简称',
  `customer_all_name` varchar(200) NOT NULL COMMENT '客户全称',
  `industry` varchar(100) DEFAULT NULL COMMENT '所属行业(字典表 字典类型industry)',
  `region` varchar(100) DEFAULT NULL COMMENT '所属区域(字典表 : 字典类型sys_yjqy)',
  `sales_manager_id` bigint DEFAULT NULL COMMENT '销售负责人ID',
  `sales_manager_name` varchar(100) DEFAULT NULL COMMENT '销售负责人姓名',
  `office_address` varchar(500) DEFAULT NULL COMMENT '办公地址',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`customer_id`),
  KEY `idx_sales_manager_id` (`sales_manager_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户信息表';

CREATE TABLE `pm_customer_contact` (
  `contact_id` bigint NOT NULL AUTO_INCREMENT COMMENT '联系人主键ID',
  `customer_id` bigint NOT NULL COMMENT '客户ID',
  `contact_name` varchar(100) NOT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(50) NOT NULL COMMENT '联系人电话',
  `contact_tag` varchar(100) NOT NULL COMMENT '联系人标签(字典表，字典类型contact_tag)',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常 1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`contact_id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户联系人表';