package com.rkos.modules.story.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 结果维度模型，描述关系的最终走向。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Outcome {

    /** 结果类型（"分手"、"和好"、"持续"、"疏远"） */
    private String type;

    /** 发起方（"A"、"B"、"mutual"） */
    private String initiator;

    /** 方式（"direct"、"gradual"、"ambiguous"） */
    private String manner;
}
