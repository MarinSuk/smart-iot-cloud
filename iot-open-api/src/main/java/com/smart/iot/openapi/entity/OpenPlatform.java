package com.smart.iot.openapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Entity mapping to table t_open_platform in [master] database.
 * 映射到主库 [master] 中 t_open_platform 表的实体类。
 */
@Data
@TableName("t_open_platform")
public class OpenPlatform {

    /** Primary key ID. 主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Platform AppId. 第三方平台ID。 */
    private String appId;

    /** Platform AppSecret. 第三方平台密钥。 */
    private String appSecret;

    /** Platform display name. 平台名称。 */
    private String platformName;

    /** Remaining API call quota. 剩余调用配额。 */
    private Integer quotaRemain;

    /** Platform status (1: Active, 0: Disabled). 状态（1启用，0禁用）。 */
    private Integer status;
}