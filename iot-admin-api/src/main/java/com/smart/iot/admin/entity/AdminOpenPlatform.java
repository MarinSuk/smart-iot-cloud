package com.smart.iot.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Admin view entity for third-party open platform config in [master] DB.
 * 后台管理端映射主库 [master] 中 t_open_platform 表的实体类。
 */
@Data
@TableName("t_open_platform")
public class AdminOpenPlatform {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String appId;
    private String appSecret;
    private String platformName;
    private Integer quotaRemain;
    private Integer status;
}