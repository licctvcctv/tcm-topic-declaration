package com.project.declaration.controller;

import com.project.declaration.common.Result;
import com.project.declaration.dto.LoginRequest;
import com.project.declaration.dto.RegisterExpertRequest;
import com.project.declaration.dto.RegisterOrgRequest;
import com.project.declaration.dto.RegisterUserRequest;
import com.project.declaration.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private com.project.declaration.service.InstitutionService institutionService;

    @GetMapping("/active-orgs")
    public Result<List<com.project.declaration.entity.Institution>> getActiveOrgs() {
        List<com.project.declaration.entity.Institution> list = institutionService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.project.declaration.entity.Institution>()
                .eq(com.project.declaration.entity.Institution::getStatus, 1)
        );
        return Result.success(list);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> data = authService.login(request);
        return Result.success(data);
    }

    @PostMapping("/register-org")
    public Result<String> registerOrg(@Valid @RequestBody RegisterOrgRequest request) {
        authService.registerOrg(request);
        return Result.success("机构注册成功，请等待超级管理员审批激活");
    }

    @PostMapping("/register-expert")
    public Result<String> registerExpert(@Valid @RequestBody RegisterExpertRequest request) {
        authService.registerExpert(request);
        return Result.success("评审专家注册成功");
    }

    @PostMapping("/register-user")
    public Result<String> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        authService.registerUser(request);
        return Result.success("账号注册成功，请等待所在机构管理员审核批准");
    }
}
