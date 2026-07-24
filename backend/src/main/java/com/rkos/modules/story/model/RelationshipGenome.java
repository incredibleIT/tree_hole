package com.rkos.modules.story.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rkos.config.JsonbTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 关系基因组主实体，映射 PostgreSQL {@code relationship_genomes} 表。
 * <p>
 * {@code genome_data} JSONB 字段通过自定义 {@link JsonbTypeHandler} 序列化/反序列化，
 * {@code autoResultMap = true} 是 TypeHandler 正常工作的必要条件。
 * <p>
 * 扁平化列（{@link #relationshipType}、{@link #outcomeType}、{@link #overallConfidence}）
 * 与 {@code genomeData} 内部字段保持同步，用于高频查询。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "relationship_genomes", autoResultMap = true)
public class RelationshipGenome {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 Story ID（UUID，对应 MongoDB Story._id） */
    private String storyId;

    /** Agent 版本号 */
    private String agentVersion;

    /** JSONB 字段 — 使用自定义 TypeHandler 序列化 */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private GenomeData genomeData;

    /** 扁平化列 — 整体置信度（与 genomeData.confidence.overall 同步） */
    private BigDecimal overallConfidence;

    /** 扁平化列 — 关系类型（与 genomeData.relationship.type 同步） */
    private String relationshipType;

    /** 扁平化列 — 结果类型（与 genomeData.outcome.type 同步） */
    private String outcomeType;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
