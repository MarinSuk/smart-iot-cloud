package com.smart.iot.openapi.controller;

import com.smart.iot.common.result.Result;
import com.smart.iot.openapi.dto.DeviceTrackMessage;
import com.smart.iot.openapi.mq.DeviceReportProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Simulator controller to test duplicate high-concurrency device reporting.
 * 用于测试高并发重复设备上报的模拟控制器。
 */
@Tag(name = "Mock - Device Simulator") // 模拟测试-硬件上报
@RestController
@RequestMapping("/mock/device")
public class DeviceMockController {

    @Resource
    private DeviceReportProducer deviceReportProducer;

    /**
     * Simulate sending 5 identical track packets simultaneously to verify idempotency.
     * 模拟同时发送 5 条完全相同的轨迹报文，用于验证消费端的幂等性控制。
     *
     * @return Result success message 响应提示
     */
    @Operation(summary = "Simulate Duplicate Reports") // 模拟并发重复上报
    @PostMapping("/duplicate-report")
    public Result<String> simulateDuplicateReport() {
        long fixedTimestamp = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            DeviceTrackMessage msg = new DeviceTrackMessage();
            msg.setRideCode("RIDE_20261126_001");
            msg.setGpsId("GPS_998877");
            msg.setReportTime(fixedTimestamp);
            msg.setDeviceSn("LV_EB_20260001");
            msg.setLongitude(new BigDecimal("120.155123"));
            msg.setLatitude(new BigDecimal("30.274199"));
            msg.setSpeed(25);
            deviceReportProducer.sendTrackReport(msg);
        }
        // 已向设备MQ发送5条重复测试报文。
        return Result.success("Sent 5 duplicate messages to device RabbitMQ.");
    }
}