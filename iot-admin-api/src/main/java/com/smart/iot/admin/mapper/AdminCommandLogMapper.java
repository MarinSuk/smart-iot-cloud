package com.smart.iot.admin.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.iot.admin.entity.AdminCommandLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper routed to [iot] database for command log monitoring.
 * 路由至 [iot] 设备库的远控指令监控 Mapper。
 */
@Mapper
@DS("iot")
public interface AdminCommandLogMapper extends BaseMapper<AdminCommandLog> {
}