package com.rkos.modules.auth.service;

import com.rkos.modules.auth.mapper.ApiKeyMapper;
import com.rkos.modules.auth.model.ApiKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * API Key 认证服务。
 * <p>
 * 负责对传入的明文 API Key 进行 SHA-256 哈希后与数据库记录比对，
 * 并校验 {@code is_active} 和 {@code expires_at} 状态。
 */
@Service
@RequiredArgsConstructor
public class ApiKeyAuthService {

    private final ApiKeyMapper apiKeyMapper;

    /**
     * 验证明文 API Key 是否有效。
     * <p>
     * 流程：SHA-256 哈希 → 数据库查询 → 校验 isActive → 校验 expiresAt
     *
     * @param rawKey 明文 API Key
     * @return true=有效，false=无效（不存在/已禁用/已过期）
     */
    public boolean validate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return false;
        }

        String hash = hashKey(rawKey);
        ApiKey apiKey = apiKeyMapper.selectByKeyHash(hash);

        if (apiKey == null) {
            return false;
        }

        // 校验是否激活
        if (apiKey.getIsActive() == null || !apiKey.getIsActive()) {
            return false;
        }

        // 校验是否过期（null 表示永不过期）
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        return true;
    }

    /**
     * 计算明文 Key 的 SHA-256 哈希值。
     *
     * @param rawKey 明文 Key
     * @return 64 位十六进制哈希字符串
     */
    public static String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }
}
