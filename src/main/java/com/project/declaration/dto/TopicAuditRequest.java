package com.project.declaration.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TopicAuditRequest {
    @NotNull(message = "课题ID不能为空")
    private Long topicId;

    @NotNull(message = "审核结果不能为空")
    @Min(value = 0, message = "审核结果只能为通过或驳回")
    @Max(value = 1, message = "审核结果只能为通过或驳回")
    private Integer approve; // 1-通过，0-拒绝/驳回

    private String reason; // 审核意见/退回原因
}
