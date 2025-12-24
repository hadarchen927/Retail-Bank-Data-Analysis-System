SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 系统用户表 (sys_user)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
                            `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                            `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录名',
                            `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt加密后的密码',
                            `role` VARCHAR(20) NOT NULL COMMENT '角色: ADMIN, ANALYST, RISK_OFFICER, MANAGER',
                            `nickname` VARCHAR(50) COMMENT '昵称',
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认管理员 (密码: 123456 的加密串)
INSERT INTO `sys_user` (`username`, `password`, `role`, `nickname`)
VALUES ('admin', '$2a$10$7JB720yub.S.f.hXN.qN8eXyv1pXz.nUuLpI.pG9ZpI.G', 'ADMIN', '超级管理员');


-- ----------------------------
-- 2. 客户画像表 (bank_customer)
-- ----------------------------
DROP TABLE IF EXISTS `bank_customer`;
CREATE TABLE `bank_customer` (
                                 `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 `cust_name` VARCHAR(50) COMMENT '客户姓名',
                                 `gender` VARCHAR(10) COMMENT '性别',
                                 `age` INT COMMENT '年龄',
                                 `annual_income` DECIMAL(15,2) COMMENT '年收入',
                                 `total_assets` DECIMAL(15,2) COMMENT '总资产',
                                 `loan_balance` DECIMAL(15,2) COMMENT '贷款余额',
                                 `cluster_tag` INT DEFAULT -1 COMMENT 'K-Means分群标签: -1未分群, 0高价值, 1潜力, 2一般'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------
-- 3. 交易明细表 (bank_transaction)
-- ----------------------------
DROP TABLE IF EXISTS `bank_transaction`;
CREATE TABLE `bank_transaction` (
                                    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                                    `cust_id` BIGINT NOT NULL COMMENT '关联客户ID',
                                    `amount` DECIMAL(15,2) NOT NULL COMMENT '交易金额',
                                    `type` VARCHAR(20) COMMENT '存入, 支出, 转账',
                                    `trans_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',
                                    `location` VARCHAR(100) COMMENT '交易地点'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------
-- 4. 风险预警记录表 (bank_risk_record) - 【新增】
-- ----------------------------
DROP TABLE IF EXISTS `bank_risk_record`;
CREATE TABLE `bank_risk_record` (
                                    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                                    `cust_id` BIGINT NOT NULL,
                                    `cust_name` VARCHAR(50),
                                    `risk_type` VARCHAR(50) COMMENT '风险类型: 高负债, 异常大额支出',
                                    `risk_level` VARCHAR(10) COMMENT '风险等级: 高, 中, 低',
                                    `description` TEXT COMMENT '风险详细描述',
                                    `status` VARCHAR(20) DEFAULT '待处理' COMMENT '待处理, 已处理, 已忽略',
                                    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------
-- 5. 系统审计日志表 (sys_log) - 【新增】
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
                           `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                           `username` VARCHAR(50) COMMENT '操作人',
                           `operation` VARCHAR(100) COMMENT '操作描述',
                           `method` VARCHAR(200) COMMENT '请求方法',
                           `time` BIGINT COMMENT '执行时长(ms)',
                           `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;