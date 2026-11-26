package com.smart.iot.openapi.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Message payload DTO sent from decoding platform to RabbitMQ.
 * 由解码平台发送到 RabbitMQ 的设备轨迹消息传输对象。
 */
@Data
public class DeviceTrackMessage {

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

    /** Riding speed (km/h). 骑行速度。 */
    private Integer speed;
}