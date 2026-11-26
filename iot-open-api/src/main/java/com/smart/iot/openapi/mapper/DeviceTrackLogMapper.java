package com.smart.iot.openapi.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.iot.openapi.entity.DeviceTrackLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for DeviceTrackLog, routed to [iot] datasource.
 * 车辆轨迹流水 Mapper，路由至 [iot] 设备数据源。
 */
@Mapper
@DS("iot")
public interface DeviceTrackLogMapper extends BaseMapper<DeviceTrackLog> {
}