package com.smart.iot.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity mapping to table t_admin_user in [master] database.
 * 映射到主库 [master] 中 t_admin_user 表的后台管理员实体类。
 */
@Data
@TableName("t_admin_user")
public class AdminUser {

    /** Primary key ID. 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Admin login username. 管理员登录账号。 */
    private String username;

    /** Encrypted password hash. 加密后的密码哈希值。 */
    private String password;

    /** Display nickname. 显示昵称。 */
    private String nickname;

    /** Account status (1: Active, 0: Disabled). 账号状态（1启用，0禁用）。 */
    private Integer status;

    /** Account creation time. 账号创建时间。 */
    private LocalDateTime createTime;
}