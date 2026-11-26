package com.smart.iot.admin.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.iot.admin.entity.AdminOpenPlatform;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper routed to [master] database for platform management.
 * 路由至 [master] 主库的开放平台管理 Mapper。
 */
@Mapper
@DS("master")
public interface AdminOpenPlatformMapper extends BaseMapper<AdminOpenPlatform> {
}