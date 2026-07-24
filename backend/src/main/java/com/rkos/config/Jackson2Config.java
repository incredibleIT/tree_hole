package com.rkos.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 2.x ObjectMapper 手动注册配置。
 * <p>
 * Spring Boot 4.x 默认使用 Jackson 3.x（{@code tools.jackson}），
 * 不再自动创建 Jackson 2.x（{@code com.fasterxml.jackson}）的 ObjectMapper bean。
 * 本配置类手动注册 Jackson 2.x ObjectMapper，供项目中使用 Jackson 2.x 的组件使用。
 * <p>
 * <b>背景：</b>Spring Boot 4.1.0 GA（2026-06-10）引入了 Jackson 3.x 作为默认 JSON 库，
 * 但 Spring AI 2.0.0、MyBatis-Plus 3.5.16 等第三方库仍依赖 Jackson 2.x。
 */
@Configuration
public class Jackson2Config {

    @Bean
    @ConditionalOnMissingBean(com.fasterxml.jackson.databind.ObjectMapper.class)
    public ObjectMapper jackson2ObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
