package com.rkos.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RKOS 自定义业务异常。
 * <p>
 * 所有业务逻辑异常应使用此类抛出，由 {@link GlobalExceptionHandler} 统一捕获并转换为
 * {@link ApiResponse} 格式返回。errorCode 用于映射 HTTP 状态码和前端错误处理。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RkosException extends RuntimeException {

    private String errorCode;
    private Object details;

    public RkosException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RkosException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public RkosException(String errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
}
