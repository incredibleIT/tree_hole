package com.rkos.modules.story.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 故事提交请求 DTO。
 */
@Data
@Schema(description = "故事提交请求")
public class StoryRequest {

    @Schema(description = "故事内容", example = "我和她是在图书馆认识的...", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "故事内容不能为空")
    @Size(max = 10000, message = "故事内容不能超过 10000 字")
    private String content;

    @Schema(description = "关系类型（可选）", example = "爱情")
    private String relationshipType;

    @Schema(description = "是否匿名提交（可选，默认 false）", example = "false")
    private Boolean anonymous;
}
