package com.smart.iot.openapi.config;

import jakarta.annotation.Resource;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology configuration exclusively bound to the [device] MQ data source.
 * 专属绑定至 [device] 设备数据源的 RabbitMQ 交换机与队列拓扑配置。
 */
@Configuration
public class DeviceMqTopologyConfig {

    /** Topic exchange for device telemetry data. 设备遥测数据主题交换机。 */
    public static final String DEVICE_REPORT_EXCHANGE = "iot.device.report.exchange";

    /** Durable queue for vehicle track reporting. 车辆轨迹上报持久化队列。 */
    public static final String DEVICE_TRACK_QUEUE = "iot.device.track.queue";

    /** Routing key pattern for track messages. 轨迹消息路由键。 */
    public static final String DEVICE_TRACK_ROUTING_KEY = "device.track.report";

    @Resource
    @Qualifier("deviceRabbitAdmin")
    private RabbitAdmin deviceRabbitAdmin;

    /**
     * Declare durable TopicExchange, Queue, and Binding on [device] RabbitMQ.
     * 在 [device] RabbitMQ 上声明持久化交换机、队列及绑定关系。
     *
     * @return Declarables collection bound to deviceRabbitAdmin 绑定至设备RabbitAdmin的声明集合
     */
    @Bean
    public Declarables deviceReportDeclarables() {
        // 1. Create durable TopicExchange / 创建持久化主题交换机
        TopicExchange exchange = ExchangeBuilder
                .topicExchange(DEVICE_REPORT_EXCHANGE)
                .durable(true)
                .build();

        // 2. Create durable Queue / 创建持久化队列
        Queue queue = QueueBuilder
                .durable(DEVICE_TRACK_QUEUE)
                .build();

        // 3. Bind Queue to Exchange / 将队列绑定到交换机
        Binding binding = BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(DEVICE_TRACK_ROUTING_KEY);

        // 4. Bind each Declarable item to deviceRabbitAdmin so they won't be created on primary inner MQ
        // 遍历每个声明组件并显式指定 deviceRabbitAdmin，防止默认创建到主库 inner MQ 上
        Declarables declarables = new Declarables(exchange, queue, binding);
        declarables.getDeclarables().forEach(declarable -> declarable.setAdminsThatShouldDeclare(deviceRabbitAdmin));
        return declarables;
    }
}