package com.project.declaration.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PublishResultRequest {
    @NotNull(message = "课题ID不能为空")
    private Long topicId;

    @NotNull(message = "发布结果不能为空")
    @Min(value = 1, message = "发布结果只能为通过或不通过")
    @Max(value = 2, message = "发布结果只能为通过或不通过")
    private Integer finalPass; // 1-立项通过，2-立项不通过

    private String adminOpinion; // 审核意见/批复

    private String announcementContent; // 通过可见的公告通知内容 (如微信二维码、群链接等)
}
