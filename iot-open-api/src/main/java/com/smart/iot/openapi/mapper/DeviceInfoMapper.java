package com.smart.iot.openapi.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.iot.openapi.entity.DeviceInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for DeviceInfo, explicitly routed to [iot] datasource.
 * 车辆设备信息 Mapper，显式路由至 [iot] 设备数据源。
 */
@Mapper
@DS("iot")
public interface DeviceInfoMapper extends BaseMapper<DeviceInfo> {
}