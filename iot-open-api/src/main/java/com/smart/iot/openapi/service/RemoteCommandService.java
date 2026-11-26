package com.smart.iot.openapi.service;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smart.iot.common.exception.BizException;
import com.smart.iot.openapi.config.CommandDlxTopologyConfig;
import com.smart.iot.openapi.entity.DeviceCommandLog;
import com.smart.iot.openapi.mapper.DeviceCommandLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service managing 4G remote control commands, anti-duplicate locks, and DLX timeout expiration.
 * 管理 4G 远控指令下发、防连点分布式锁及死信队列超时失效的服务类。
 */
@Slf4j
@Service
public class RemoteCommandService {

    @Resource
    @Qualifier("bizRedisTemplate")
    private StringRedisTemplate bizRedisTemplate;

    @Resource
    @Qualifier("deviceRabbitTemplate")
    private RabbitTemplate deviceRabbitTemplate;

    @Resource
    private DeviceCommandLogMapper commandLogMapper;

    /** Redis key prefix for preventing duplicate button clicks. 防止用户连续点击开锁的分布式锁键前缀。 */
    private static final String CMD_CLICK_LOCK_PREFIX = "biz:cmd:lock:";

    /** Command valid duration in milliseconds (10 seconds). 指令有效时长（10秒）。 */
    private static final long CMD_TTL_MS = 10_000L;

    /**
     * Send a 4G remote unlock command to the vehicle and schedule a 10s DLX timeout check.
     * 向车辆下发 4G 远程开锁指令，并投递 10 秒死信队列超时检查任务。
     *
     * @param deviceSn vehicle serial number 车辆序列号
     * @param cmdType  command type (e.g., UNLOCK) 指令类型（如 UNLOCK）
     * @return generated global unique cmdId 生成的全局唯一指令ID
     */
    public String sendRemoteCommand(String deviceSn, String cmdType) {
        // 1. Prevent rapid duplicate clicks within 3 seconds using Redis SETNX
        // 利用 Redis SETNX 防止用户在 3 秒内焦急连续点击发送重复指令
        String lockKey = CMD_CLICK_LOCK_PREFIX + deviceSn + ":" + cmdType;
        Boolean acquired = bizRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 3, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(acquired)) {
            // 指令正在下发中，请勿重复点击。
            throw new BizException(429, "Command is being processed, please do not click repeatedly.");
        }

        // 2. Persist command log with status = 0 (Pending) and 10s expiration timestamp
        // 持久化指令流水，初始状态为 0（下发中），并记录 10 秒后的绝对过期时间戳
        String cmdId = "CMD_" + UUID.fastUUID().toString(true);
        long expireAt = System.currentTimeMillis() + CMD_TTL_MS;

        DeviceCommandLog cmdLog = new DeviceCommandLog();
        cmdLog.setCmdId(cmdId);
        cmdLog.setDeviceSn(deviceSn);
        cmdLog.setCmdType(cmdType);
        cmdLog.setStatus(0); // 0: Pending / 下发中
        cmdLog.setExpireTimestamp(expireAt);
        commandLogMapper.insert(cmdLog);

        // 3. Publish cmdId to the 10s TTL wait queue (will enter DLQ if not ACKed within 10s)
        // 将指令ID投递至 10 秒 TTL 等待队列（若 10 秒内未收到车辆回执，将自动进入死信队列）
        deviceRabbitTemplate.convertAndSend(
                CommandDlxTopologyConfig.CMD_WAIT_EXCHANGE,
                CommandDlxTopologyConfig.CMD_WAIT_ROUTING_KEY,
                cmdId,
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                }
        );
        // 4G远控指令已下发
        log.info("4G command dispatched: cmdId={}, deviceSn={}, cmdType={}", cmdId, deviceSn, cmdType);
        return cmdId;
    }

    /**
     * Handle execution acknowledgment (ACK) reported back from the vehicle hardware.
     * 处理车辆硬件执行完成后回传的执行回执（ACK）。
     *
     * @param cmdId global unique command ID 全局唯一指令ID
     */
    public void handleVehicleAck(String cmdId) {
        DeviceCommandLog cmdLog = commandLogMapper.selectOne(
                new LambdaQueryWrapper<DeviceCommandLog>().eq(DeviceCommandLog::getCmdId, cmdId)
        );
        if (cmdLog == null) {
            // 未找到对应的指令记录。
            throw new BizException(404, "Command record not found.");
        }

        // Check if command already expired (e.g., vehicle just came out of underground garage)
        // 校验指令是否已过期（防止车辆刚出地下室时执行了延迟积压的开锁指令）
        if (cmdLog.getStatus() == 2 || System.currentTimeMillis() > cmdLog.getExpireTimestamp()) {
            // 拒绝已过期指令的延迟回执
            log.warn("Rejected late vehicle ACK for expired command: cmdId={}", cmdId);
            // 指令已超时失效，拒绝执行。
            throw new BizException(400, "Command already expired, execution aborted.");
        }

        // Optimistic CAS update: only transition from status 0 (Pending) to 1 (Success)
        // 乐观锁状态机更新：仅允许从状态 0（下发中）流转至 1（执行成功）
        int updated = commandLogMapper.update(null, new LambdaUpdateWrapper<DeviceCommandLog>()
                .eq(DeviceCommandLog::getCmdId, cmdId)
                .eq(DeviceCommandLog::getStatus, 0)
                .set(DeviceCommandLog::getStatus, 1));

        if (updated > 0) {
            // 车辆执行远控指令成功
            log.info("Vehicle executed command successfully: cmdId={}", cmdId);
        }
    }

    /**
     * Mark pending command as Timeout Expired (status = 2) when triggered by Dead Letter Queue.
     * 当死信队列触发时，将仍处于“下发中”的指令标记为超时失效（状态 = 2）。
     *
     * @param cmdId global unique command ID 全局唯一指令ID
     */
    public void expireTimeoutCommand(String cmdId) {
        // Only update if status is still 0 (Pending); ignore if already 1 (Success)
        // 仅当状态仍为 0（下发中）时才更新为 2（超时失效）；若已为 1（成功）则直接忽略
        int updated = commandLogMapper.update(null, new LambdaUpdateWrapper<DeviceCommandLog>()
                .eq(DeviceCommandLog::getCmdId, cmdId)
                .eq(DeviceCommandLog::getStatus, 0)
                .set(DeviceCommandLog::getStatus, 2));

        if (updated > 0) {
            // 车辆10秒未响应，指令已自动标记为超时失效
            log.warn("Command expired due to 10s vehicle timeout: cmdId={}", cmdId);
        } else {
            // 指令在超时前已完成，忽略死信消息
            log.info("Command already completed before timeout, DLX message ignored: cmdId={}", cmdId);
        }
    }
}