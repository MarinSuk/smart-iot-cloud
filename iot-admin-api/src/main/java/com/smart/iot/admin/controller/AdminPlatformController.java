package com.smart.iot.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smart.iot.admin.entity.AdminCommandLog;
import com.smart.iot.admin.entity.AdminOpenPlatform;
import com.smart.iot.admin.mapper.AdminCommandLogMapper;
import com.smart.iot.admin.mapper.AdminOpenPlatformMapper;
import com.smart.iot.common.exception.BizException;
import com.smart.iot.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin controller for managing third-party platform quotas and monitoring 4G commands.
 * 后台管理控制器，负责第三方平台配额充值（双写同步）及 4G 指令日志监控。
 */
@Slf4j
@Tag(name = "Admin - Platform & Monitor") // 后台-平台配额与设备监控
@RestController
@RequestMapping("/admin/platform")
public class AdminPlatformController {

    @Resource
    private AdminOpenPlatformMapper platformMapper;

    @Resource
    private AdminCommandLogMapper commandLogMapper;

    @Resource
    @Qualifier("bizRedisTemplate")
    private StringRedisTemplate bizRedisTemplate;

    /** Redis key prefix for platform quota in biz Redis. 业务 Redis 中存储平台配额的键前缀。 */
    private static final String QUOTA_KEY_PREFIX = "biz:open:quota:";

    /**
     * List all registered third-party open platforms from [master] DB.
     * 从 [master] 主库查询所有已接入的第三方开放平台列表。
     *
     * @return Result containing platform list 包含平台列表的响应体
     */
    @Operation(summary = "List Open Platforms") // 查询第三方平台列表
    @GetMapping("/list")
    public Result<List<AdminOpenPlatform>> listPlatforms() {
        List<AdminOpenPlatform> list = platformMapper.selectList(
                new LambdaQueryWrapper<AdminOpenPlatform>().orderByDesc(AdminOpenPlatform::getId)
        );
        return Result.success(list);
    }

    /**
     * Recharge API call quota for a partner (Updates MySQL first, then increments Redis atomically).
     * 为合作方充值接口调用配额（先更新 MySQL 主库，再原子增加业务 Redis 配额，保障双写一致性）。
     *
     * @param appId       platform AppId 平台ID
     * @param addQuantity quota amount to add 增加的配额数量
     * @return Result success message 响应提示
     */
    @Operation(summary = "Recharge Platform Quota") // 第三方平台配额充值
    @PostMapping("/recharge")
    public Result<String> rechargeQuota(
            @Parameter(description = "Platform AppId", example = "partner_001") // 平台ID
            @RequestParam("appId") String appId,
            @Parameter(description = "Quota amount to add", example = "500") // 充值次数
            @RequestParam("addQuantity") Integer addQuantity) {

        if (addQuantity == null || addQuantity <= 0) {
            throw new BizException(400, "Recharge quantity must be positive."); // 充值数量必须大于0
        }

        // 1. Update MySQL [master] database atomically / 原子更新 MySQL [master] 主库配额
        int updated = platformMapper.update(null, new LambdaUpdateWrapper<AdminOpenPlatform>()
                .eq(AdminOpenPlatform::getAppId, appId)
                .setSql("quota_remain = quota_remain + " + addQuantity));

        if (updated == 0) {
            throw new BizException(404, "Platform AppId not found."); // 未找到对应的第三方平台
        }

        // 2. Atomically increment quota in bizRedis / 原子增加业务 Redis 中的剩余配额
        Long newQuota = bizRedisTemplate.opsForValue().increment(QUOTA_KEY_PREFIX + appId, addQuantity);
        // 平台配额充值成功
        log.info("Platform quota recharged: appId={}, added={}, currentRedisQuota={}",
                appId, addQuantity, newQuota);

        return Result.success("Quota recharged successfully."); // 配额充值成功
    }

    /**
     * Query latest 50 remote 4G command execution logs from [iot] DB.
     * 从 [iot] 设备库查询最近 50 条 4G 远控指令执行日志。
     *
     * @return Result containing command log list 包含指令日志列表的响应体
     */
    @Operation(summary = "List 4G Command Logs") // 查询4G远控指令日志
    @GetMapping("/command-logs")
    public Result<List<AdminCommandLog>> listCommandLogs() {
        List<AdminCommandLog> logs = commandLogMapper.selectList(
                new LambdaQueryWrapper<AdminCommandLog>()
                        .orderByDesc(AdminCommandLog::getId)
                        .last("LIMIT 50")
        );
        return Result.success(logs);
    }
}