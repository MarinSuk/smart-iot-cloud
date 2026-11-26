package com.smart.iot.openapi.controller;

import com.smart.iot.common.context.AuthContext;
import com.smart.iot.common.context.AuthContextHolder;
import com.smart.iot.common.result.Result;
import com.smart.iot.openapi.entity.DeviceInfo;
import com.smart.iot.openapi.service.DeviceQueryService;
import com.smart.iot.openapi.service.QuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Third-party OpenAPI controller for querying vehicle device status.
 * 第三方开放平台车辆设备状态查询控制器。
 */
@Tag(name = "OpenAPI - Vehicle Device") // 开放平台-车辆设备接口
@RestController
@RequestMapping("/open/device")
public class OpenDeviceController {

    @Resource
    private QuotaService quotaService;

    @Resource
    private DeviceQueryService deviceQueryService;

    /**
     * Query real-time vehicle status by serial number (Protected by Sign Auth + Quota + Cache).
     * 根据车辆序列号查询实时状态（受签名鉴权、配额扣减及多级缓存保护）。
     *
     * @param deviceSn vehicle serial number 车辆硬件序列号
     * @return Result containing DeviceInfo 包含设备信息的统一响应体
     */
    @Operation(summary = "Query Vehicle Status") // 查询车辆实时状态
    @GetMapping("/{deviceSn}")
    public Result<DeviceInfo> getDeviceStatus(
            @Parameter(description = "Vehicle Serial Number", example = "LV_EB_20260001") // 车辆序列号
            @PathVariable("deviceSn") String deviceSn) {

        // 1. Get authenticated partner AppId from ThreadLocal / 从 ThreadLocal 获取已鉴权的第三方平台ID
        AuthContext context = AuthContextHolder.getContext();

        // 2. Atomically deduct API quota via Lua script / 通过 Lua 脚本原子扣减该平台的调用配额
        quotaService.checkAndDeductQuota(context.subjectId());

        // 3. Query device info with cache protection & expiry interception / 查询设备信息（含缓存防护与有效期拦截）
        DeviceInfo deviceInfo = deviceQueryService.getDeviceWithProtection(deviceSn);

        return Result.success(deviceInfo);
    }
}