package com.project.declaration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopicDeclarationRequest {
    private Long id; // If updating

    @NotBlank(message = "课题名称不能为空")
    private String title;

    @NotNull(message = "主要研究方向（分类）不能为空")
    private Long categoryId;

    private String secondaryCategories; // Comma-separated category IDs

    @NotBlank(message = "联系手机号不能为空")
    private String contactMobile;

    @NotBlank(message = "任务书文件路径不能为空")
    private String taskBookUrl;

    @NotBlank(message = "活页文件路径不能为空")
    private String anonymousPageUrl;

    @NotNull(message = "保存状态不能为空")
    private Integer isSubmit; // 0-保存草稿，1-正式提交
}
