package com.project.declaration.controller;

import com.project.declaration.common.Result;
import com.project.declaration.dto.AssignAuthRequest;
import com.project.declaration.dto.AuditUserRequest;
import com.project.declaration.dto.TopicAuditRequest;
import com.project.declaration.entity.TopicDeclaration;
import com.project.declaration.entity.User;
import com.project.declaration.security.SecurityUtils;
import com.project.declaration.service.TopicDeclarationService;
import com.project.declaration.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/institution")
@PreAuthorize("hasRole('ORG_ADMIN')")
public class InstitutionController {

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private UserService userService;

    @Autowired
    private TopicDeclarationService topicService;

    @GetMapping("/users")
    public Result<List<User>> getInstitutionUsers() {
        User current = securityUtils.getCurrentUser();
        return Result.success(userService.listInstitutionUsers(current.getOrgId()));
    }

    @PostMapping("/users/audit")
    public Result<String> auditUser(@Valid @RequestBody AuditUserRequest request) {
        User current = securityUtils.getCurrentUser();
        userService.auditUser(request, current.getOrgId());
        return Result.success("机构员工审核成功");
    }

    @PostMapping("/users/assign-auth")
    public Result<String> assignAuth(@Valid @RequestBody AssignAuthRequest request) {
        User current = securityUtils.getCurrentUser();
        userService.assignDeclarationAuth(request, current.getOrgId());
        return Result.success("指派账号申报权限成功");
    }

    @GetMapping("/topics")
    public Result<List<TopicDeclaration>> listInstitutionTopics() {
        User current = securityUtils.getCurrentUser();
        return Result.success(topicService.listTopicsForCurrentUser(current.getId(), current.getRole(), current.getOrgId()));
    }

    @PostMapping("/topics/audit")
    public Result<String> auditTopic(@Valid @RequestBody TopicAuditRequest request) {
        User current = securityUtils.getCurrentUser();
        topicService.auditByOrg(request, current.getId(), current.getOrgId());
        return Result.success("审核机构课题申报成功");
    }
}
