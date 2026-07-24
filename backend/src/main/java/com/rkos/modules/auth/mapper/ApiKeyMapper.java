package com.rkos.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rkos.modules.auth.model.ApiKey;
import org.apache.ibatis.annotations.Mapper;

/**
 * API Key MyBatis-Plus Mapper。
 * <p>
 * 继承 {@link BaseMapper} 提供基础 CRUD，
 * 额外定义 {@link #selectByKeyHash(String)} 用于认证查询。
 */
@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {

    /**
     * 根据 SHA-256 哈希值查询 API Key 记录。
     *
     * @param keyHash SHA-256 哈希字符串（64 位十六进制）
     * @return 匹配的 ApiKey 记录，不存在则返回 null
     */
    default ApiKey selectByKeyHash(String keyHash) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getKeyHash, keyHash));
    }
}
