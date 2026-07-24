package com.rkos.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * <p>
 * 拦截所有 Controller 层异常，统一转换为 {@link ApiResponse} 格式返回。
 * 禁止向客户端暴露堆栈信息，仅返回友好的错误描述。
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 参数校验异常（Bean Validation 失败时触发）。
     *
     * @param ex 校验异常
     * @return 400 Bad Request + 字段级错误明细
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing
                ));
        log.warn("参数校验失败: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR", "参数校验失败", errors));
    }

    /**
     * 请求体解析异常（非法 JSON、类型不匹配等）。
     *
     * @param ex 解析异常
     * @return 400 Bad Request + 友好错误消息
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("BAD_REQUEST", "请求体格式错误", null));
    }

    /**
     * 自定义业务异常。
     *
     * @param ex 业务异常
     * @return 根据 errorCode 映射的 HTTP 状态码 + 错误信息
     */
    @ExceptionHandler(RkosException.class)
    public ResponseEntity<ApiResponse<Object>> handleRkosException(RkosException ex) {
        log.warn("业务异常 [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(getHttpStatus(ex.getErrorCode()))
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage(), ex.getDetails()));
    }

    /**
     * 未捕获的通用异常（兜底处理）。
     *
     * @param ex 未处理异常
     * @return 500 Internal Server Error + 友好消息
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("未捕获的异常", ex);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "系统内部错误", null));
    }

    /**
     * 根据 errorCode 映射 HTTP 状态码。
     */
    private HttpStatus getHttpStatus(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (errorCode) {
            case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
            case "CONFLICT" -> HttpStatus.CONFLICT;
            case "LLM_QUOTA_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "LLM_CALL_FAILED" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
