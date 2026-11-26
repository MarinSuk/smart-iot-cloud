package com.smart.iot.admin.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.iot.admin.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for AdminUser, explicitly routed to [master] datasource.
 * 后台管理员 Mapper，显式路由至 [master] 主数据源。
 */
@Mapper
@DS("master")
public interface AdminUserMapper extends BaseMapper<AdminUser> {
}