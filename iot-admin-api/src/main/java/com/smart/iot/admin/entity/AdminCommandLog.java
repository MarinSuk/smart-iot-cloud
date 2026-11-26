package com.smart.iot.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Admin view entity for 4G command logs in [iot] DB.
 * 后台管理端映射设备库 [iot] 中 t_device_command_log 表的实体类。
 */
@Data
@TableName("t_device_command_log")
public class AdminCommandLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String cmdId;
    private String deviceSn;
    private String cmdType;
    private Integer status;
    private Long expireTimestamp;
    private LocalDateTime createTime;
}