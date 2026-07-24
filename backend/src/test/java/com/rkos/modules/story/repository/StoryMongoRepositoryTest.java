package com.rkos.modules.story.repository;

import com.rkos.modules.story.model.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * StoryMongoRepository 集成测试。
 * <p>
 * 使用 @DataMongoTest + 嵌入式 MongoDB，验证 CRUD 操作、索引约束和自定义查询方法。
 */
@DataMongoTest
@Import(MongoTestMapperFixConfig.class)
class StoryMongoRepositoryTest {

    @Autowired
    private StoryMongoRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void save_shouldPersistStory() {
        Story story = buildStory("save-test");

        Story saved = repository.save(story);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStoryId()).isEqualTo("save-test");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void save_shouldEnforceStoryIdUniqueIndex() {
        repository.save(buildStory("dup-id"));

        Story duplicate = buildStory("dup-id");

        assertThatThrownBy(() -> repository.save(duplicate))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void findByStoryId_shouldReturnExistingStory() {
        repository.save(buildStory("find-test"));

        Optional<Story> found = repository.findByStoryId("find-test");

        assertThat(found).isPresent();
        assertThat(found.get().getContent()).isEqualTo("故事正文内容");
    }

    @Test
    void findByStoryId_shouldReturnEmptyForNonExisting() {
        Optional<Story> found = repository.findByStoryId("non-existing");

        assertThat(found).isEmpty();
    }

    @Test
    void findByProcessingStatus_shouldReturnMatchingStories() {
        repository.save(buildStory("s1"));
        repository.save(buildStory("s2"));
        Story completed = buildStory("s3");
        completed.setProcessingStatus("COMPLETED");
        repository.save(completed);

        List<Story> pending = repository.findByProcessingStatus("PENDING");
        List<Story> completedList = repository.findByProcessingStatus("COMPLETED");

        assertThat(pending).hasSize(2);
        assertThat(completedList).hasSize(1);
        assertThat(completedList.get(0).getStoryId()).isEqualTo("s3");
    }

    @Test
    void update_shouldModifyExistingStory() {
        Story saved = repository.save(buildStory("update-test"));
        saved.setContent("更新后的故事内容");
        saved.setContentLength("更新后的故事内容".length());

        repository.save(saved);

        Story updated = repository.findByStoryId("update-test").orElseThrow();
        assertThat(updated.getContent()).isEqualTo("更新后的故事内容");
        assertThat(updated.getContentLength()).isEqualTo("更新后的故事内容".length());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void delete_shouldRemoveStory() {
        Story saved = repository.save(buildStory("delete-test"));
        assertThat(repository.count()).isEqualTo(1);

        repository.deleteById(saved.getId());

        assertThat(repository.count()).isZero();
        assertThat(repository.findByStoryId("delete-test")).isEmpty();
    }

    @Test
    void save_shouldPersistProcessingMetadata() {
        Story story = buildStory("meta-test");
        story.setProcessingMetadata(Story.ProcessingMetadata.builder()
                .agentVersion("v1.0")
                .modelUsed("qwen-max")
                .startedAt(LocalDateTime.of(2026, 7, 15, 10, 21, 5))
                .completedAt(LocalDateTime.of(2026, 7, 15, 10, 21, 42))
                .retryCount(0)
                .errorMessage(null)
                .build());

        repository.save(story);

        Story found = repository.findByStoryId("meta-test").orElseThrow();
        assertThat(found.getProcessingMetadata()).isNotNull();
        assertThat(found.getProcessingMetadata().getAgentVersion()).isEqualTo("v1.0");
        assertThat(found.getProcessingMetadata().getModelUsed()).isEqualTo("qwen-max");
        assertThat(found.getProcessingMetadata().getRetryCount()).isZero();
    }

    private Story buildStory(String storyId) {
        return Story.builder()
                .storyId(storyId)
                .authorId("anonymous")
                .content("故事正文内容")
                .relationshipType("情侣")
                .anonymous(true)
                .contentLength("故事正文内容".length())
                .language("zh-CN")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
