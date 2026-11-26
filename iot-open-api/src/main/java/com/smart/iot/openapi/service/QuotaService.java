package com.smart.iot.openapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.iot.common.exception.BizException;
import com.smart.iot.openapi.entity.OpenPlatform;
import com.smart.iot.openapi.mapper.OpenPlatformMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Service managing third-party API quota deduction using atomic Redis Lua scripts.
 * 使用 Redis 原子 Lua 脚本管理第三方接口调用配额扣减的服务类。
 */
@Slf4j
@Service
public class QuotaService {

    @Resource
    @Qualifier("bizRedisTemplate")
    private StringRedisTemplate bizRedisTemplate;

    @Resource
    @Qualifier("authRedisTemplate")
    private StringRedisTemplate authRedisTemplate;

    @Resource
    private OpenPlatformMapper openPlatformMapper;

    /** Redis key prefix for platform quota in biz Redis. 业务 Redis 中存储平台配额的键前缀。 */
    private static final String QUOTA_KEY_PREFIX = "biz:open:quota:";

    /** Redis key prefix for platform secret in auth Redis. 鉴权 Redis 中存储平台密钥的键前缀。 */
    private static final String SECRET_KEY_PREFIX = "auth:open:secret:";

    /**
     * Lua script for atomic check-and-decrement.
     * Returns remaining quota (>= 0) if successful, or -1 if insufficient quota.
     * 原子检查并扣减配额的 Lua 脚本：扣减成功返回剩余配额（>=0），配额不足返回 -1。
     */
    private static final String DEDUCT_QUOTA_LUA = """
            local current = redis.call('get', KEYS[1])
            if (current and tonumber(current) > 0) then
                return redis.call('decr', KEYS[1])
            else
                return -1
            end
            """;

    /**
     * Warm up active platform secrets and quotas into Redis on application startup.
     * 应用启动时将启用的第三方平台密钥与配额预热加载至对应的 Redis 实例中。
     */
    @PostConstruct
    public void warmUpPlatformCache() {
        List<OpenPlatform> platforms = openPlatformMapper.selectList(
                new LambdaQueryWrapper<OpenPlatform>().eq(OpenPlatform::getStatus, 1)
        );
        for (OpenPlatform p : platforms) {
            // Store secret in authRedis / 密钥存入鉴权专用 Redis
            authRedisTemplate.opsForValue().set(SECRET_KEY_PREFIX + p.getAppId(), p.getAppSecret());
            // Store quota in bizRedis / 配额存入业务高频 Redis
            bizRedisTemplate.opsForValue().set(QUOTA_KEY_PREFIX + p.getAppId(), String.valueOf(p.getQuotaRemain()));
        }
        // 已预热 {} 个开放平台配置至 Redis
        log.info("Warmed up {} open platforms into Redis.", platforms.size(), platforms.size());
    }

    /**
     * Atomically check and deduct 1 API call quota for the specified platform.
     * 为指定第三方平台原子校验并扣减 1 次接口调用配额。
     *
     * @param appId third-party platform ID 第三方平台ID
     */
    public void checkAndDeductQuota(String appId) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(DEDUCT_QUOTA_LUA, Long.class);
        Long remain = bizRedisTemplate.execute(
                redisScript,
                Collections.singletonList(QUOTA_KEY_PREFIX + appId)
        );

        if (remain == null || remain < 0) {
            // 平台调用配额已耗尽
            log.warn("Quota exhausted for platform: appId={}", appId);
            // 接口调用配额已耗尽
            throw new BizException(429, "API call quota exhausted.");
        }
        // 配额扣减成功
        log.debug("Quota deducted: appId={}, remain={}", appId, remain);
    }
}