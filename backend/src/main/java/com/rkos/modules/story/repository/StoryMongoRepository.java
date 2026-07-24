package com.rkos.modules.story.repository;

import com.rkos.modules.story.model.Story;
import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 故事数据 MongoDB 访问接口。
 */
public interface StoryMongoRepository extends MongoRepository<Story, String> {

    /**
     * 按业务标识查询故事。
     *
     * @param storyId 业务唯一标识（UUID）
     * @return 故事（可选）
     */
    Optional<Story> findByStoryId(String storyId);

    /**
     * 按 Agent 处理状态查询故事列表。
     *
     * @param processingStatus 处理状态
     * @return 故事列表
     */
    List<Story> findByProcessingStatus(String processingStatus);

    /**
     * 按关系类型 + 处理状态分页查询。
     */
    Page<Story> findByRelationshipTypeAndProcessingStatus(
            String relationshipType, String processingStatus, Pageable pageable);

    /**
     * 按关系类型分页查询。
     */
    Page<Story> findByRelationshipType(String relationshipType, Pageable pageable);

    /**
     * 按处理状态分页查询。
     */
    Page<Story> findByProcessingStatus(String processingStatus, Pageable pageable);
}
