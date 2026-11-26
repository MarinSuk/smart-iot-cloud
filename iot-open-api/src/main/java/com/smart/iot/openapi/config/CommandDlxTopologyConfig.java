package com.smart.iot.openapi.config;

import jakarta.annotation.Resource;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Dead Letter Exchange (DLX) and TTL topology for 4G command timeout handling.
 * 用于处理 4G 远控指令超时的 RabbitMQ 死信队列（DLX）与 TTL 拓扑配置。
 */
@Configuration
public class CommandDlxTopologyConfig {

    /** Normal exchange for pending command timeout tracking. 指令超时等待普通交换机。 */
    public static final String CMD_WAIT_EXCHANGE = "iot.cmd.wait.exchange";

    /** Normal queue with 10s TTL (no consumer attached, expires into DLX). 10秒TTL等待队列（无消费者，到期自动转入死信）。 */
    public static final String CMD_WAIT_QUEUE = "iot.cmd.wait.queue";

    /** Routing key for wait queue. 等待队列路由键。 */
    public static final String CMD_WAIT_ROUTING_KEY = "cmd.wait.timeout";

    /** Dead Letter Exchange (DLX) receiving expired command messages. 接收过期指令消息的死信交换机。 */
    public static final String CMD_DLX_EXCHANGE = "iot.cmd.dlx.exchange";

    /** Dead Letter Queue (DLQ) consumed to mark commands as expired. 死信队列（消费者监听此队列将超时指令置为失效）。 */
    public static final String CMD_DLX_QUEUE = "iot.cmd.dlx.queue";

    /** Dead letter routing key. 死信路由键。 */
    public static final String CMD_DLX_ROUTING_KEY = "cmd.dlx.expired";

    @Resource
    @Qualifier("deviceRabbitAdmin")
    private RabbitAdmin deviceRabbitAdmin;

    /**
     * Declare normal TTL wait queue bound to DLX, and the dead letter queue on [device] RabbitMQ.
     * 在 [device] RabbitMQ 上声明绑定了死信交换机的 TTL 等待队列，以及死信队列。
     *
     * @return Declarables bound to deviceRabbitAdmin 绑定至设备RabbitAdmin的声明集合
     */
    @Bean
    public Declarables commandDlxDeclarables() {
        // 1. Declare Dead Letter Exchange and Dead Letter Queue / 声明死信交换机与死信队列
        DirectExchange dlxExchange = ExchangeBuilder
                .directExchange(CMD_DLX_EXCHANGE)
                .durable(true)
                .build();
        Queue dlxQueue = QueueBuilder
                .durable(CMD_DLX_QUEUE)
                .build();
        Binding dlxBinding = BindingBuilder
                .bind(dlxQueue)
                .to(dlxExchange)
                .with(CMD_DLX_ROUTING_KEY);

        // 2. Declare Normal Exchange and 10-second TTL Wait Queue forwarding dead letters to DLX
        // 声明普通交换机与 10 秒 TTL 等待队列，配置死信转发参数指向上述 DLX
        DirectExchange waitExchange = ExchangeBuilder
                .directExchange(CMD_WAIT_EXCHANGE)
                .durable(true)
                .build();
        Queue waitQueue = QueueBuilder
                .durable(CMD_WAIT_QUEUE)
                .withArgument("x-message-ttl", 10000) // 10 seconds TTL / 10秒消息存活时间
                .withArgument("x-dead-letter-exchange", CMD_DLX_EXCHANGE) // Target DLX / 指定死信交换机
                .withArgument("x-dead-letter-routing-key", CMD_DLX_ROUTING_KEY) // Target DLX routing key / 指定死信路由键
                .build();
        Binding waitBinding = BindingBuilder
                .bind(waitQueue)
                .to(waitExchange)
                .with(CMD_WAIT_ROUTING_KEY);

        // 3. Bind all declarables to [device] RabbitAdmin / 将所有声明组件绑定至 [device] 数据源管理器
        Declarables declarables = new Declarables(
                dlxExchange, dlxQueue, dlxBinding,
                waitExchange, waitQueue, waitBinding
        );
        declarables.getDeclarables().forEach(item -> item.setAdminsThatShouldDeclare(deviceRabbitAdmin));
        return declarables;
    }
}