package com.smart.iot.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity mapping to table t_device_command_log in [iot] database.
 * 映射到设备库 [iot] 中 t_device_command_log 表的远控指令流水实体类。
 */
@Data
@TableName("t_device_command_log")
public class DeviceCommandLog {

    /** Primary key ID. 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Global unique command ID. 全局唯一指令ID。 */
    private String cmdId;

    /** Vehicle serial number. 车辆硬件序列号。 */
    private String deviceSn;

    /** Command type (e.g., UNLOCK). 指令类型（如开锁）。 */
    private String cmdType;

    /** Execution status (0: Pending, 1: Success, 2: Timeout Expired). 执行状态（0下发中 1成功 2超时失效）。 */
    private Integer status;

    /** Command expiration timestamp in milliseconds. 指令过期时间戳（毫秒）。 */
    private Long expireTimestamp;

    /** Creation time. 创建时间。 */
    private LocalDateTime createTime;

    /** Last update time. 更新时间。 */
    private LocalDateTime updateTime;
}