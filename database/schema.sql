-- 1. 系统用户表（用于登录和权限控制）
CREATE TABLE `sys_user` (
                            `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                            `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录名',
                            `password` VARCHAR(255) NOT NULL COMMENT '加密后的密码',
                            `role` VARCHAR(20) NOT NULL COMMENT '角色: ANALYST, MANAGER, RISK_OFFICER',
                            `nickname` VARCHAR(50) COMMENT '昵称',
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 客户画像表（用于 K-Means 聚类分析的核心数据源）
CREATE TABLE `bank_customer` (
                                 `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 `cust_name` VARCHAR(50),
                                 `age` INT,
                                 `gender` VARCHAR(10),
                                 `annual_income` DECIMAL(15,2) COMMENT '年收入',
                                 `credit_score` INT COMMENT '信用评分',
                                 `total_assets` DECIMAL(15,2) COMMENT '总资产',
                                 `loan_balance` DECIMAL(15,2) COMMENT '贷款余额',
                                 `cluster_tag` INT DEFAULT -1 COMMENT '机器学习生成的分群标签: -1未分群, 0, 1, 2...'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 交易明细表（用于仪表盘 ECharts 图表展示）
CREATE TABLE `bank_transaction` (
                                    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
                                    `cust_id` BIGINT,
                                    `amount` DECIMAL(15,2),
                                    `type` VARCHAR(20) COMMENT '存入, 支出, 转账',
                                    `trans_time` DATETIME,
                                    `location` VARCHAR(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入一条默认管理员账号 (密码: 123456, 这里先存明文，之后集成Security再改)
INSERT INTO `sys_user` (username, password, role, nickname)
VALUES ('admin', '123456', 'ANALYST', '首席分析师');