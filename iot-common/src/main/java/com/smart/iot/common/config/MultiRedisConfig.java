package com.smart.iot.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class MultiRedisConfig {

    /**
     * Primary Redis properties for authentication Redis.
     * 鉴权专用 Redis 的主配置属性（添加 @Primary 避免自动装配冲突）。
     *
     * @return RedisProperties instance 属性对象
     */
    @Primary
    @Bean("authRedisProps")
    @ConfigurationProperties(prefix = "spring.custom-redis.auth")
    public RedisProperties authRedisProps() {
        return new RedisProperties();
    }

    @Bean("bizRedisProps")
    @ConfigurationProperties(prefix = "spring.custom-redis.biz")
    public RedisProperties bizRedisProps() {
        return new RedisProperties();
    }

    @Primary
    @Bean("authRedisFactory")
    public LettuceConnectionFactory authRedisFactory(@Qualifier("authRedisProps") RedisProperties props) {
        return createFactory(props);
    }

    @Bean("bizRedisFactory")
    public LettuceConnectionFactory bizRedisFactory(@Qualifier("bizRedisProps") RedisProperties props) {
        return createFactory(props);
    }

    @Primary
    @Bean("authRedisTemplate")
    public StringRedisTemplate authRedisTemplate(@Qualifier("authRedisFactory") LettuceConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean("bizRedisTemplate")
    public StringRedisTemplate bizRedisTemplate(@Qualifier("bizRedisFactory") LettuceConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    private LettuceConnectionFactory createFactory(RedisProperties props) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(props.getHost());
        config.setPort(props.getPort());
        config.setDatabase(props.getDatabase());
        if (props.getPassword() != null) {
            config.setPassword(props.getPassword());
        }
        return new LettuceConnectionFactory(config);
    }
}