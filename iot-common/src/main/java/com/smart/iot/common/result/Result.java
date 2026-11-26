package com.smart.iot.common.result;

import lombok.Data;

/**
 * Unified API response wrapper.
 * 统一接口响应包装类。
 */
@Data
public class Result<T> {

    /** Response status code (200 means success). 响应状态码（200表示成功）。 */
    private Integer code;

    /** Response message. 响应提示信息。 */
    private String message;

    /** Response payload data. 响应业务数据。 */
    private T data;

    /**
     * Build a successful response with data.
     * 构建带数据的成功响应。
     *
     * @param data payload data 业务数据
     * @return Result instance 响应对象
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("Success.");
        result.setData(data);
        return result;
    }

    /**
     * Build a failed response with code and message.
     * 构建带状态码和错误信息的失败响应。
     *
     * @param code    error code 错误码
     * @param message error message 错误信息
     * @return Result instance 响应对象
     */
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}