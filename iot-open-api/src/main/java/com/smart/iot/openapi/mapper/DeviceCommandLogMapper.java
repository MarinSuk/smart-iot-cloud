package com.smart.iot.openapi.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.iot.openapi.entity.DeviceCommandLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for DeviceCommandLog, routed to [iot] datasource.
 * 远控指令流水 Mapper，路由至 [iot] 设备数据源。
 */
@Mapper
@DS("iot")
public interface DeviceCommandLogMapper extends BaseMapper<DeviceCommandLog> {
}