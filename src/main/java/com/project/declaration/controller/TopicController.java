package com.project.declaration.controller;

import com.project.declaration.common.Result;
import com.project.declaration.dto.TopicDeclarationRequest;
import com.project.declaration.entity.DeclarationPeriod;
import com.project.declaration.entity.TopicCategory;
import com.project.declaration.entity.TopicDeclaration;
import com.project.declaration.entity.User;
import com.project.declaration.security.SecurityUtils;
import com.project.declaration.service.DeclarationPeriodService;
import com.project.declaration.service.TopicCategoryService;
import com.project.declaration.service.TopicDeclarationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private TopicDeclarationService topicService;

    @Autowired
    private TopicCategoryService categoryService;

    @Autowired
    private DeclarationPeriodService periodService;

    @PostMapping("/save-submit")
    @PreAuthorize("hasAnyRole('NORMAL_USER', 'ORG_ADMIN')")
    public Result<String> saveOrSubmitTopic(@Valid @RequestBody TopicDeclarationRequest request) {
        User current = securityUtils.getCurrentUser();
        topicService.saveOrSubmitTopic(request, current.getId(), current.getOrgId());
        return Result.success(request.getIsSubmit() == 1 ? "课题提交审核成功" : "草稿保存成功");
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('NORMAL_USER', 'ORG_ADMIN')")
    public Result<List<TopicDeclaration>> getMyTopics() {
        User current = securityUtils.getCurrentUser();
        return Result.success(topicService.listTopicsForCurrentUser(current.getId(), current.getRole(), current.getOrgId()));
    }

    @GetMapping("/detail/{id}")
    public Result<TopicDeclaration> getTopicDetail(@PathVariable Long id) {
        User current = securityUtils.getCurrentUser();
        TopicDeclaration topic = topicService.getTopicDetail(id, current.getId(), current.getRole(), current.getOrgId());
        return Result.success(topic);
    }

    @GetMapping("/categories")
    public Result<List<TopicCategory>> getCategories() {
        return Result.success(categoryService.listActiveCategories());
    }

    @GetMapping("/active-period")
    public Result<DeclarationPeriod> getActivePeriod() {
        return Result.success(periodService.getActivePeriod(null));
    }

    @GetMapping("/is-open")
    public Result<Boolean> isDeclarationOpen() {
        return Result.success(periodService.isDeclarationOpen());
    }
}
