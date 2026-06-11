package com.project.declaration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopicAuditRequest {
    @NotNull(message = "课题ID不能为空")
    private Long topicId;

    @NotNull(message = "审核结果不能为空")
    private Integer approve; // 1-通过，0-拒绝/驳回

    private String reason; // 审核意见/退回原因
}
