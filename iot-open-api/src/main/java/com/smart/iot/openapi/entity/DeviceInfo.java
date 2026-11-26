package com.smart.iot.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity mapping to table t_device_info in [iot] database.
 * 映射到设备库 [iot] 中 t_device_info 表的实体类。
 */
@Data
@TableName("t_device_info")
public class DeviceInfo {

    /** Primary key ID. 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Device serial number. 车辆硬件序列号。 */
    private String deviceSn;

    /** Current battery percentage. 当前电池电量。 */
    private Integer batteryLevel;

    /** Current longitude. 当前经度。 */
    private BigDecimal longitude;

    /** Current latitude. 当前纬度。 */
    private BigDecimal latitude;

    /** Service expiration time. 车联服务有效期。 */
    private LocalDateTime expireTime;
}