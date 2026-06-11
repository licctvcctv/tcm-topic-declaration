package com.project.declaration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterExpertRequest {
    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    private String mobile;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "手写签字图片不能为空")
    private String expertSignature; // Signature URL

    @NotNull(message = "主要研究方向不能为空")
    private Long majorDirection;

    private String minorDirections;
}
