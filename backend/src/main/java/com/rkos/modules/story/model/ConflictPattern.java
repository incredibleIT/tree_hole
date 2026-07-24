package com.rkos.modules.story.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 冲突模式模型，描述关系中反复出现的冲突类型与解决方式。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictPattern {

    /** 冲突类型（"communication"、"emotional_needs"、"values" 等） */
    private String type;

    /** 频率（"recurring"、"occasional"、"one-time"） */
    private String frequency;

    /** 解决方式（"escalation"、"unresolved"、"compromise"） */
    private String resolution;

    /** 冲突详细描述 */
    private String description;
}
