package com.rkos.modules.story.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 参与者维度模型，描述关系中某一方的特征。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Participant {

    /** 角色（"叙述者"、"对方"） */
    private String role;

    /** 依恋类型（"焦虑型"、"回避型"、"安全型"） */
    private String attachment;

    /** 行为模式列表（如 ["索取确认", "频繁追问"]） */
    private List<String> behaviors;

    /** 主要情绪列表（如 ["焦虑", "不安"]） */
    private List<String> emotions;

    /** 故事发生时的年龄 */
    private Integer ageAtStory;

    /** 性别 */
    private String gender;
}
