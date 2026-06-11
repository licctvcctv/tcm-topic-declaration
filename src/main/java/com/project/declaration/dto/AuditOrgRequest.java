package com.project.declaration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditOrgRequest {
    @NotNull(message = "机构ID不能为空")
    private Long orgId;

    @NotNull(message = "审核结果不能为空")
    private Integer status; // 1-通过，2-拒绝

    private String rejectReason; // 驳回原因
}
