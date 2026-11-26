package com.smart.iot.openapi.mq;

import com.rabbitmq.client.Channel;
import com.smart.iot.openapi.config.CommandDlxTopologyConfig;
import com.smart.iot.openapi.service.RemoteCommandService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumer listening to the Dead Letter Queue (DLQ) to process expired 4G commands.
 * 监听死信队列（DLQ）的消费者，负责处理超时未响应的 4G 远控指令。
 */
@Slf4j
@Component
public class CommandDlxConsumer {

    @Resource
    private RemoteCommandService remoteCommandService;

    /**
     * Consume dead-lettered command IDs after 10s TTL expiration.
     * 在 10 秒 TTL 到期后消费进入死信队列的指令ID。
     *
     * @param message RabbitMQ dead letter message 死信消息
     * @param channel RabbitMQ channel for manual ACK 用于手动确认的信道
     * @throws IOException if ACK fails 确认异常
     */
    @RabbitListener(
            queues = CommandDlxTopologyConfig.CMD_DLX_QUEUE,
            containerFactory = "deviceContainerFactory"
    )
    public void onCommandExpired(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String cmdId = new String(message.getBody());

        try {
            remoteCommandService.expireTimeoutCommand(cmdId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 处理死信队列过期指令异常
            log.error("Error processing expired command in DLQ: cmdId={}", cmdId, e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}