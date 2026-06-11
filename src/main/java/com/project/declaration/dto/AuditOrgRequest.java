package com.project.declaration.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AuditOrgRequest {
    @NotNull(message = "机构ID不能为空")
    private Long orgId;

    @NotNull(message = "审核结果不能为空")
    @Min(value = 1, message = "审核结果只能为通过或拒绝")
    @Max(value = 2, message = "审核结果只能为通过或拒绝")
    private Integer status; // 1-通过，2-拒绝

    private String rejectReason; // 驳回原因
}
