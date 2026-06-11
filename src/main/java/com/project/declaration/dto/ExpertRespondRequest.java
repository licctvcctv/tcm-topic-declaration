package com.project.declaration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExpertRespondRequest {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "响应结果不能为空")
    private Integer accept; // 1-接受评审，2-拒绝评审
}
