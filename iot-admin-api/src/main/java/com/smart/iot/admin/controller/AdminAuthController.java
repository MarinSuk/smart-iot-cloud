package com.smart.iot.admin.controller;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.iot.admin.entity.AdminUser;
import com.smart.iot.admin.mapper.AdminUserMapper;
import com.smart.iot.common.context.AuthContext;
import com.smart.iot.common.context.AuthContextHolder;
import com.smart.iot.common.exception.BizException;
import com.smart.iot.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Controller handling admin user login (backed by [master] DB) and session inspection.
 * 处理后台管理员登录（基于 [master] 主库校验）与当前会话查询的控制器。
 */
@Tag(name = "Admin - Authentication") // 后台-认证授权接口
@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    @Qualifier("authRedisTemplate")
    private StringRedisTemplate authRedisTemplate;

    /** Redis key prefix matching AdminTokenInterceptor. 与拦截器一致的 Token 键前缀。 */
    private static final String TOKEN_KEY_PREFIX = "auth:admin:token:";

    /**
     * Login request payload DTO.
     * 登录请求参数对象。
     */
    @Data
    public static class LoginRequest {
        @Schema(description = "Admin username", example = "admin") // 管理员账号
        private String username;

        @Schema(description = "Admin password", example = "123456") // 管理员密码
        private String password;
    }

    /**
     * Login response payload DTO.
     * 登录响应数据对象。
     */
    @Data
    public static class LoginResponse {
        @Schema(description = "Bearer access token") // 访问令牌
        private String token;

        @Schema(description = "Display nickname") // 管理员昵称
        private String nickname;
    }

    /**
     * Authenticate admin credentials against [master] DB and issue a Bearer Token in auth Redis.
     * 查询 [master] 主库校验管理员账号密码，并签发存储于鉴权 Redis 中的 Bearer Token。
     *
     * @param req login credentials // 登录请求参数
     * @return Result containing access token 包含访问令牌的响应体
     */
    @Operation(summary = "Admin Login") // 管理员登录
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest req) {
        if (StrUtil.hasBlank(req.getUsername(), req.getPassword())) {
            throw new BizException(400, "Username and password cannot be empty."); // 账号和密码不能为空
        }

        // 1. Query admin user from [master] MySQL database / 从 [master] 主库查询管理员账号
        AdminUser adminUser = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, req.getUsername())
        );

        // 2. Verify user existence, status, and SHA-256 password hash / 校验账号是否存在、是否启用及密码哈希是否匹配
        String inputHash = DigestUtil.sha256Hex(req.getPassword());
        if (adminUser == null || !inputHash.equalsIgnoreCase(adminUser.getPassword())) {
            throw new BizException(401, "Invalid username or password."); // 用户名或密码错误
        }
        if (adminUser.getStatus() == null || adminUser.getStatus() != 1) {
            throw new BizException(403, "Admin account is disabled."); // 该管理员账号已被禁用
        }

        // 3. Generate token and store "userId:nickname" in authRedis / 生成 Token 并存入鉴权专用 Redis
        String token = UUID.fastUUID().toString(true);
        authRedisTemplate.opsForValue().set(
                TOKEN_KEY_PREFIX + token,
                adminUser.getId() + ":" + adminUser.getNickname(),
                2,
                TimeUnit.HOURS
        );

        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setNickname(adminUser.getNickname());
        return Result.success(resp);
    }

    /**
     * Get current authenticated admin profile from ThreadLocal context.
     * 从 ThreadLocal 上下文获取当前已登录管理员信息。
     *
     * @return Result containing AuthContext 包含当前用户上下文的响应体
     */
    @Operation(summary = "Get Current User") // 获取当前登录用户信息
    @GetMapping("/me")
    public Result<AuthContext> getCurrentUser() {
        return Result.success(AuthContextHolder.getContext());
    }
}