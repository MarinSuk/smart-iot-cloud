package com.smart.iot.common.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MultiRabbitConfig {

    /**
     * Primary RabbitMQ properties for internal business MQ.
     * 内部业务 MQ 的主配置属性（添加 @Primary 解决与 Spring Boot 自动装配的 Bean 冲突）。
     *
     * @return RabbitProperties instance 属性对象
     */
    @Primary
    @Bean("innerMqProps")
    @ConfigurationProperties(prefix = "spring.custom-rabbitmq.inner")
    public RabbitProperties innerMqProps() {
        return new RabbitProperties();
    }

    @Bean("deviceMqProps")
    @ConfigurationProperties(prefix = "spring.custom-rabbitmq.device")
    public RabbitProperties deviceMqProps() {
        return new RabbitProperties();
    }

    @Primary
    @Bean("innerConnectionFactory")
    public ConnectionFactory innerConnectionFactory(@Qualifier("innerMqProps") RabbitProperties props) {
        return buildConnectionFactory(props);
    }

    @Bean("deviceConnectionFactory")
    public ConnectionFactory deviceConnectionFactory(@Qualifier("deviceMqProps") RabbitProperties props) {
        return buildConnectionFactory(props);
    }

    @Primary
    @Bean("innerRabbitTemplate")
    public RabbitTemplate innerRabbitTemplate(@Qualifier("innerConnectionFactory") ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMandatory(true); // 开启失败回调，确保消息路由到队列
        return template;
    }

    @Bean("deviceRabbitTemplate")
    public RabbitTemplate deviceRabbitTemplate(@Qualifier("deviceConnectionFactory") ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMandatory(true);
        return template;
    }

    /**
     * 消费者容器工厂：后续 @RabbitListener(containerFactory = "deviceContainerFactory") 指定监听设备MQ
     */
    @Bean("deviceContainerFactory")
    public SimpleRabbitListenerContainerFactory deviceContainerFactory(
            @Qualifier("deviceConnectionFactory") ConnectionFactory factory) {
        SimpleRabbitListenerContainerFactory containerFactory = new SimpleRabbitListenerContainerFactory();
        containerFactory.setConnectionFactory(factory);
        containerFactory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL); // 强制手动ACK
        containerFactory.setPrefetchCount(50); // 每次预拉取50条，提升吞吐量
        return containerFactory;
    }

    @Bean("deviceRabbitAdmin")
    public RabbitAdmin deviceRabbitAdmin(@Qualifier("deviceConnectionFactory") ConnectionFactory factory) {
        return new RabbitAdmin(factory);
    }

    private CachingConnectionFactory buildConnectionFactory(RabbitProperties props) {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(props.getHost());
        factory.setPort(props.getPort());
        factory.setUsername(props.getUsername());
        factory.setPassword(props.getPassword());
        factory.setVirtualHost(props.getVirtualHost());
        // 开启发送确认机制（面试高频点：防消息丢失）
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);
        return factory;
    }
}