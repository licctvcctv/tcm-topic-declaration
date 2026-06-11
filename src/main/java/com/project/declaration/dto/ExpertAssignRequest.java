package com.project.declaration.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ExpertAssignRequest {
    @NotNull(message = "课题ID不能为空")
    private Long topicId;

    @NotNull(message = "必须指定评审专家")
    @Size(min = 3, max = 3, message = "每个课题必须分配且只能分配3名评审专家")
    private List<Long> expertIds;
}
