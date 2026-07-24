package com.rkos.modules.story.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关系维度模型，描述两人关系的性质与阶段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Relationship {

    /** 关系类型（情侣、友谊、家庭、同事等） */
    private String type;

    /** 持续时间（如 "3年"） */
    private String duration;

    /** 当前阶段（如 "冷淡期"、"热恋期"） */
    private String stage;

    /** 起始背景（如 "大学校园"） */
    private String startContext;
}
