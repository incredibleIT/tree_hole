package com.rkos.modules.story.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rkos.modules.story.model.RelationshipGenome;
import org.apache.ibatis.annotations.Mapper;

/**
 * 关系基因组 MyBatis-Plus Mapper。
 * <p>
 * 继承 {@link BaseMapper} 提供基础 CRUD，
 * 额外定义 {@link #selectByStoryId(String)} 用于按 Story ID 查询，
 * 以及 {@link #upsertByStoryId(RelationshipGenome)} 用于重新处理场景的覆盖写入。
 */
@Mapper
public interface GenomeMapper extends BaseMapper<RelationshipGenome> {

    /**
     * 根据 Story ID 查询关系基因组。
     *
     * @param storyId Story ID（UUID）
     * @return 匹配的 RelationshipGenome 记录，不存在则返回 null
     */
    default RelationshipGenome selectByStoryId(String storyId) {
        return selectOne(new LambdaQueryWrapper<RelationshipGenome>()
                .eq(RelationshipGenome::getStoryId, storyId));
    }

    /**
     * 按 storyId 覆盖写入 Genome（重新处理场景）。
     * <p>
     * 存在旧记录时先删后插，不存在时直接插入。
     * 不使用 PostgreSQL ON CONFLICT 语法，通过应用层实现保证 MyBatis-Plus 兼容性。
     *
     * @param genome 待写入的关系基因组（storyId 必须已设置）
     */
    default void upsertByStoryId(RelationshipGenome genome) {
        RelationshipGenome existing = selectByStoryId(genome.getStoryId());
        if (existing != null) {
            deleteById(existing.getId());
        }
        insert(genome);
    }
}
