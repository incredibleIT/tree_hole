package com.rkos.modules.story.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 情感弧线模型，描述关系中主导情绪及其演变轨迹。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionalArc {

    /** 主导情绪列表（如 ["遗憾", "不舍"]） */
    private List<String> dominantEmotions;

    /** 情感轨迹（"decline"、"rise"、"fluctuate"、"stable"） */
    private String trajectory;
}
