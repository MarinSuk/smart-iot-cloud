package com.smart.iot.openapi.mq;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.smart.iot.openapi.config.DeviceMqTopologyConfig;
import com.smart.iot.openapi.dto.DeviceTrackMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Producer simulating hardware decoding platform publishing messages with Confirm/Return callbacks.
 * 模拟硬件解码平台发送报文的生产者，配置 Confirm 与 Return 回调确保消息不丢失。
 */
@Slf4j
@Component
public class DeviceReportProducer {

    @Resource
    @Qualifier("deviceRabbitTemplate")
    private RabbitTemplate deviceRabbitTemplate;

    /**
     * Initialize publisher confirm and return callbacks on startup.
     * 启动时初始化生产者确认（Confirm）与路由失败退回（Return）回调。
     */
    @PostConstruct
    public void initCallbacks() {
        // 1. ConfirmCallback: Verify whether message reached the Exchange
        // ConfirmCallback：确认消息是否成功到达 Exchange 交换机
        deviceRabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String msgId = correlationData != null ? correlationData.getId() : "unknown";
            if (ack) {
                // 消息成功到达交换机
                log.debug("Message reached Exchange: msgId={}", msgId);
            } else {
                // In production, save failed message to local retry table / 生产环境此处应落库本地重试表进行补偿
                // 消息到达交换机失败
                log.error("Message failed to reach Exchange: msgId={}, cause={}", msgId, cause);
            }
        });

        // 2. ReturnsCallback: Triggered if Exchange fails to route message to any Queue
        // ReturnsCallback：当交换机无法将消息路由到任何队列时触发退回
        deviceRabbitTemplate.setReturnsCallback(returned -> {
            // 消息路由至队列失败
            log.error("Message unroutable to Queue: exchange={}, routingKey={}, replyText={}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
        });
    }

    /**
     * Send persistent vehicle track message to [device] RabbitMQ.
     * 发送持久化车辆轨迹消息至 [device] RabbitMQ。
     *
     * @param trackMessage device track DTO 设备轨迹消息对象
     */
    public void sendTrackReport(DeviceTrackMessage trackMessage) {
        String msgId = UUID.fastUUID().toString(true);
        CorrelationData correlationData = new CorrelationData(msgId);
        String jsonBody = JSONUtil.toJsonStr(trackMessage);

        deviceRabbitTemplate.convertAndSend(
                DeviceMqTopologyConfig.DEVICE_REPORT_EXCHANGE,
                DeviceMqTopologyConfig.DEVICE_TRACK_ROUTING_KEY,
                jsonBody,
                message -> {
                    // Set message persistence on disk / 设置消息持久化到磁盘，防止MQ重启丢失
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setMessageId(msgId);
                    return message;
                },
                correlationData
        );
    }
}