package com.rkos.modules.auth.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API Key 实体模型，映射 PostgreSQL {@code api_keys} 表。
 * <p>
 * 存储 SHA-256 哈希后的 Key，不存储明文。
 * 支持 Key 禁用（{@link #isActive}）和过期（{@link #expiresAt}）管理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("api_keys")
public class ApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** SHA-256 哈希后的 Key（64 位十六进制字符串） */
    private String keyHash;

    /** Key 名称/描述 */
    private String name;

    /** 是否激活（禁用后即使 Key 正确也无法通过认证） */
    private Boolean isActive;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 过期时间（null 表示永不过期） */
    private LocalDateTime expiresAt;
}
