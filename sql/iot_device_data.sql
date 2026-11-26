CREATE DATABASE IF NOT EXISTS `iot_device_data`
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_general_ci;


-- Execute in database [iot_device_data] / 在设备库 [iot_device_data] 中执行
USE iot_device_data;
CREATE TABLE `t_device_info` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key / 主键',
                                 `device_sn` varchar(64) NOT NULL COMMENT 'Vehicle/Device Serial Number / 车辆硬件序列号',
                                 `battery_level` int NOT NULL DEFAULT '100' COMMENT 'Battery Percentage / 电池电量百分比',
                                 `longitude` decimal(10,6) DEFAULT NULL COMMENT 'Longitude / 经度',
                                 `latitude` decimal(10,6) DEFAULT NULL COMMENT 'Latitude / 纬度',
                                 `expire_time` datetime NOT NULL COMMENT 'IoT Service Expiration Time / 车联服务有效期',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_device_sn` (`device_sn`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Vehicle IoT Device Info / 车辆物联网设备信息表';

-- Insert test vehicle data / 插入一条测试车辆数据
INSERT INTO `t_device_info` (`device_sn`, `battery_level`, `longitude`, `latitude`, `expire_time`)
VALUES ('LV_EB_20260001', 85, 120.155070, 30.274084, '2027-12-31 23:59:59');

USE iot_device_data;
CREATE TABLE `t_device_track_log` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key / 主键',
                                      `ride_code` varchar(64) NOT NULL COMMENT 'Riding Session Code / 骑行编码',
                                      `gps_id` varchar(64) NOT NULL COMMENT 'GPS Module ID / GPS设备ID',
                                      `report_time` bigint NOT NULL COMMENT 'Device Report Timestamp (ms) / 设备上报时间戳（毫秒）',
                                      `device_sn` varchar(64) NOT NULL COMMENT 'Vehicle Serial Number / 车辆序列号',
                                      `longitude` decimal(10,6) NOT NULL COMMENT 'Longitude / 经度',
                                      `latitude` decimal(10,6) NOT NULL COMMENT 'Latitude / 纬度',
                                      `speed` int NOT NULL DEFAULT '0' COMMENT 'Speed (km/h) / 骑行速度',
                                      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record Insert Time / 入库时间',
                                      PRIMARY KEY (`id`),
    -- Composite unique key matching your real-world business rule / 贴合真实业务的联合唯一索引
                                      UNIQUE KEY `uk_ride_gps_time` (`ride_code`, `gps_id`, `report_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Vehicle Riding Track Log / 车辆骑行轨迹流水表';

USE iot_device_data;
CREATE TABLE `t_device_command_log` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary Key / 主键',
                                        `cmd_id` varchar(64) NOT NULL COMMENT 'Unique Command ID / 全局唯一指令ID',
                                        `device_sn` varchar(64) NOT NULL COMMENT 'Vehicle Serial Number / 车辆硬件序列号',
                                        `cmd_type` varchar(32) NOT NULL COMMENT 'Command Type (e.g., UNLOCK) / 指令类型（如开锁）',
                                        `status` tinyint NOT NULL DEFAULT '0' COMMENT '0:Pending, 1:Success, 2:Timeout Expired / 状态：0下发中 1执行成功 2超时失效',
                                        `expire_timestamp` bigint NOT NULL COMMENT 'Command Expiration Timestamp (ms) / 指令过期时间戳（毫秒）',
                                        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time / 创建时间',
                                        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time / 更新时间',
                                        PRIMARY KEY (`id`),
                                        UNIQUE KEY `uk_cmd_id` (`cmd_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='4G Remote Control Command Log / 4G远控指令流水表';

