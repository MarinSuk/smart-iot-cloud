package com.smart.iot.openapi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap class for the Third-Party OpenAPI Gateway service.
 * 第三方开放平台网关服务启动类。
 */
@SpringBootApplication(scanBasePackages = "com.smart.iot")
@MapperScan("com.smart.iot.**.mapper")
public class OpenApiApplication {

    /**
     * Application main entry point.
     * 应用程序主入口方法。
     *
     * @param args command line arguments 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(OpenApiApplication.class, args);
    }
}