package com.smart.iot.openapi.mq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.smart.iot.openapi.config.DeviceMqTopologyConfig;
import com.smart.iot.openapi.dto.DeviceTrackMessage;
import com.smart.iot.openapi.entity.DeviceTrackLog;
import com.smart.iot.openapi.mapper.DeviceTrackLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Consumer listening to [device] RabbitMQ with idempotency control, NPE defense, and manual ACK.
 * 监听 [device] RabbitMQ 的消费者，内置幂等性控制、空指针防御及手动 ACK 确认机制。
 */
@Slf4j
@Component
public class DeviceReportConsumer {

    @Resource
    @Qualifier("bizRedisTemplate")
    private StringRedisTemplate bizRedisTemplate;

    @Resource
    private DeviceTrackLogMapper deviceTrackLogMapper;

    /** Redis key prefix for message idempotency check. 消息幂等性校验的 Redis 键前缀。 */
    private static final String IDEMPOTENT_KEY_PREFIX = "biz:mq:track:idem:";

    /**
     * Consume device track report messages from [device] MQ container.
     * 从 [device] MQ 容器消费设备轨迹上报消息。
     *
     * @param message raw RabbitMQ message 原始MQ消息
     * @param channel RabbitMQ channel for manual ACK/NACK 用于手动确认的信道
     * @throws IOException if channel ack/nack fails 信道确认异常
     */
    @RabbitListener(
            queues = DeviceMqTopologyConfig.DEVICE_TRACK_QUEUE,
            containerFactory = "deviceContainerFactory" // Must bind to device MQ container / 必须绑定设备MQ容器工厂
    )
    public void onTrackMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String rawBody = new String(message.getBody());

        try {
            // 1. Parse JSON and perform defensive validation (Prevent NPE on malformed hardware packets)
            // 解析 JSON 并执行防御性校验（防止异构固件或残缺报文引发空指针异常导致无限重试）
            DeviceTrackMessage dto = JSONUtil.toBean(rawBody, DeviceTrackMessage.class);
            if (dto == null || StrUtil.hasBlank(dto.getRideCode(), dto.getGpsId(), dto.getDeviceSn())
                    || dto.getReportTime() == null || dto.getLongitude() == null || dto.getLatitude() == null) {
                // 丢弃字段缺失的异常硬件报文
                log.warn("Malformed device packet discarded: body={}", rawBody);
                // Discard invalid message directly (requeue = false) / 直接确认丢弃非法报文，避免阻塞队列
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. First-layer Idempotency: Redis SETNX with composite business key (rideCode + gpsId + reportTime)
            // 第一层幂等防护：使用业务联合唯一键（骑行编码 + GPS_ID + 时间戳）在 Redis 中原子占位
            String uniqueBizKey = dto.getRideCode() + ":" + dto.getGpsId() + ":" + dto.getReportTime();
            Boolean isFirstConsume = bizRedisTemplate.opsForValue()
                    .setIfAbsent(IDEMPOTENT_KEY_PREFIX + uniqueBizKey, "1", 24, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(isFirstConsume)) {
                // Redis幂等拦截重复上报报文
                log.info("Duplicate track message skipped by Redis: key={}", uniqueBizKey);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 3. Persist to MySQL [iot] database (Second-layer Idempotency via Unique Index)
            // 写入 MySQL [iot] 库（第二层幂等兜底：数据库联合唯一索引 uk_ride_gps_time）
            DeviceTrackLog entity = new DeviceTrackLog();
            BeanUtils.copyProperties(dto, entity);
            try {
                deviceTrackLogMapper.insert(entity);
            } catch (DuplicateKeyException dupEx) {
                // Gracefully catch duplicate key if Redis expired or evicted / 若Redis过期导致并发穿透，优雅捕获唯一索引冲突
                // 若Redis过期导致并发穿透，优雅捕获唯一索引冲突
                log.warn("Duplicate track record caught by DB Unique Index: key={}", uniqueBizKey);
            }

            // 4. Manual ACK after DB persistence succeeds / 数据库落库彻底成功后，手动发送 ACK
            channel.basicAck(deliveryTag, false);
            // 轨迹报文处理并落库成功
            log.debug("Track message processed successfully: key={}", uniqueBizKey);

        } catch (Exception ex) {
            // 5. System/DB transient error -> NACK and requeue (or route to DLX in production)
            // 数据库连接抖动等系统异常 -> 拒绝消息并重新入队（生产环境可结合重试次数转入死信队列）
            // 处理轨迹报文异常，重新入队
            log.error("Failed to process track message, requeuing: body={}", rawBody, ex);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}