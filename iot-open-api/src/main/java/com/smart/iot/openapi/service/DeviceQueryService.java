package com.smart.iot.openapi.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.iot.common.exception.BizException;
import com.smart.iot.openapi.entity.DeviceInfo;
import com.smart.iot.openapi.mapper.DeviceInfoMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Vehicle device query service demonstrating solutions for Cache Penetration, Breakdown, and Avalanche.
 * 车辆设备查询服务，演示缓存穿透、缓存击穿与缓存雪崩的完整工程解决方案。
 */
@Slf4j
@Service
public class DeviceQueryService {

    @Resource
    @Qualifier("bizRedisTemplate")
    private StringRedisTemplate bizRedisTemplate;

    @Resource
    private DeviceInfoMapper deviceInfoMapper;

    /** Redis key prefix for cached device info. 车辆设备缓存键前缀。 */
    private static final String DEVICE_CACHE_PREFIX = "biz:device:info:";

    /** Redis key prefix for distributed mutex lock (anti-breakdown). 分布式互斥锁键前缀（防缓存击穿）。 */
    private static final String DEVICE_LOCK_PREFIX = "biz:device:lock:";

    /** Placeholder string for non-existent DB records (anti-penetration). 空值占位符（防缓存穿透）。 */
    private static final String NULL_PLACEHOLDER = "NULL_OBJECT";

    /**
     * Query vehicle device info with multi-layer cache protection and service validity check.
     * 查询车辆设备信息，内置多级缓存防护机制及车联服务有效期拦截。
     *
     * @param deviceSn vehicle serial number 车辆硬件序列号
     * @return DeviceInfo entity 设备信息实体
     */
    public DeviceInfo getDeviceWithProtection(String deviceSn) {
        String cacheKey = DEVICE_CACHE_PREFIX + deviceSn;

        // 1. Query Redis cache first / 优先查询 Redis 缓存
        String cachedJson = bizRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(cachedJson)) {
            // [Anti-Penetration] Hit null placeholder, block malicious query immediately
            // [防缓存穿透] 命中空值占位符，直接拦截恶意不存在数据的查询，保护 MySQL
            if (NULL_PLACEHOLDER.equals(cachedJson)) {
                // 车辆设备不存在
                throw new BizException(404, "Device does not exist. ");
            }
            DeviceInfo device = JSONUtil.toBean(cachedJson, DeviceInfo.class);
            validateDeviceServiceExpiry(device);
            return device;
        }

        // 2. Cache miss -> Rebuild cache with Distributed Mutex Lock
        // 缓存未命中 -> 使用分布式互斥锁重建缓存（防止热点Key过期瞬间引发缓存击穿）
        return rebuildCacheWithLock(deviceSn, cacheKey);
    }

    /**
     * Rebuild cache safely using Redis SETNX mutex lock and double-check locking (DCL).
     * 使用 Redis SETNX 互斥锁与双重检查锁（DCL）安全重建缓存，防止缓存击穿。
     *
     * @param deviceSn vehicle serial number 车辆序列号
     * @param cacheKey target Redis cache key 目标缓存键
     * @return DeviceInfo entity 设备信息实体
     */
    private DeviceInfo rebuildCacheWithLock(String deviceSn, String cacheKey) {
        String lockKey = DEVICE_LOCK_PREFIX + deviceSn;
        // Try to acquire lock with 10s TTL to prevent deadlock / 尝试获取互斥锁（10秒自动过期防死锁）
        Boolean locked = bizRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            try {
                // Double-check cache after acquiring lock / 获取锁后再次检查缓存（双重检查锁）
                String doubleCheckJson = bizRedisTemplate.opsForValue().get(cacheKey);
                if (StrUtil.isNotBlank(doubleCheckJson)) {
                    if (NULL_PLACEHOLDER.equals(doubleCheckJson)) {
                        // 车辆设备不存在
                        throw new BizException(404, "Device does not exist. ");
                    }
                    DeviceInfo cachedDevice = JSONUtil.toBean(doubleCheckJson, DeviceInfo.class);
                    validateDeviceServiceExpiry(cachedDevice);
                    return cachedDevice;
                }

                // Query [iot] MySQL database / 查询 [iot] 设备数据库
                DeviceInfo dbDevice = deviceInfoMapper.selectOne(
                        new LambdaQueryWrapper<DeviceInfo>().eq(DeviceInfo::getDeviceSn, deviceSn)
                );

                if (dbDevice == null) {
                    // [Anti-Penetration] Cache null value with short TTL (2 mins)
                    // [防缓存穿透] 数据库也不存在，写入空值占位符并设置较短过期时间（2分钟）
                    bizRedisTemplate.opsForValue().set(cacheKey, NULL_PLACEHOLDER, 2, TimeUnit.MINUTES);
                    // 车辆设备不存在
                    throw new BizException(404, "Device does not exist. ");
                }

                // [Anti-Avalanche] Base TTL (30 mins) + Random Jitter (0~300s) to prevent simultaneous expiration
                // [防缓存雪崩] 基础过期时间（30分钟）+ 随机抖动（0~300秒），避免大量 Key 同时失效压垮数据库
                long ttlSeconds = 30 * 60L + RandomUtil.randomLong(0, 300);
                bizRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(dbDevice), ttlSeconds, TimeUnit.SECONDS);

                validateDeviceServiceExpiry(dbDevice);
                return dbDevice;
            } finally {
                // Always release mutex lock / 务必在 finally 中释放互斥锁
                bizRedisTemplate.delete(lockKey);
            }
        } else {
            // Failed to acquire lock -> Sleep briefly and retry / 未抢到锁的并发线程短暂休眠后重试
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getDeviceWithProtection(deviceSn);
        }
    }

    /**
     * Validate if the vehicle's IoT service subscription is still valid.
     * 校验车辆的车联服务有效期（对应简历中的“车联有效期拦截”）。
     *
     * @param device DeviceInfo entity 设备信息实体
     */
    private void validateDeviceServiceExpiry(DeviceInfo device) {
        if (device.getExpireTime() != null && device.getExpireTime().isBefore(LocalDateTime.now())) {
            // 车辆车联服务已过期
            throw new BizException(403, "Vehicle IoT service has expired.");
        }
    }
}