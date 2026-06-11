package com.project.declaration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditUserRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "审核结果不能为空")
    private Integer status; // 1-通过，2-禁用/拒绝
}
