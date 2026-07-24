package com.rkos.modules.story.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 置信度维度模型，描述 Agent 对各维度提取结果的可信度评估。
 * <p>
 * 所有字段取值范围 0.00–1.00，对应数据库 {@code DECIMAL(3,2)}。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Confidence {

    /** 整体置信度 */
    private BigDecimal overall;

    /** 关系维度置信度 */
    private BigDecimal relationship;

    /** 参与者维度置信度 */
    private BigDecimal participants;

    /** 因果链置信度 */
    private BigDecimal causalChain;

    /** 冲突模式置信度 */
    private BigDecimal conflictPatterns;
}
