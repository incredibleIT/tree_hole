package com.rkos.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ApiResponse} 单元测试。
 */
class ApiResponseTest {

    @Test
    void success_shouldReturnCorrectFields() {
        Map<String, String> data = Map.of("key", "value");
        ApiResponse<Map<String, String>> response = ApiResponse.success(data);

        assertEquals("SUCCESS", response.getCode());
        assertEquals("操作成功", response.getMessage());
        assertEquals(data, response.getData());
        assertNotNull(response.getTimestamp());
        assertTrue(response.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void success_withNullData_shouldReturnNullData() {
        ApiResponse<Void> response = ApiResponse.success(null);

        assertEquals("SUCCESS", response.getCode());
        assertNull(response.getData());
    }

    @Test
    void error_shouldReturnCorrectFields() {
        Map<String, String> details = Map.of("field", "不能为空");
        ApiResponse<Map<String, String>> response =
                ApiResponse.error("VALIDATION_ERROR", "参数校验失败", details);

        assertEquals("VALIDATION_ERROR", response.getCode());
        assertEquals("参数校验失败", response.getMessage());
        assertEquals(details, response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void error_withNullData_shouldReturnNullData() {
        ApiResponse<Void> response =
                ApiResponse.error("INTERNAL_ERROR", "系统内部错误", null);

        assertEquals("INTERNAL_ERROR", response.getCode());
        assertNull(response.getData());
    }
}
