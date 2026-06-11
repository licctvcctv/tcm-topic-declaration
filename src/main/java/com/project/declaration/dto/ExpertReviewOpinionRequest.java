package com.project.declaration.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpertReviewOpinionRequest {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "评审打分不能为空")
    @DecimalMin(value = "0.0", message = "打分最低不能低于0分")
    @DecimalMax(value = "100.0", message = "打分最高不能高于100分")
    private BigDecimal score;

    @NotBlank(message = "评审意见内容不能为空")
    private String comments;

    @NotNull(message = "评审结论不能为空")
    @Min(value = 1, message = "评审结论只能为推荐或不推荐")
    @Max(value = 2, message = "评审结论只能为推荐或不推荐")
    private Integer recommendResult; // 1-推荐立项，2-不推荐立项

    @NotNull(message = "保存状态不能为空")
    @Min(value = 0, message = "保存状态只能为暂存或提交")
    @Max(value = 1, message = "保存状态只能为暂存或提交")
    private Integer isSubmit; // 0-暂存草稿，1-正式提交意见
}
