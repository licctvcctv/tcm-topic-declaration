package com.project.declaration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignAuthRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "授权状态不能为空")
    private Integer hasAuth; // 0-取消授权，1-赋予授权
}
