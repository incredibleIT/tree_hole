package com.rkos.modules.story.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 故事领域模型，映射到 MongoDB `stories` 集合。
 * <p>
 * story_id 为业务唯一标识（UUID），与 PostgreSQL relationship_genomes 表通过 UUID 字符串桥接。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "stories")
public class Story {

    @Id
    private String id;

    @Field("story_id")
    @Indexed(unique = true)
    private String storyId;

    @Field("author_id")
    private String authorId;

    @Field("content")
    private String content;

    @Field("relationship_type")
    private String relationshipType;

    @Field("anonymous")
    private Boolean anonymous;

    @Field("attachments")
    @Builder.Default
    private List<String> attachments = List.of();

    @Field("status")
    @Builder.Default
    private String status = "ACTIVE";

    @Field("version")
    @Builder.Default
    private Integer version = 1;

    @Field("processing_status")
    @Indexed
    @Builder.Default
    private String processingStatus = "PENDING";

    @Field("processing_metadata")
    private ProcessingMetadata processingMetadata;

    @Field("content_length")
    private Integer contentLength;

    @Field("language")
    private String language;

    @Field("created_at")
    @Indexed
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Agent 处理元信息，嵌套文档。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingMetadata {

        @Field("agent_version")
        private String agentVersion;

        @Field("model_used")
        private String modelUsed;

        @Field("started_at")
        private LocalDateTime startedAt;

        @Field("completed_at")
        private LocalDateTime completedAt;

        @Field("retry_count")
        private Integer retryCount;

        @Field("error_message")
        private String errorMessage;
    }
}
