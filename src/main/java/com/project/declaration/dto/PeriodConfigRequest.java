package com.project.declaration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PeriodConfigRequest {
    @NotNull(message = "年份不能为空")
    private Integer year;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    @NotNull(message = "状态不能为空")
    private Integer status; // 0-关闭，1-启用

    private String instructions; // 填报注意事项内容
}
