package com.smart.iot.openapi.controller;

import com.smart.iot.common.context.AuthContext;
import com.smart.iot.common.context.AuthContextHolder;
import com.smart.iot.common.result.Result;
import com.smart.iot.openapi.service.QuotaService;
import com.smart.iot.openapi.service.RemoteCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for dispatching 4G remote control commands and receiving vehicle ACKs.
 * 4G 远控指令下发及模拟车辆回执控制器。
 */
@Tag(name = "OpenAPI - 4G Remote Command") // 开放平台-4G远控指令接口
@RestController
public class OpenCommandController {

    @Resource
    private QuotaService quotaService;

    @Resource
    private RemoteCommandService remoteCommandService;

    /**
     * Dispatch a 4G unlock command (Protected by Sign Auth + Quota + 10s DLX Timeout).
     * 下发 4G 远程开锁指令（受签名鉴权、配额扣减及 10 秒死信超时机制保护）。
     *
     * @param deviceSn vehicle serial number 车辆序列号
     * @return Result containing generated cmdId 包含指令ID的响应体
     */
    @Operation(summary = "Dispatch 4G Unlock Command") // 下发4G远程开锁指令
    @PostMapping("/open/command/unlock/{deviceSn}")
    public Result<String> unlockVehicle(
            @Parameter(description = "Vehicle Serial Number", example = "LV_EB_20260001") // 车辆序列号
            @PathVariable("deviceSn") String deviceSn) {

        // 1. Deduct partner API quota / 扣减第三方平台接口配额
        AuthContext context = AuthContextHolder.getContext();
        quotaService.checkAndDeductQuota(context.subjectId());

        // 2. Dispatch 4G command and start 10s DLX countdown / 下发4G指令并启动10秒死信倒计时
        String cmdId = remoteCommandService.sendRemoteCommand(deviceSn, "UNLOCK");
        return Result.success(cmdId);
    }

    /**
     * Mock endpoint simulating vehicle hardware reporting execution ACK within 10 seconds.
     * 模拟车辆硬件在 10 秒内回传执行成功回执的测试接口。
     *
     * @param cmdId global unique command ID 全局唯一指令ID
     * @return Result success message 响应提示
     */
    @Operation(summary = "Mock Vehicle Command ACK") // 模拟车机执行回执
    @PostMapping("/mock/device/command/ack/{cmdId}")
    public Result<String> mockVehicleAck(
            @Parameter(description = "Command ID") // 指令ID
            @PathVariable("cmdId") String cmdId) {

        remoteCommandService.handleVehicleAck(cmdId);
        return Result.success("Vehicle ACK processed successfully."); // 车机指令回执处理成功。
    }
}