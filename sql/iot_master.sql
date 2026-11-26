CREATE DATABASE IF NOT EXISTS `iot_master`
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_general_ci;

-- Execute in database [iot_master] / 在主库 [iot_master] 中执行
USE iot_master;
CREATE TABLE `t_open_platform` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key / 主键',
                                   `app_id` varchar(64) NOT NULL COMMENT 'Third-party Platform ID / 第三方平台ID',
                                   `app_secret` varchar(128) NOT NULL COMMENT 'Platform Secret Key / 平台密钥',
                                   `platform_name` varchar(100) NOT NULL COMMENT 'Platform Name / 平台名称',
                                   `quota_remain` int NOT NULL DEFAULT '0' COMMENT 'Remaining API Quota / 剩余接口调用配额',
                                   `status` tinyint NOT NULL DEFAULT '1' COMMENT '1:Active, 0:Disabled / 状态：1启用 0禁用',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Third-party Open Platform Config / 第三方开放平台配置表';

-- Insert test partner data / 插入一条测试合作方数据
INSERT INTO `t_open_platform` (`app_id`, `app_secret`, `platform_name`, `quota_remain`, `status`)
VALUES ('partner_001', 'secret_key_888888', '高德出行合作方', 1000, 1);

USE iot_master;
CREATE TABLE `t_admin_user` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key / 主键',
                                `username` varchar(64) NOT NULL COMMENT 'Admin Login Username / 管理员登录账号',
                                `password` varchar(128) NOT NULL COMMENT 'Encrypted Password (BCrypt/SHA256) / 加密后的登录密码',
                                `nickname` varchar(64) NOT NULL COMMENT 'Display Nickname / 管理员显示昵称',
                                `status` tinyint NOT NULL DEFAULT '1' COMMENT '1:Active, 0:Disabled / 状态：1启用 0禁用',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time / 创建时间',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise Admin User Table / 企业后台管理员表';

-- Insert default admin account (password: 123456, stored as SHA-256 hex)
-- 插入默认管理员账号（明文密码为 123456，数据库存储 SHA-256 摘要值）
INSERT INTO `t_admin_user` (`username`, `password`, `nickname`, `status`)
VALUES ('admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'super admin', 1);



