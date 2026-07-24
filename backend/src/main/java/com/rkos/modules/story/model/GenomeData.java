package com.rkos.modules.story.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Genome 数据嵌套结构，映射 {@code relationship_genomes.genome_data} JSONB 字段内部内容。
 * <p>
 * 包含 9 个维度：relationship、participants、keyEvents、causalChain、
 * conflictPatterns、outcome、lessons、confidence、emotionalArc。
 * <p>
 * 本类不是独立数据库实体，仅作为 JSONB 序列化/反序列化的 POJO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenomeData {

    /** Genome 唯一标识（UUID） */
    private String genomeId;

    /** 关联的 Story ID */
    private String storyId;

    /** Genome Schema 版本 */
    private String version;

    /** 关系维度 */
    private Relationship relationship;

    /** 参与者维度（"A" → Participant, "B" → Participant） */
    private Map<String, Participant> participants;

    /** 关键事件列表 */
    private List<KeyEvent> keyEvents;

    /** 因果链（事件字符串列表） */
    private List<String> causalChain;

    /** 冲突模式列表 */
    private List<ConflictPattern> conflictPatterns;

    /** 结果维度 */
    private Outcome outcome;

    /** 经验教训列表 */
    private List<String> lessons;

    /** 置信度维度 */
    private Confidence confidence;

    /** 情感弧线维度 */
    private EmotionalArc emotionalArc;
}
