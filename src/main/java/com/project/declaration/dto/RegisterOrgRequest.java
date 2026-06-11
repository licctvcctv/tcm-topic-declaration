package com.project.declaration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterOrgRequest {
    @NotBlank(message = "机构名称不能为空")
    private String orgName;

    @NotBlank(message = "登录账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "联系人不能为空")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    private String mobile;

    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "单位地址不能为空")
    private String address;

    @NotBlank(message = "资质证书材料不能为空")
    private String licenseUrl;
}
