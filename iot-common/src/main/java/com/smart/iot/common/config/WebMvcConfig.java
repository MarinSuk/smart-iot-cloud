package com.smart.iot.common.config;

import com.smart.iot.common.interceptor.AdminTokenInterceptor;
import com.smart.iot.common.interceptor.OpenApiSignInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration registering dual-track authentication interceptors.
 * Web MVC 配置类，用于注册双轨鉴权拦截器。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private AdminTokenInterceptor adminTokenInterceptor;

    @Resource
    private OpenApiSignInterceptor openApiSignInterceptor;

    /**
     * Register admin and open-api interceptors with specific URL patterns.
     * 注册后台管理拦截器与开放平台拦截器，并指定各自的路由匹配规则。
     *
     * @param registry interceptor registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Track 1: Admin management endpoints (exclude login and Swagger/Knife4j docs)
        // 轨道一：后台管理端接口（排除登录接口及 Knife4j 接口文档路径）
        registry.addInterceptor(adminTokenInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/auth/login", "/doc.html", "/webjars/**", "/v3/api-docs/**");

        // Track 2: Third-party partner OpenAPI endpoints
        // 轨道二：第三方合作平台开放接口
        registry.addInterceptor(openApiSignInterceptor)
                .addPathPatterns("/open/**");
    }
}