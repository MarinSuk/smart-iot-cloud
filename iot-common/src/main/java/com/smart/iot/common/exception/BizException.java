package com.smart.iot.common.exception;

import lombok.Getter;

/**
 * Custom business exception for controlled error handling.
 * 自定义业务异常类，用于可控的业务错误抛出。
 */
@Getter
public class BizException extends RuntimeException {

    /** HTTP or business error code. 错误状态码。 */
    private final Integer code;

    /**
     * Construct a business exception with code and message.
     * 构造带状态码和提示信息的业务异常。
     *
     * @param code    error code 错误码
     * @param message error description 错误描述
     */
    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}