package com.project.declaration.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AuditUserRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "审核结果不能为空")
    @Min(value = 1, message = "审核结果只能为通过或禁用")
    @Max(value = 2, message = "审核结果只能为通过或禁用")
    private Integer status; // 1-通过，2-禁用/拒绝
}
