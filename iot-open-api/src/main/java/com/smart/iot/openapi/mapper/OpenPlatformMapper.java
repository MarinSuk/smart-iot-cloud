package com.smart.iot.openapi.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.iot.openapi.entity.OpenPlatform;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for OpenPlatform, explicitly routed to [master] datasource.
 * 开放平台配置 Mapper，显式路由至 [master] 主数据源。
 */
@Mapper
@DS("master")
public interface OpenPlatformMapper extends BaseMapper<OpenPlatform> {
}