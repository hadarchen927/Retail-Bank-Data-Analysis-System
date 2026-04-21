SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 系统用户表 (sys_user)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
                            `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户唯一标识',
                            `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录名',
                            `password` VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
                            `role` VARCHAR(20) DEFAULT 'ANALYST' COMMENT '角色: ADMIN, ANALYST, RISK_OFFICER, MANAGER',
                            `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
                            `login_count` INT DEFAULT 0 COMMENT '登录次数',
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认管理员 (密码: 123456 的加密串)
INSERT INTO `sys_user` (`username`, `password`, `role`, `nickname`, `login_count`)
VALUES ('admin', '$2a$10$7JB720yub.S.f.hXN.qN8eXyv1pXz.nUuLpI.pG9ZpI.G', 'ADMIN', '超级管理员', 0);


-- ----------------------------
-- 2. 客户信息表 (bank_customer)
-- ----------------------------
DROP TABLE IF EXISTS `bank_customer`;
CREATE TABLE `bank_customer` (
                                 `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '客户唯一标识',
                                 `cust_name` VARCHAR(50) NOT NULL COMMENT '客户姓名',
                                 `age` INT DEFAULT NULL COMMENT '年龄',
                                 `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别(男/女)',
                                 `annual_income` DECIMAL(15,2) DEFAULT NULL COMMENT '年收入(元)',
                                 `total_assets` DECIMAL(15,2) DEFAULT NULL COMMENT '总资产(元)',
                                 `loan_balance` DECIMAL(15,2) DEFAULT NULL COMMENT '贷款余额(元)',
                                 `credit_score` INT DEFAULT NULL COMMENT '信用评分(0-1000)',
                                 `overdue_days` INT DEFAULT 0 COMMENT '逾期天数',
                                 `cluster_tag` INT DEFAULT -1 COMMENT '分群标签, -1表示未分群',
                                 INDEX `idx_cust_name` (`cust_name`),
                                 INDEX `idx_cluster_tag` (`cluster_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------
-- 3. 交易明细表 (bank_transaction)
-- (文档截图中未完全展示，暂保留并增加基础外键约束)
-- ----------------------------
DROP TABLE IF EXISTS `bank_transaction`;
CREATE TABLE `bank_transaction` (
                                    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                                    `cust_id` BIGINT NOT NULL COMMENT '关联客户ID',
                                    `amount` DECIMAL(15,2) NOT NULL COMMENT '交易金额',
                                    `type` VARCHAR(20) COMMENT '存入, 支出, 转账',
                                    `trans_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',
                                    `location` VARCHAR(100) COMMENT '交易地点',
                                    CONSTRAINT `fk_transaction_customer` FOREIGN KEY (`cust_id`) REFERENCES `bank_customer` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------
-- 4. 风险预警表 (bank_risk_record)
-- ----------------------------
DROP TABLE IF EXISTS `bank_risk_record`;
CREATE TABLE `bank_risk_record` (
                                    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预警唯一标识',
                                    `cust_id` BIGINT NOT NULL COMMENT '关联 bank_customer 表的 id',
                                    `cust_name` VARCHAR(50) NOT NULL COMMENT '客户姓名(冗余存储)',
                                    `risk_type` VARCHAR(50) DEFAULT NULL COMMENT '风险类型',
                                    `risk_level` VARCHAR(10) DEFAULT NULL COMMENT '风险等级(高中低)',
                                    `description` VARCHAR(200) DEFAULT NULL COMMENT '风��描述',
                                    `status` VARCHAR(20) DEFAULT '待处理' COMMENT '处理状态',
                                    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    INDEX `idx_status` (`status`),
                                    CONSTRAINT `fk_risk_customer` FOREIGN KEY (`cust_id`) REFERENCES `bank_customer` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ----------------------------
-- 5. 操作日志表 (sys_log)
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
                           `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志唯一标识',
                           `username` VARCHAR(50) DEFAULT '匿名用户' COMMENT '操作用户',
                           `operation` VARCHAR(100) DEFAULT NULL COMMENT '操作描述',
                           `method` VARCHAR(200) DEFAULT NULL COMMENT '调用方法(类名.方法名)',
                           `time` BIGINT DEFAULT NULL COMMENT '执行耗时(毫秒)',
                           `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                           INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;