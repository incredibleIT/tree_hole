package com.rkos.modules.story.seed;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rkos.modules.story.model.RelationshipGenome;
import com.rkos.modules.story.model.Story;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * 种子数据加载器（测试工具类）。
 * <p>
 * 从 classpath 读取 JSON 种子数据文件，反序列化为 Java 对象列表。
 * 仅用于测试环境，不依赖 Spring 上下文。
 * <p>
 * 使用 Jackson 2.x {@link ObjectMapper}（与项目一致：{@code com.fasterxml.jackson}），
 * 注册 {@link JavaTimeModule} 以支持 {@link java.time.LocalDateTime} 序列化/反序列化。
 */
public final class SeedDataLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private SeedDataLoader() {
    }

    /**
     * 从 classpath {@code seed/seed-genomes.json} 读取并反序列化为 Genome 列表。
     *
     * @return 5 条 RelationshipGenome 种子数据
     * @throws IllegalStateException  文件不存在时
     * @throws UncheckedIOException   读取/解析失败时
     */
    public static List<RelationshipGenome> loadGenomes() {
        return loadList("seed/seed-genomes.json", RelationshipGenome.class);
    }

    /**
     * 从 classpath {@code seed/seed-stories.json} 读取并反序列化为 Story 列表。
     *
     * @return 5 条 Story 种子数据
     * @throws IllegalStateException  文件不存在时
     * @throws UncheckedIOException   读取/解析失败时
     */
    public static List<Story> loadStories() {
        return loadList("seed/seed-stories.json", Story.class);
    }

    private static <T> List<T> loadList(String resourcePath, Class<T> type) {
        try (InputStream is = SeedDataLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("种子数据文件不存在: " + resourcePath);
            }
            return MAPPER.readValue(is,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new UncheckedIOException("种子数据加载失败: " + resourcePath, e);
        }
    }
}
