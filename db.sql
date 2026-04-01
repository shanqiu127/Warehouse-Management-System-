-- =============================================
-- 仓库管理系统 - 数据库建表及初始化脚本
-- MySQL 8.0
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `warehouse_management` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `warehouse_management`;

DROP VIEW IF EXISTS `v_sales_detail`;
DROP VIEW IF EXISTS `v_purchase_detail`;
DROP VIEW IF EXISTS `v_goods_detail`;
DROP PROCEDURE IF EXISTS `sp_purchase_add_stock`;

-- =============================================
-- 一、系统管理及认证体系表
-- =============================================

-- 1.1 用户表 (sys_user)
-- 存储用户登录信息，区分管理员和普通员工
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: superadmin-超级管理员, admin-管理员, employee-普通用户',
    `dept_id` BIGINT DEFAULT NULL COMMENT '所属部门ID，superadmin 允许为空',
    `is_superadmin` TINYINT GENERATED ALWAYS AS (CASE WHEN `role` = 'superadmin' THEN 1 ELSE NULL END) STORED COMMENT '超级管理员唯一约束辅助列',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `current_login_time` DATETIME DEFAULT NULL COMMENT '本次登录时间',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '上次登录时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_only_one_superadmin` (`is_superadmin`),
    KEY `idx_role_dept_status` (`role`, `dept_id`, `status`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 1.2 部门表 (sys_dept)
-- 存储组织架构部门信息
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dept_name` VARCHAR(50) NOT NULL COMMENT '部门名称',
    `dept_code` VARCHAR(20) NOT NULL COMMENT '部门编码',
    `leader` VARCHAR(50) DEFAULT NULL COMMENT '部门负责人',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `status` TINYINT NOT NULL DEFAULT 2 COMMENT '状态: 1-待审批, 2-已生效, 3-已驳回',
    `requester_id` BIGINT DEFAULT NULL COMMENT '提交人ID',
    `requester_name` VARCHAR(50) DEFAULT NULL COMMENT '提交人姓名',
    `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID',
    `approver_name` VARCHAR(50) DEFAULT NULL COMMENT '审批人姓名',
    `approval_remark` VARCHAR(200) DEFAULT NULL COMMENT '审批备注',
    `approved_at` DATETIME DEFAULT NULL COMMENT '审批通过时间',
    `rejected_at` DATETIME DEFAULT NULL COMMENT '审批驳回时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dept_code` (`dept_code`),
    KEY `idx_status_rejected_at` (`status`, `rejected_at`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- 1.3 员工表 (sys_employee)
-- 存储员工基本信息，关联部门
DROP TABLE IF EXISTS `sys_employee`;
CREATE TABLE `sys_employee` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '关联用户ID',
    `emp_code` VARCHAR(20) NOT NULL COMMENT '员工工号',
    `emp_name` VARCHAR(50) NOT NULL COMMENT '员工姓名',
    `dept_id` BIGINT NOT NULL COMMENT '部门ID',
    `position` VARCHAR(50) DEFAULT NULL COMMENT '职位',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-在职, 0-离职',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_emp_code` (`emp_code`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_dept_status` (`dept_id`, `status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工表';

-- 1.4 公告表 (sys_notice)
-- 存储系统公告信息
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title` VARCHAR(100) NOT NULL COMMENT '公告标题',
    `content` TEXT COMMENT '公告内容',
    `target_role` VARCHAR(20) NOT NULL DEFAULT 'all' COMMENT '受众角色: admin/employee/all',
    `target_dept_id` BIGINT DEFAULT NULL COMMENT '目标部门ID，空表示该角色全体',
    `publisher` VARCHAR(50) NOT NULL COMMENT '发布人',
    `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 1-已发布, 0-草稿',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_target_role_dept_status` (`target_role`, `target_dept_id`, `status`),
    KEY `idx_status` (`status`),
    KEY `idx_publish_time` (`publish_time`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

DROP TABLE IF EXISTS `sys_error_log`;
CREATE TABLE `sys_error_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `request_uri` VARCHAR(200) DEFAULT NULL COMMENT '请求路径',
    `method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
    `status_code` INT DEFAULT NULL COMMENT '响应状态码',
    `error_type` VARCHAR(100) DEFAULT NULL COMMENT '异常类型',
    `message` VARCHAR(500) DEFAULT NULL COMMENT '异常摘要',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统错误日志表';

-- 1.5 IP 策略表 (sys_ip_policy)
-- 存储登录来源 IP 白名单/黑名单策略
DROP TABLE IF EXISTS `sys_ip_policy`;
CREATE TABLE `sys_ip_policy` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `policy_name` VARCHAR(100) NOT NULL COMMENT '策略名称',
    `ip_cidr` VARCHAR(64) NOT NULL COMMENT 'IP或CIDR网段',
    `allow_flag` TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许: 1-允许, 0-拒绝',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    `priority` INT NOT NULL DEFAULT 100 COMMENT '优先级(数值越小优先级越高)',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    KEY `idx_ip_cidr` (`ip_cidr`),
    KEY `idx_priority_status` (`priority`, `status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IP策略表';

-- 1.6 登录日志表 (sys_login_log)
-- 存储登录成功/失败记录
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
    `user_agent` VARCHAR(300) DEFAULT NULL COMMENT '客户端标识',
    `success_flag` TINYINT NOT NULL DEFAULT 1 COMMENT '登录结果: 1-成功, 0-失败',
    `fail_reason` VARCHAR(200) DEFAULT NULL COMMENT '失败原因',
    `login_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_username` (`username`),
    KEY `idx_ip` (`ip`),
    KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- 1.7 操作日志表 (sys_operation_log)
-- 存储关键业务操作审计记录
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '操作人用户名',
    `module` VARCHAR(50) DEFAULT NULL COMMENT '模块名称',
    `action` VARCHAR(50) DEFAULT NULL COMMENT '操作动作',
    `target_type` VARCHAR(50) DEFAULT NULL COMMENT '目标类型',
    `target_id` VARCHAR(64) DEFAULT NULL COMMENT '目标ID',
    `before_data` TEXT DEFAULT NULL COMMENT '变更前数据(JSON)',
    `after_data` TEXT DEFAULT NULL COMMENT '变更后数据(JSON)',
    `request_uri` VARCHAR(200) DEFAULT NULL COMMENT '请求路径',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT '来源IP',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    PRIMARY KEY (`id`),
    KEY `idx_module_action` (`module`, `action`),
    KEY `idx_username` (`username`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- =============================================
-- 二、基础资料表
-- =============================================

-- 2.1 供应商表 (base_supplier)
-- 存储供应商基本信息
DROP TABLE IF EXISTS `base_supplier`;
CREATE TABLE `base_supplier` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `supplier_code` VARCHAR(20) NOT NULL COMMENT '供应商编码',
    `supplier_name` VARCHAR(100) NOT NULL COMMENT '供应商名称',
    `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `address` VARCHAR(200) DEFAULT NULL COMMENT '地址',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_supplier_code` (`supplier_code`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商表';

-- 2.2 商品表 (base_goods)
-- 存储商品基本信息，关联供应商，包含库存字段
DROP TABLE IF EXISTS `base_goods`;
CREATE TABLE `base_goods` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `goods_code` VARCHAR(20) NOT NULL COMMENT '商品编码',
    `goods_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '商品类别',
    `brand` VARCHAR(50) DEFAULT NULL COMMENT '商品品牌(用于图表聚合)',
    `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
    `purchase_price` DECIMAL(10,2) DEFAULT NULL COMMENT '进价',
    `sale_price` DECIMAL(10,2) DEFAULT NULL COMMENT '售价',
    `stock` INT NOT NULL DEFAULT 0 COMMENT '当前库存量',
    `warning_stock` INT NOT NULL DEFAULT 10 COMMENT '库存预警阈值',
    `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-上架, 0-下架',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_goods_code` (`goods_code`),
    KEY `idx_supplier_id` (`supplier_id`),
    KEY `idx_supplier_status` (`supplier_id`, `status`),
    KEY `idx_brand` (`brand`),
    KEY `idx_stock` (`stock`),
    KEY `idx_warning_stock` (`warning_stock`),
    KEY `idx_category` (`category`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- =============================================
-- 三、核心业务票据表
-- =============================================

-- 3.1 进货表 (biz_purchase)
-- 记录商品进货信息，库存增加
DROP TABLE IF EXISTS `biz_purchase`;
CREATE TABLE `biz_purchase` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `purchase_no` VARCHAR(30) NOT NULL COMMENT '进货单号',
    `goods_id` BIGINT NOT NULL COMMENT '商品ID',
    `goods_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称(冗余字段)',
    `quantity` INT NOT NULL COMMENT '进货数量',
    `unit_price` DECIMAL(10,2) NOT NULL COMMENT '进货单价',
    `total_price` DECIMAL(10,2) NOT NULL COMMENT '总金额',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名(冗余字段)',
    `operation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作发生时间',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `biz_status` TINYINT NOT NULL DEFAULT 1 COMMENT '业务状态: 1-正常, 2-已作废, 3-红冲单',
    `source_id` BIGINT DEFAULT NULL COMMENT '红冲来源单ID',
    `void_time` DATETIME DEFAULT NULL COMMENT '作废时间',
    `void_reason` VARCHAR(200) DEFAULT NULL COMMENT '作废原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_purchase_no` (`purchase_no`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_goods_time` (`goods_id`, `operation_time`),
    KEY `idx_goods_status_time` (`goods_id`, `biz_status`, `is_deleted`, `operation_time`, `id`),
    KEY `idx_biz_status` (`biz_status`),
    KEY `idx_source_id` (`source_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_operation_time` (`operation_time`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='进货表';

-- 3.2 退货表 (biz_purchase_return)
-- 记录商品退给供应商的信息，库存减少；支持关联来源进货单
DROP TABLE IF EXISTS `biz_purchase_return`;
CREATE TABLE `biz_purchase_return` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `return_no` VARCHAR(30) NOT NULL COMMENT '退货单号',
    `source_purchase_id` BIGINT DEFAULT NULL COMMENT '来源进货单ID',
    `source_purchase_no` VARCHAR(30) DEFAULT NULL COMMENT '来源进货单号',
    `goods_id` BIGINT NOT NULL COMMENT '商品ID',
    `goods_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称(冗余字段)',
    `quantity` INT NOT NULL COMMENT '退货数量',
    `unit_price` DECIMAL(10,2) NOT NULL COMMENT '退货单价',
    `total_price` DECIMAL(10,2) NOT NULL COMMENT '总金额',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名(冗余字段)',
    `operation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作发生时间',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `biz_status` TINYINT NOT NULL DEFAULT 1 COMMENT '业务状态: 1-正常, 2-已作废, 3-红冲单',
    `source_id` BIGINT DEFAULT NULL COMMENT '红冲来源单ID',
    `void_time` DATETIME DEFAULT NULL COMMENT '作废时间',
    `void_reason` VARCHAR(200) DEFAULT NULL COMMENT '作废原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_return_no` (`return_no`),
    KEY `idx_source_purchase_id` (`source_purchase_id`),
    KEY `idx_source_purchase_no` (`source_purchase_no`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_goods_time` (`goods_id`, `operation_time`),
    KEY `idx_stat_time_status` (`operation_time`, `biz_status`, `is_deleted`, `goods_id`),
    KEY `idx_biz_status` (`biz_status`),
    KEY `idx_source_id` (`source_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_operation_time` (`operation_time`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退货表(商品退给供应商)';

-- 3.3 销售表 (biz_sales)
-- 记录商品销售信息，库存减少
DROP TABLE IF EXISTS `biz_sales`;
CREATE TABLE `biz_sales` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sales_no` VARCHAR(30) NOT NULL COMMENT '销售单号',
    `goods_id` BIGINT NOT NULL COMMENT '商品ID',
    `goods_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称(冗余字段)',
    `quantity` INT NOT NULL COMMENT '销售数量',
    `unit_price` DECIMAL(10,2) NOT NULL COMMENT '销售单价',
    `cost_unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT '成本单价快照',
    `cost_total_price` DECIMAL(12,2) DEFAULT NULL COMMENT '成本总额快照',
    `cost_source` VARCHAR(30) DEFAULT NULL COMMENT '成本来源: RECENT_PURCHASE/GOODS_PRICE/ZERO_FALLBACK',
    `total_price` DECIMAL(10,2) NOT NULL COMMENT '总金额',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名(冗余字段)',
    `operation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作发生时间',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `biz_status` TINYINT NOT NULL DEFAULT 1 COMMENT '业务状态: 1-正常, 2-已作废, 3-红冲单',
    `source_id` BIGINT DEFAULT NULL COMMENT '红冲来源单ID',
    `void_time` DATETIME DEFAULT NULL COMMENT '作废时间',
    `void_reason` VARCHAR(200) DEFAULT NULL COMMENT '作废原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sales_no` (`sales_no`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_goods_time` (`goods_id`, `operation_time`),
    KEY `idx_stat_time_status` (`operation_time`, `biz_status`, `is_deleted`, `goods_id`),
    KEY `idx_cost_stat` (`operation_time`, `biz_status`, `is_deleted`, `cost_total_price`),
    KEY `idx_biz_status` (`biz_status`),
    KEY `idx_source_id` (`source_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_operation_time` (`operation_time`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售表';

-- 3.4 客退表 (biz_sales_return)
-- 记录客户退货信息，库存增加
DROP TABLE IF EXISTS `biz_sales_return`;
CREATE TABLE `biz_sales_return` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `return_no` VARCHAR(30) NOT NULL COMMENT '退货单号',
    `source_sales_id` BIGINT DEFAULT NULL COMMENT '来源销售单ID',
    `source_sales_no` VARCHAR(30) DEFAULT NULL COMMENT '来源销售单号',
    `goods_id` BIGINT NOT NULL COMMENT '商品ID',
    `goods_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称(冗余字段)',
    `quantity` INT NOT NULL COMMENT '退货数量',
    `unit_price` DECIMAL(10,2) NOT NULL COMMENT '退货单价',
    `cost_unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT '成本单价快照',
    `cost_total_price` DECIMAL(12,2) DEFAULT NULL COMMENT '成本总额快照',
    `cost_source` VARCHAR(30) DEFAULT NULL COMMENT '成本来源: SOURCE_SALE/RECENT_PURCHASE/GOODS_PRICE/ZERO_FALLBACK',
    `total_price` DECIMAL(10,2) NOT NULL COMMENT '总金额',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名(冗余字段)',
    `operation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作发生时间',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `biz_status` TINYINT NOT NULL DEFAULT 1 COMMENT '业务状态: 1-正常, 2-已作废, 3-红冲单',
    `source_id` BIGINT DEFAULT NULL COMMENT '红冲来源单ID',
    `void_time` DATETIME DEFAULT NULL COMMENT '作废时间',
    `void_reason` VARCHAR(200) DEFAULT NULL COMMENT '作废原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_return_no` (`return_no`),
    KEY `idx_goods_id` (`goods_id`),
    KEY `idx_goods_time` (`goods_id`, `operation_time`),
    KEY `idx_stat_time_status` (`operation_time`, `biz_status`, `is_deleted`, `goods_id`, `source_sales_id`),
    KEY `idx_cost_stat` (`operation_time`, `biz_status`, `is_deleted`, `cost_total_price`),
    KEY `idx_source_sales_id` (`source_sales_id`),
    KEY `idx_source_sales_no` (`source_sales_no`),
    KEY `idx_biz_status` (`biz_status`),
    KEY `idx_source_id` (`source_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_operation_time` (`operation_time`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客退表(客户退回商品)';

-- 3.5 作废审批单表 (biz_approval_order)
-- 记录普通员工发起的“作废/作废并红冲”审批请求，由管理员审批后执行
DROP TABLE IF EXISTS `biz_approval_order`;
CREATE TABLE `biz_approval_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `approval_no` VARCHAR(32) NOT NULL COMMENT '审批单号',
    `biz_type` VARCHAR(30) NOT NULL COMMENT '业务类型: purchase/purchase_return/sales/sales_return',
    `biz_id` BIGINT NOT NULL COMMENT '业务单据ID',
    `biz_no` VARCHAR(30) DEFAULT NULL COMMENT '业务单号(冗余)',
    `request_action` VARCHAR(20) NOT NULL COMMENT '申请动作: void/void_red',
    `request_reason` VARCHAR(200) DEFAULT NULL COMMENT '申请原因',
    `before_biz_status` TINYINT DEFAULT NULL COMMENT '审批前业务状态快照',
    `before_biz_snapshot` LONGTEXT COMMENT '审批前业务详情快照(JSON)',
    `after_biz_status` TINYINT DEFAULT NULL COMMENT '审批后业务状态快照',
    `after_biz_snapshot` LONGTEXT COMMENT '审批后业务详情快照(JSON)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-待审批, 2-已通过, 3-已驳回, 4-处理中',
    `requester_id` BIGINT NOT NULL COMMENT '申请人ID',
    `requester_name` VARCHAR(50) NOT NULL COMMENT '申请人姓名',
    `requester_role` VARCHAR(20) NOT NULL COMMENT '申请人角色',
    `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID',
    `approver_name` VARCHAR(50) DEFAULT NULL COMMENT '审批人姓名',
    `approve_remark` VARCHAR(200) DEFAULT NULL COMMENT '审批备注',
    `approved_at` DATETIME DEFAULT NULL COMMENT '审批通过时间',
    `rejected_at` DATETIME DEFAULT NULL COMMENT '审批驳回时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-删除',
    `pending_unique_key` VARCHAR(100)
        GENERATED ALWAYS AS (
            CASE
                WHEN `status` = 1 AND `is_deleted` = 0 THEN CONCAT(`biz_type`, '#', `biz_id`)
                ELSE NULL
            END
        ) STORED COMMENT '待审批唯一键(仅status=1且未删除生效)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_approval_no` (`approval_no`),
    UNIQUE KEY `uk_pending_unique_key` (`pending_unique_key`),
    KEY `idx_biz` (`biz_type`, `biz_id`),
    KEY `idx_status_create_time` (`status`, `create_time`),
    KEY `idx_requester` (`requester_id`, `requester_role`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作废审批单表';

-- =============================================
-- 四、初始化测试数据
-- =============================================

-- 4.1 初始化部门数据
INSERT INTO `sys_dept` (`dept_name`, `dept_code`, `leader`, `phone`, `description`) VALUES
('财务部', 'finance', '孙经理', '021-12345682', '负责财务报表与经营分析'),
('销售部', 'sales', '李经理', '021-12345680', '负责销售业务管理'),
('仓储部', 'warehouse', '赵经理', '021-12345681', '负责仓储、库存与作废审批管理'),
('采购部', 'purchase', '王经理', '021-12345679', '负责采购与退货业务管理'),
('人事部', 'hr', '张总', '021-12345678', '负责组织与人事管理'),
('系统管理部', 'system_management', '平台管理员', '021-12345677', '用于展示系统管理员与超级管理员信息');

-- 4.1.1 初始化IP策略示例数据
INSERT INTO `sys_ip_policy` (`policy_name`, `ip_cidr`, `allow_flag`, `status`, `priority`, `remark`) VALUES
('本机回环地址', '127.0.0.1/32', 1, 1, 1, '开发环境白名单');

-- 4.2 初始化账号（5 个部门管理员 + 5 个部门员工 + 1 个超级管理员）
-- 全部默认密码均为 123456
-- 注意：这里不再依赖固定部门 ID，而是按 dept_code 反查，避免插入顺序变化后账号串部门。
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `role`, `dept_id`, `status`, `phone`, `email`) VALUES
(1, 'hr_admin', '$2a$10$yxRor5xgip624/ulGHfyxerZlyhK39FpoVlaTIeBmi1DTAGFD6tl6', '人事管理员', 'admin', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'hr'), 1, '13800138000', 'hr_admin@warehouse.com'),
(2, 'purchase_admin', '$2a$10$yxRor5xgip624/ulGHfyxerZlyhK39FpoVlaTIeBmi1DTAGFD6tl6', '采购管理员', 'admin', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'purchase'), 1, '13800138001', 'purchase_admin@warehouse.com'),
(3, 'sales_admin', '$2a$10$yxRor5xgip624/ulGHfyxerZlyhK39FpoVlaTIeBmi1DTAGFD6tl6', '销售管理员', 'admin', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'sales'), 1, '13800138002', 'sales_admin@warehouse.com'),
(4, 'sales_employee', '$2a$10$yxRor5xgip624/ulGHfyxerZlyhK39FpoVlaTIeBmi1DTAGFD6tl6', '李四', 'employee', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'sales'), 1, '13800138003', 'sales_employee@warehouse.com'),
(5, 'warehouse_admin', '$2a$10$yxRor5xgip624/ulGHfyxerZlyhK39FpoVlaTIeBmi1DTAGFD6tl6', '仓储管理员', 'admin', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'warehouse'), 1, '13800138004', 'warehouse_admin@warehouse.com'),
(6, 'finance_admin', '$2a$10$yxRor5xgip624/ulGHfyxerZlyhK39FpoVlaTIeBmi1DTAGFD6tl6', '财务管理员', 'admin', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'finance'), 1, '13800138005', 'finance_admin@warehouse.com'),
(7, 'hr_employee', '$2a$10$yxRor5xgip624/ulGHfyxerZlyhK39FpoVlaTIeBmi1DTAGFD6tl6', '人事员工', 'employee', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'hr'), 1, '13800138006', 'hr_employee@warehouse.com'),
(8, 'purchase_employee', '$2a$10$yxRor5xgip624/ulGHfyxerZlyhK39FpoVlaTIeBmi1DTAGFD6tl6', '采购员工', 'employee', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'purchase'), 1, '13800138007', 'purchase_employee@warehouse.com'),
(9, 'warehouse_employee', '$2a$10$yxRor5xgip624/ulGHfyxerZlyhK39FpoVlaTIeBmi1DTAGFD6tl6', '仓储员工', 'employee', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'warehouse'), 1, '13800138008', 'warehouse_employee@warehouse.com'),
(10, 'finance_employee', '$2a$10$yxRor5xgip624/ulGHfyxerZlyhK39FpoVlaTIeBmi1DTAGFD6tl6', '财务员工', 'employee', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'finance'), 1, '13800138009', 'finance_employee@warehouse.com'),
(11, 'superadmin', '$2a$10$yxRor5xgip624/ulGHfyxerZlyhK39FpoVlaTIeBmi1DTAGFD6tl6', '超级管理员', 'superadmin', NULL, 1, '13800138010', 'superadmin@warehouse.com');

-- 4.3 初始化员工数据
INSERT INTO `sys_employee` (`user_id`, `emp_code`, `emp_name`, `dept_id`, `position`, `phone`, `email`) VALUES
((SELECT id FROM `sys_user` WHERE `username` = 'finance_employee'), 'EMP001', '财务员工', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'finance'), '财务专员', '13800138101', 'finance_employee@warehouse.com'),
((SELECT id FROM `sys_user` WHERE `username` = 'sales_employee'), 'EMP002', '李四', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'sales'), '销售代表', '13800138102', 'sales_employee@warehouse.com'),
((SELECT id FROM `sys_user` WHERE `username` = 'warehouse_employee'), 'EMP003', '仓储员工', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'warehouse'), '仓管员', '13800138103', 'warehouse_employee@warehouse.com'),
((SELECT id FROM `sys_user` WHERE `username` = 'purchase_employee'), 'EMP004', '采购员工', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'purchase'), '采购专员', '13800138104', 'purchase_employee@warehouse.com'),
((SELECT id FROM `sys_user` WHERE `username` = 'hr_employee'), 'EMP005', '人事员工', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'hr'), '人事专员', '13800138105', 'hr_employee@warehouse.com');

-- 4.4 初始化供应商数据
INSERT INTO `base_supplier` (`supplier_code`, `supplier_name`, `contact_person`, `contact_phone`, `address`, `description`) VALUES
('SUP001', '华强电子有限公司', '刘总', '0755-88888888', '深圳市华强北路电子大厦', '主营电子元件'),
('SUP002', '盛达贸易集团', '陈总', '021-77777777', '上海市浦东新区张江高科', '综合贸易公司'),
('SUP003', '科技数码港', '黄总', '010-66666666', '北京市海淀区中关村', '数码产品供应商'),
('SUP004', '宏达电子元件厂', '吴总', '0755-55555555', '深圳市宝安区西乡街道', '电子元件制造'),
('SUP005', '智能科技股份', '周总', '020-44444444', '广州市天河区科韵路', '智能硬件供应商');

-- 4.5 初始化商品数据
INSERT INTO `base_goods` (`goods_code`, `goods_name`, `category`, `brand`, `supplier_id`, `purchase_price`, `sale_price`, `stock`, `unit`, `description`)
SELECT
    seed.`goods_code`,
    seed.`goods_name`,
    seed.`category`,
    seed.`brand`,
    supplier.`id`,
    seed.`purchase_price`,
    seed.`sale_price`,
    seed.`stock`,
    seed.`unit`,
    seed.`description`
FROM (
    SELECT 'GD001' AS `goods_code`, '电阻10K' AS `goods_name`, '电子配件' AS `category`, '村田' AS `brand`, 'SUP004' AS `supplier_code`, 0.5 AS `purchase_price`, 1.5 AS `sale_price`, 500 AS `stock`, '个' AS `unit`, '10K欧姆电阻' AS `description`
    UNION ALL SELECT 'GD002', '电容100uF', '电子配件', '村田', 'SUP004', 1.0, 2.5, 400, '个', '100微法电容'
    UNION ALL SELECT 'GD003', '华为Mate60', '数码产品', '华为', 'SUP003', 4500.0, 5999.0, 200, '台', '华为最新旗舰手机'
    UNION ALL SELECT 'GD004', '小米14 Pro', '数码产品', '小米', 'SUP003', 3800.0, 4999.0, 150, '台', '小米高端手机'
    UNION ALL SELECT 'GD005', '联想ThinkPad', '数码产品', '联想', 'SUP002', 6000.0, 7500.0, 80, '台', '联想商务笔记本'
    UNION ALL SELECT 'GD006', '戴尔显示器', '数码产品', '戴尔', 'SUP005', 1200.0, 1699.0, 120, '台', '戴尔27寸显示器'
    UNION ALL SELECT 'GD007', '三星24英寸显示器', '数码产品', '三星', 'SUP005', 900.0, 1299.0, 100, '台', '三星入门显示器'
    UNION ALL SELECT 'GD008', '华为FreeBuds', '数码产品', '华为', 'SUP001', 300.0, 499.0, 300, '副', '华为无线耳机'
    UNION ALL SELECT 'GD009', '小米AirDots', '数码产品', '小米', 'SUP001', 80.0, 129.0, 500, '副', '小米蓝牙耳机'
    UNION ALL SELECT 'GD010', '惠普打印机', '办公用品', '惠普', 'SUP002', 800.0, 1200.0, 50, '台', '惠普激光打印机'
    UNION ALL SELECT 'GD011', '爱普生投影仪', '办公用品', '爱普生', 'SUP003', 3500.0, 4500.0, 30, '台', '爱普生商用投影仪'
    UNION ALL SELECT 'GD012', '佳能扫描仪', '办公用品', '佳能', 'SUP002', 1500.0, 2000.0, 40, '台', '佳能高速扫描仪'
) AS seed
JOIN `base_supplier` AS supplier ON supplier.`supplier_code` = seed.`supplier_code`;

-- 4.6 初始化进货记录
INSERT INTO `biz_purchase` (`purchase_no`, `goods_id`, `goods_name`, `quantity`, `unit_price`, `total_price`, `operator_id`, `operator_name`, `operation_time`, `remark`)
SELECT
    seed.`purchase_no`,
    goods.`id`,
    goods.`goods_name`,
    seed.`quantity`,
    seed.`unit_price`,
    seed.`total_price`,
    operator.`id`,
    seed.`operator_name`,
    seed.`operation_time`,
    seed.`remark`
FROM (
    SELECT 'PUR202501001' AS `purchase_no`, 'GD001' AS `goods_code`, 200 AS `quantity`, 0.5 AS `unit_price`, 100.00 AS `total_price`, 'superadmin' AS `username`, '系统管理员' AS `operator_name`, '2025-01-05 10:00:00' AS `operation_time`, '首批进货' AS `remark`
    UNION ALL SELECT 'PUR202501002', 'GD002', 150, 1.0, 150.00, 'superadmin', '系统管理员', '2025-01-05 10:30:00', '首批进货'
    UNION ALL SELECT 'PUR202501003', 'GD003', 50, 4500.0, 225000.00, 'superadmin', '系统管理员', '2025-01-08 14:00:00', '新品上市'
    UNION ALL SELECT 'PUR202501004', 'GD004', 40, 3800.0, 152000.00, 'superadmin', '系统管理员', '2025-01-08 14:30:00', '新品上市'
    UNION ALL SELECT 'PUR202501005', 'GD005', 20, 6000.0, 120000.00, 'superadmin', '系统管理员', '2025-01-10 09:00:00', '企业采购'
    UNION ALL SELECT 'PUR202501006', 'GD006', 30, 1200.0, 36000.00, 'superadmin', '系统管理员', '2025-01-10 09:30:00', '办公采购'
    UNION ALL SELECT 'PUR202502001', 'GD008', 100, 300.0, 30000.00, 'superadmin', '系统管理员', '2025-02-01 10:00:00', '节前备货'
    UNION ALL SELECT 'PUR202502002', 'GD009', 150, 80.0, 12000.00, 'superadmin', '系统管理员', '2025-02-01 10:30:00', '节前备货'
    UNION ALL SELECT 'PUR202502003', 'GD003', 30, 4500.0, 135000.00, 'superadmin', '系统管理员', '2025-02-05 14:00:00', '补货'
    UNION ALL SELECT 'PUR202502004', 'GD004', 25, 3800.0, 95000.00, 'superadmin', '系统管理员', '2025-02-05 14:30:00', '补货'
    UNION ALL SELECT 'PUR202503001', 'GD010', 20, 800.0, 16000.00, 'superadmin', '系统管理员', '2025-03-01 09:00:00', '新品采购'
    UNION ALL SELECT 'PUR202503002', 'GD011', 10, 3500.0, 35000.00, 'superadmin', '系统管理员', '2025-03-01 09:30:00', '新品采购'
    UNION ALL SELECT 'PUR202503003', 'GD012', 15, 1500.0, 22500.00, 'superadmin', '系统管理员', '2025-03-05 10:00:00', '新品采购'
    UNION ALL SELECT 'PUR202503004', 'GD001', 100, 0.5, 50.00, 'superadmin', '系统管理员', '2025-03-08 11:00:00', '补货'
    UNION ALL SELECT 'PUR202503005', 'GD002', 100, 1.0, 100.00, 'superadmin', '系统管理员', '2025-03-08 11:30:00', '补货'
) AS seed
JOIN `base_goods` AS goods ON goods.`goods_code` = seed.`goods_code`
JOIN `sys_user` AS operator ON operator.`username` = seed.`username`;

-- 4.7 初始化退货记录 (商品退给供应商)
INSERT INTO `biz_purchase_return` (`return_no`, `source_purchase_id`, `source_purchase_no`, `goods_id`, `goods_name`, `quantity`, `unit_price`, `total_price`, `operator_id`, `operator_name`, `operation_time`, `remark`)
SELECT
    seed.`return_no`,
    source_purchase.`id`,
    source_purchase.`purchase_no`,
    goods.`id`,
    goods.`goods_name`,
    seed.`quantity`,
    seed.`unit_price`,
    seed.`total_price`,
    operator.`id`,
    seed.`operator_name`,
    seed.`operation_time`,
    seed.`remark`
FROM (
    SELECT 'RET202501001' AS `return_no`, 'PUR202501001' AS `source_purchase_no`, 'GD001' AS `goods_code`, 20 AS `quantity`, 0.5 AS `unit_price`, 10.00 AS `total_price`, 'superadmin' AS `username`, '系统管理员' AS `operator_name`, '2025-01-20 10:00:00' AS `operation_time`, '质量问题退货' AS `remark`
    UNION ALL SELECT 'RET202502001', 'PUR202501002', 'GD002', 15, 1.0, 15.00, 'superadmin', '系统管理员', '2025-02-15 10:00:00', '质量问题退货'
) AS seed
JOIN `biz_purchase` AS source_purchase ON source_purchase.`purchase_no` = seed.`source_purchase_no`
JOIN `base_goods` AS goods ON goods.`goods_code` = seed.`goods_code`
JOIN `sys_user` AS operator ON operator.`username` = seed.`username`;

-- 4.8 初始化销售记录
INSERT INTO `biz_sales` (`sales_no`, `goods_id`, `goods_name`, `quantity`, `unit_price`, `total_price`, `operator_id`, `operator_name`, `operation_time`, `remark`)
SELECT
    seed.`sales_no`,
    goods.`id`,
    goods.`goods_name`,
    seed.`quantity`,
    seed.`unit_price`,
    seed.`total_price`,
    operator.`id`,
    seed.`operator_name`,
    seed.`operation_time`,
    seed.`remark`
FROM (
    SELECT 'SAL202501001' AS `sales_no`, 'GD003' AS `goods_code`, 10 AS `quantity`, 5999.0 AS `unit_price`, 59990.00 AS `total_price`, 'sales_employee' AS `username`, '李四' AS `operator_name`, '2025-01-15 10:00:00' AS `operation_time`, '正常销售' AS `remark`
    UNION ALL SELECT 'SAL202501002', 'GD004', 8, 4999.0, 39992.00, 'sales_employee', '李四', '2025-01-15 11:00:00', '正常销售'
    UNION ALL SELECT 'SAL202501003', 'GD005', 5, 7500.0, 37500.00, 'sales_employee', '李四', '2025-01-16 14:00:00', '企业采购'
    UNION ALL SELECT 'SAL202501004', 'GD006', 10, 1699.0, 16990.00, 'sales_employee', '李四', '2025-01-17 09:00:00', '批量销售'
    UNION ALL SELECT 'SAL202501005', 'GD008', 30, 499.0, 14970.00, 'sales_employee', '李四', '2025-01-18 10:00:00', '促销活动'
    UNION ALL SELECT 'SAL202501006', 'GD009', 50, 129.0, 6450.00, 'sales_employee', '李四', '2025-01-18 11:00:00', '促销活动'
    UNION ALL SELECT 'SAL202502001', 'GD003', 15, 5999.0, 89985.00, 'sales_employee', '李四', '2025-02-10 10:00:00', '节后销售'
    UNION ALL SELECT 'SAL202502002', 'GD004', 12, 4999.0, 59988.00, 'sales_employee', '李四', '2025-02-10 11:00:00', '节后销售'
    UNION ALL SELECT 'SAL202502003', 'GD006', 8, 1699.0, 13592.00, 'sales_employee', '李四', '2025-02-12 09:00:00', '正常销售'
    UNION ALL SELECT 'SAL202502004', 'GD008', 40, 499.0, 19960.00, 'sales_employee', '李四', '2025-02-14 10:00:00', '情人节促销'
    UNION ALL SELECT 'SAL202503001', 'GD003', 20, 5999.0, 119980.00, 'sales_employee', '李四', '2025-03-05 10:00:00', '正常销售'
    UNION ALL SELECT 'SAL202503002', 'GD004', 18, 4999.0, 89982.00, 'sales_employee', '李四', '2025-03-05 11:00:00', '正常销售'
    UNION ALL SELECT 'SAL202503003', 'GD005', 8, 7500.0, 60000.00, 'sales_employee', '李四', '2025-03-06 09:00:00', '企业采购'
    UNION ALL SELECT 'SAL202503004', 'GD006', 12, 1699.0, 20388.00, 'sales_employee', '李四', '2025-03-07 10:00:00', '正常销售'
    UNION ALL SELECT 'SAL202503005', 'GD010', 5, 1200.0, 6000.00, 'sales_employee', '李四', '2025-03-08 09:00:00', '新品销售'
    UNION ALL SELECT 'SAL202503006', 'GD011', 3, 4500.0, 13500.00, 'sales_employee', '李四', '2025-03-08 10:00:00', '新品销售'
    UNION ALL SELECT 'SAL202503007', 'GD012', 4, 2000.0, 8000.00, 'sales_employee', '李四', '2025-03-09 09:00:00', '新品销售'
    UNION ALL SELECT 'SAL202503008', 'GD007', 15, 1299.0, 19485.00, 'sales_employee', '李四', '2025-03-10 10:00:00', '正常销售'
) AS seed
JOIN `base_goods` AS goods ON goods.`goods_code` = seed.`goods_code`
JOIN `sys_user` AS operator ON operator.`username` = seed.`username`;

-- 4.9 初始化客退记录 (客户退货)
INSERT INTO `biz_sales_return` (`return_no`, `source_sales_id`, `source_sales_no`, `goods_id`, `goods_name`, `quantity`, `unit_price`, `total_price`, `operator_id`, `operator_name`, `operation_time`, `remark`)
SELECT
    seed.`return_no`,
    source_sales.`id`,
    source_sales.`sales_no`,
    goods.`id`,
    goods.`goods_name`,
    seed.`quantity`,
    seed.`unit_price`,
    seed.`total_price`,
    operator.`id`,
    seed.`operator_name`,
    seed.`operation_time`,
    seed.`remark`
FROM (
    SELECT 'CSTRET202501001' AS `return_no`, 'SAL202501001' AS `source_sales_no`, 'GD003' AS `goods_code`, 1 AS `quantity`, 5999.0 AS `unit_price`, 5999.00 AS `total_price`, 'sales_employee' AS `username`, '李四' AS `operator_name`, '2025-01-20 10:00:00' AS `operation_time`, '质量问题退货' AS `remark`
    UNION ALL SELECT 'CSTRET202501002', 'SAL202501002', 'GD004', 1, 4999.0, 4999.00, 'sales_employee', '李四', '2025-01-20 11:00:00', '质量问题退货'
    UNION ALL SELECT 'CSTRET202502001', 'SAL202501005', 'GD008', 2, 499.0, 998.00, 'sales_employee', '李四', '2025-02-20 10:00:00', '客户退货'
) AS seed
JOIN `biz_sales` AS source_sales ON source_sales.`sales_no` = seed.`source_sales_no`
JOIN `base_goods` AS goods ON goods.`goods_code` = seed.`goods_code`
JOIN `sys_user` AS operator ON operator.`username` = seed.`username`;

-- 4.9.1 初始化成本快照字段（V2.2）
-- 销售单：按销售时间回溯最近有效进货价，兜底商品进价
UPDATE `biz_sales` s
LEFT JOIN `base_goods` bg ON bg.id = s.goods_id
SET
        s.cost_unit_price = COALESCE(
                (
                        SELECT p.unit_price
                        FROM `biz_purchase` p
                        WHERE p.goods_id = s.goods_id
                            AND p.is_deleted = 0
                            AND p.biz_status = 1
                            AND p.operation_time <= s.operation_time
                        ORDER BY p.operation_time DESC, p.id DESC
                        LIMIT 1
                ),
                bg.purchase_price,
                0
        ),
        s.cost_total_price = ROUND(
                s.quantity * COALESCE(
                        (
                                SELECT p.unit_price
                                FROM `biz_purchase` p
                                WHERE p.goods_id = s.goods_id
                                    AND p.is_deleted = 0
                                    AND p.biz_status = 1
                                    AND p.operation_time <= s.operation_time
                                ORDER BY p.operation_time DESC, p.id DESC
                                LIMIT 1
                        ),
                        bg.purchase_price,
                        0
                ),
                2
        ),
        s.cost_source = CASE
                WHEN (
                        SELECT p.unit_price
                        FROM `biz_purchase` p
                        WHERE p.goods_id = s.goods_id
                            AND p.is_deleted = 0
                            AND p.biz_status = 1
                            AND p.operation_time <= s.operation_time
                        ORDER BY p.operation_time DESC, p.id DESC
                        LIMIT 1
                ) IS NOT NULL THEN 'RECENT_PURCHASE'
                WHEN bg.purchase_price IS NOT NULL THEN 'GOODS_PRICE'
                ELSE 'ZERO_FALLBACK'
        END
WHERE s.is_deleted = 0;

-- 客退单：优先继承来源销售成本快照，兜底最近进货价/商品进价
UPDATE `biz_sales_return` r
LEFT JOIN `biz_sales` s ON s.id = r.source_sales_id
LEFT JOIN `base_goods` bg ON bg.id = r.goods_id
SET
        r.cost_unit_price = COALESCE(
                s.cost_unit_price,
                (
                        SELECT p.unit_price
                        FROM `biz_purchase` p
                        WHERE p.goods_id = r.goods_id
                            AND p.is_deleted = 0
                            AND p.biz_status = 1
                            AND p.operation_time <= COALESCE(s.operation_time, r.operation_time)
                        ORDER BY p.operation_time DESC, p.id DESC
                        LIMIT 1
                ),
                bg.purchase_price,
                0
        ),
        r.cost_total_price = ROUND(
                r.quantity * COALESCE(
                        s.cost_unit_price,
                        (
                                SELECT p.unit_price
                                FROM `biz_purchase` p
                                WHERE p.goods_id = r.goods_id
                                    AND p.is_deleted = 0
                                    AND p.biz_status = 1
                                    AND p.operation_time <= COALESCE(s.operation_time, r.operation_time)
                                ORDER BY p.operation_time DESC, p.id DESC
                                LIMIT 1
                        ),
                        bg.purchase_price,
                        0
                ),
                2
        ),
        r.cost_source = CASE
                WHEN s.cost_unit_price IS NOT NULL THEN 'SOURCE_SALE'
                WHEN (
                        SELECT p.unit_price
                        FROM `biz_purchase` p
                        WHERE p.goods_id = r.goods_id
                            AND p.is_deleted = 0
                            AND p.biz_status = 1
                            AND p.operation_time <= COALESCE(s.operation_time, r.operation_time)
                        ORDER BY p.operation_time DESC, p.id DESC
                        LIMIT 1
                ) IS NOT NULL THEN 'RECENT_PURCHASE'
                WHEN bg.purchase_price IS NOT NULL THEN 'GOODS_PRICE'
                ELSE 'ZERO_FALLBACK'
        END
WHERE r.is_deleted = 0;

-- 成本快照覆盖率校验（应返回 0）
SELECT 'biz_sales' AS table_name,
             COUNT(*) AS missing_count
FROM `biz_sales`
WHERE is_deleted = 0
    AND (cost_unit_price IS NULL OR cost_total_price IS NULL OR cost_source IS NULL)
UNION ALL
SELECT 'biz_sales_return' AS table_name,
             COUNT(*) AS missing_count
FROM `biz_sales_return`
WHERE is_deleted = 0
    AND (cost_unit_price IS NULL OR cost_total_price IS NULL OR cost_source IS NULL);

-- 4.9.2 可选严格模式（S4）
-- 仅当上方 missing_count 均为 0 时再启用。
-- 如需启用，请取消注释后执行。
-- ALTER TABLE `biz_sales`
-- MODIFY COLUMN `cost_unit_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
-- MODIFY COLUMN `cost_total_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
-- MODIFY COLUMN `cost_source` VARCHAR(30) NOT NULL DEFAULT 'ZERO_FALLBACK';
--
-- ALTER TABLE `biz_sales_return`
-- MODIFY COLUMN `cost_unit_price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
-- MODIFY COLUMN `cost_total_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
-- MODIFY COLUMN `cost_source` VARCHAR(30) NOT NULL DEFAULT 'ZERO_FALLBACK';

-- 4.10 初始化公告数据
INSERT INTO `sys_notice` (`title`, `content`, `target_role`, `target_dept_id`, `publisher`, `publish_time`, `status`) VALUES
('管理员月度例会通知', '请各部门管理员于本周五下午参加月度管理例会，汇报本部门重点事项。', 'admin', NULL, '超级管理员', '2025-03-10 10:00:00', 1),
('全员制度更新通知', '仓库管理制度已完成统一修订，请全体账号登录后及时查阅并执行。', 'all', NULL, '超级管理员', '2025-03-08 14:00:00', 1),
('仓储部盘点安排', '仓储部员工请于本周三下班前完成月度盘点，并提交差异说明。', 'employee', (SELECT id FROM `sys_dept` WHERE `dept_code` = 'warehouse'), '仓储管理员', '2025-03-09 09:30:00', 1);

-- =============================================
-- 五、视图 (便于前端查询)
-- =============================================

-- 5.1 商品详细信息视图 (包含供应商信息)
CREATE OR REPLACE VIEW `v_goods_detail` AS
SELECT
    g.id,
    g.goods_code,
    g.goods_name,
    g.category,
    g.brand,
    g.supplier_id,
    s.supplier_name,
    s.supplier_code,
    g.purchase_price,
    g.sale_price,
    g.stock,
    g.unit,
    g.status,
    g.description,
    g.create_time,
    g.update_time
FROM `base_goods` g
LEFT JOIN `base_supplier` s ON g.supplier_id = s.id
WHERE g.is_deleted = 0;

-- 5.2 进货详情视图 (包含商品和操作人信息)
CREATE OR REPLACE VIEW `v_purchase_detail` AS
SELECT
    p.id,
    p.purchase_no,
    p.goods_id,
    p.goods_name,
    g.goods_code,
    g.category,
    g.brand,
    p.quantity,
    p.unit_price,
    p.total_price,
    p.operator_id,
    p.operator_name,
    u.real_name AS operator_real_name,
    p.operation_time,
    p.remark,
    p.create_time,
    p.update_time
FROM `biz_purchase` p
LEFT JOIN `base_goods` g ON p.goods_id = g.id
LEFT JOIN `sys_user` u ON p.operator_id = u.id
WHERE p.is_deleted = 0;

-- 5.3 销售详情视图 (包含商品和操作人信息)
CREATE OR REPLACE VIEW `v_sales_detail` AS
SELECT
    s.id,
    s.sales_no,
    s.goods_id,
    s.goods_name,
    g.goods_code,
    g.category,
    g.brand,
    s.quantity,
    s.unit_price,
    s.total_price,
    s.operator_id,
    s.operator_name,
    u.real_name AS operator_real_name,
    s.operation_time,
    s.remark,
    s.create_time,
    s.update_time
FROM `biz_sales` s
LEFT JOIN `base_goods` g ON s.goods_id = g.id
LEFT JOIN `sys_user` u ON s.operator_id = u.id
WHERE s.is_deleted = 0;

-- =============================================
-- 六、存储过程示例
-- =============================================

-- 6.1 进货库存增加存储过程
DELIMITER //
DROP PROCEDURE IF EXISTS `sp_purchase_add_stock` //
CREATE PROCEDURE `sp_purchase_add_stock`(
    IN p_goods_id BIGINT,
    IN p_quantity INT,
    OUT p_result INT
)
BEGIN
    DECLARE v_stock INT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result = 0;
    END;

    START TRANSACTION;

    -- 更新商品库存
    UPDATE `base_goods` SET stock = stock + p_quantity WHERE id = p_goods_id;

    -- 获取更新后的库存
    SELECT stock INTO v_stock FROM `base_goods` WHERE id = p_goods_id;

    COMMIT;
    SET p_result = v_stock;
END //
DELIMITER ;

-- =============================================
-- 脚本执行完成
-- =============================================
