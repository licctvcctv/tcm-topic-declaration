package com.project.declaration.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ExpertRespondRequest {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "响应结果不能为空")
    @Min(value = 1, message = "响应结果只能为接受或拒绝")
    @Max(value = 2, message = "响应结果只能为接受或拒绝")
    private Integer accept; // 1-接受评审，2-拒绝评审
}
