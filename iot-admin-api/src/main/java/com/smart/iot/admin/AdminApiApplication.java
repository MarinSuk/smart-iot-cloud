package com.smart.iot.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap class for the Enterprise Admin Management service.
 * 企业后台管理系统服务启动类。
 */
@SpringBootApplication(scanBasePackages = "com.smart.iot")
@MapperScan("com.smart.iot.**.mapper")
public class AdminApiApplication {

    /**
     * Application main entry point.
     * 应用程序主入口方法。
     *
     * @param args command line arguments 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AdminApiApplication.class, args);
    }
}