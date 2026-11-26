package com.smart.iot.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity mapping to table t_device_track_log in [iot] database.
 * 映射到设备库 [iot] 中 t_device_track_log 表的轨迹流水实体类。
 */
@Data
@TableName("t_device_track_log")
public class DeviceTrackLog {

    /** Primary key ID. 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Riding session code. 骑行编码。 */
    private String rideCode;

    /** GPS module ID. GPS模块ID。 */
    private String gpsId;

    /** Device report timestamp in milliseconds. 设备上报时间戳（毫秒）。 */
    private Long reportTime;

    /** Vehicle serial number. 车辆序列号。 */
    private String deviceSn;

    /** Longitude coordinate. 经度坐标。 */
    private BigDecimal longitude;

    /** Latitude coordinate. 纬度坐标。 */
    private BigDecimal latitude;

    /** Current riding speed (km/h). 当前骑行速度。 */
    private Integer speed;

    /** Database insertion timestamp. 数据库入库时间。 */
    private LocalDateTime createTime;
}