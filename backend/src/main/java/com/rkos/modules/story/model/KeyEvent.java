package com.rkos.modules.story.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关键事件模型，描述关系演进中的标志性事件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyEvent {

    /** 事件名称（如 "工作压力增加"） */
    private String event;

    /** 事件在故事中的位置（"beginning"、"climax"、"end"） */
    private String position;

    /** 事件详细描述 */
    private String description;
}
