package com.smart.iot.common.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Interceptor for Third-Party OpenAPIs verifying AppId, Timestamp, Nonce, and HMAC-SHA256 Sign.
 * 第三方开放平台接口拦截器，负责校验平台ID、时间戳、防重放随机串及 HMAC-SHA256 签名。
 */
@Component
public class OpenApiSignInterceptor implements HandlerInterceptor {

    @Resource
    @Qualifier("authRedisTemplate")
    private StringRedisTemplate authRedisTemplate;

    /** Allowed timestamp drift in milliseconds (5 minutes). 允许的时间戳误差（5分钟）。 */
    private static final long ALLOWED_TIME_WINDOW_MS = 5 * 60 * 1000L;

    /** Redis key prefix for anti-replay nonce. 防重放随机串的 Redis 键前缀。 */
    private static final String NONCE_KEY_PREFIX = "auth:open:nonce:";

    /** Redis key prefix for cached platform secrets. 缓存第三方平台密钥的 Redis 键前缀。 */
    private static final String SECRET_KEY_PREFIX = "auth:open:secret:";

    /**
     * Verify third-party signature and prevent replay attacks.
     * 校验第三方签名并防止重放攻击。
     *
     * @param request  HTTP request 请求对象
     * @param response HTTP response 响应对象
     * @param handler  target handler 目标处理器
     * @return true if signature is valid 签名合法返回 true
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. Extract required security headers / 提取必要的安全校验请求头
        String appId = request.getHeader("X-App-Id");
        String timestampStr = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String clientSign = request.getHeader("X-Sign");

        if (StrUtil.hasBlank(appId, timestampStr, nonce, clientSign)) {
            // 缺少开放平台安全校验请求头。
            throw new BizException(401, "Missing OpenAPI security headers.");
        }

        // 2. Validate timestamp window to block expired requests / 校验时间戳窗口，拦截过期请求
        long requestTime;
        try {
            requestTime = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            // 时间戳格式非法。
            throw new BizException(400, "Invalid timestamp format.");
        }
        if (Math.abs(System.currentTimeMillis() - requestTime) > ALLOWED_TIME_WINDOW_MS) {
            // 请求时间戳已过期。
            throw new BizException(401, "Request timestamp expired.");
        }

        // 3. Anti-replay check using Redis SETNX (atomic operation) / 利用 Redis SETNX 原子操作防止重放攻击
        String nonceKey = NONCE_KEY_PREFIX + appId + ":" + nonce;
        Boolean isFirstRequest = authRedisTemplate.opsForValue()
                .setIfAbsent(nonceKey, "1", 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(isFirstRequest)) {
            // 检测到重复请求（重放攻击拦截）。
            throw new BizException(403, "Duplicate request detected (Replay attack).");
        }

        // 4. Fetch AppSecret from Redis (will integrate DB fallback in Phase 2) / 从 Redis 获取平台密钥（第二阶段接入数据库兜底）
        String appSecret = authRedisTemplate.opsForValue().get(SECRET_KEY_PREFIX + appId);
        if (StrUtil.isBlank(appSecret)) {
            // 无效或已禁用的平台ID。
            throw new BizException(401, "Invalid or disabled AppId.");
        }

        // 5. Recompute HMAC-SHA256 signature and compare / 重新计算 HMAC-SHA256 签名并比对
        // Signature payload format: appId + timestamp + nonce + requestURI / 签名原文格式：平台ID + 时间戳 + 随机串 + 请求路径
        String rawPayload = appId + timestampStr + nonce + request.getRequestURI();
        HMac hmac = new HMac(HmacAlgorithm.HmacSHA256, appSecret.getBytes(StandardCharsets.UTF_8));
        String serverSign = hmac.digestHex(rawPayload);

        if (!serverSign.equalsIgnoreCase(clientSign)) {
            // 接口签名校验失败。
            throw new BizException(401, "Signature verification failed.");
        }

        // 6. Bind platform identity to ThreadLocal / 将第三方平台身份绑定至当前线程上下文
        AuthContextHolder.setContext(new AuthContext("OPEN_API", appId, "Partner-" + appId));
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