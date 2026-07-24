package com.rkos;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RKOS（关系知识操作系统）后端启动类。
 * <p>
 * Story 1.2 已启用双数据库自动配置（MongoDB + PostgreSQL），
 * 移除了 Story 1.1 阶段的数据库排除策略。
 */
@SpringBootApplication
@MapperScan("com.rkos.modules.*.mapper")
public class RkosApplication {

    public static void main(String[] args) {
        SpringApplication.run(RkosApplication.class, args);
    }
}
