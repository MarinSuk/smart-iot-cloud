package com.smart.iot.common.context;

/**
 * Immutable authentication context using JDK 17 Record feature.
 * 使用 JDK 17 Record 特性定义的不可变鉴权上下文对象。Record 是不可变的，final类型的，创建后内部值就不能更改，使用authContext.authType
 *
 * @param authType   authentication type ("ADMIN" or "OPEN_API") 鉴权类型（后台管理或开放平台）
 * @param subjectId  user ID or third-party AppId 用户ID或第三方平台AppId
 * @param subjectName username or platform name 用户名或平台名称
 */
public record AuthContext(String authType, String subjectId, String subjectName) {
}