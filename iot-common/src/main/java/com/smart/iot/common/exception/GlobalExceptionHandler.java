package com.smart.iot.common.exception;

import com.smart.iot.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler to intercept and format exceptions.
 * 全局异常处理器，统一拦截并格式化异常输出。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle custom business exceptions.
     * 处理自定义业务异常。
     *
     * @param e BizException instance 业务异常实例
     * @return formatted Result 格式化后的响应结果
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        // 发生业务异常
        log.warn("Business exception occurred: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * Handle unexpected system exceptions.
     * 处理未预期的系统级异常。
     *
     * @param e Exception instance 异常实例
     * @return formatted Result 格式化后的响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        // 发生系统异常:
        log.error("System error occurred: ", e);
        // 服务器内部错误。
        return Result.fail(500, "Internal server error.");
    }
}