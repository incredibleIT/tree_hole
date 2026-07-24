package com.rkos.modules.auth;

import com.rkos.modules.auth.mapper.ApiKeyMapper;
import com.rkos.modules.auth.model.ApiKey;
import com.rkos.modules.auth.service.ApiKeyAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * {@link ApiKeyAuthService} 单元测试。
 * <p>
 * Mock {@link ApiKeyMapper} 验证认证逻辑：
 * SHA-256 哈希计算、isActive 校验、expiresAt 校验。
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyAuthServiceTest {

    @Mock
    private ApiKeyMapper apiKeyMapper;

    @InjectMocks
    private ApiKeyAuthService apiKeyAuthService;

    private static final String RAW_KEY = "test-api-key-abc";
    private static final String HASHED_KEY = ApiKeyAuthService.hashKey(RAW_KEY);

    private ApiKey validApiKey;

    @BeforeEach
    void setUp() {
        validApiKey = ApiKey.builder()
                .id(1L)
                .keyHash(HASHED_KEY)
                .name("Test Key")
                .isActive(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .expiresAt(null) // 永不过期
                .build();
    }

    @Test
    void hashKey_shouldReturnConsistentSha256() {
        String hash1 = ApiKeyAuthService.hashKey("dev-api-key-12345");
        String hash2 = ApiKeyAuthService.hashKey("dev-api-key-12345");

        assertEquals(hash1, hash2, "相同输入的哈希值应一致");
        assertEquals(64, hash1.length(), "SHA-256 哈希应为 64 位十六进制");
        assertEquals("8264dc9f07e749d9c2ffead0b25de8cb22bed7af774e189ef224ae015908776b", hash1,
                "dev-api-key-12345 的哈希值应与 V2 迁移脚本一致");
    }

    @Test
    void validate_validKey_shouldReturnTrue() {
        when(apiKeyMapper.selectByKeyHash(HASHED_KEY)).thenReturn(validApiKey);

        assertTrue(apiKeyAuthService.validate(RAW_KEY));
    }

    @Test
    void validate_nullKey_shouldReturnFalse() {
        assertFalse(apiKeyAuthService.validate(null));
        verifyNoInteractions(apiKeyMapper);
    }

    @Test
    void validate_blankKey_shouldReturnFalse() {
        assertFalse(apiKeyAuthService.validate("   "));
        verifyNoInteractions(apiKeyMapper);
    }

    @Test
    void validate_keyNotFoundInDb_shouldReturnFalse() {
        when(apiKeyMapper.selectByKeyHash(anyString())).thenReturn(null);

        assertFalse(apiKeyAuthService.validate("unknown-key"));
    }

    @Test
    void validate_inactiveKey_shouldReturnFalse() {
        validApiKey.setIsActive(false);
        when(apiKeyMapper.selectByKeyHash(HASHED_KEY)).thenReturn(validApiKey);

        assertFalse(apiKeyAuthService.validate(RAW_KEY));
    }

    @Test
    void validate_nullIsActive_shouldReturnFalse() {
        validApiKey.setIsActive(null);
        when(apiKeyMapper.selectByKeyHash(HASHED_KEY)).thenReturn(validApiKey);

        assertFalse(apiKeyAuthService.validate(RAW_KEY));
    }

    @Test
    void validate_expiredKey_shouldReturnFalse() {
        validApiKey.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(apiKeyMapper.selectByKeyHash(HASHED_KEY)).thenReturn(validApiKey);

        assertFalse(apiKeyAuthService.validate(RAW_KEY));
    }

    @Test
    void validate_notYetExpiredKey_shouldReturnTrue() {
        validApiKey.setExpiresAt(LocalDateTime.now().plusDays(30));
        when(apiKeyMapper.selectByKeyHash(HASHED_KEY)).thenReturn(validApiKey);

        assertTrue(apiKeyAuthService.validate(RAW_KEY));
    }
}
