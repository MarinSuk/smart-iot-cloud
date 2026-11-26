package com.smart.iot.common.interceptor;

import cn.hutool.core.util.StrUtil;
import com.smart.iot.common.context.AuthContext;
import com.smart.iot.common.context.AuthContextHolder;
import com.smart.iot.common.exception.BizException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * Interceptor for Admin APIs verifying Bearer Token in auth Redis.
 * 后台管理接口拦截器，用于在鉴权 Redis 中校验 Bearer Token。
 */
@Component
public class AdminTokenInterceptor implements HandlerInterceptor {

    @Resource
    @Qualifier("authRedisTemplate")
    private StringRedisTemplate authRedisTemplate;

    /** Redis key prefix for admin login tokens. 后台登录 Token 的 Redis 键前缀。 */
    private static final String TOKEN_KEY_PREFIX = "auth:admin:token:";

    /**
     * Intercept incoming admin requests before controller execution.
     * 在 Controller 执行前拦截后台管理请求。
     *
     * @param request  HTTP request 请求对象
     * @param response HTTP response 响应对象
     * @param handler  target handler 目标处理器
     * @return true if authorized 校验通过返回 true
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. Extract Authorization header / 提取 Authorization 请求头
        String authHeader = request.getHeader("Authorization");
        if (StrUtil.isBlank(authHeader) || !authHeader.startsWith("Bearer ")) {
            // 缺少或非法的认证请求头。
            throw new BizException(401, "Missing or invalid Authorization header.");
        }

        // 2. Parse token and query auth Redis / 解析 Token 并查询鉴权专用 Redis
        String token = authHeader.substring(7);
        String redisKey = TOKEN_KEY_PREFIX + token;
        String userInfo = authRedisTemplate.opsForValue().get(redisKey);

        if (StrUtil.isBlank(userInfo)) {
            // 登录已过期或凭证无效。
            throw new BizException(401, "Token expired or invalid.");
        }

        // 3. Auto-renew token TTL (sliding expiration: 2 hours) / 自动续期 Token 有效期（滑动过期：2小时）
        authRedisTemplate.expire(redisKey, 2, TimeUnit.HOURS);

        // 4. Parse "userId:username" and bind to ThreadLocal / 解析用户信息并绑定至当前线程上下文
        String[] parts = userInfo.split(":");
        String userId = parts[0];
        String username = parts.length > 1 ? parts[1] : "Admin";
        AuthContextHolder.setContext(new AuthContext("ADMIN", userId, username));

        return true;
    }

    /**
     * Clean up ThreadLocal after request completion.
     * 请求处理完成后清理 ThreadLocal 上下文。
     *
     * @param request  HTTP request 请求对象
     * @param response HTTP response 响应对象
     * @param handler  target handler 目标处理器
     * @param ex       exception if any 异常对象
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContextHolder.clear();
    }
}