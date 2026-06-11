package com.project.declaration.controller;

import com.project.declaration.common.Result;
import com.project.declaration.dto.ExpertRespondRequest;
import com.project.declaration.dto.ExpertReviewOpinionRequest;
import com.project.declaration.entity.ExpertReviewTask;
import com.project.declaration.entity.User;
import com.project.declaration.security.SecurityUtils;
import com.project.declaration.service.ExpertReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expert")
@PreAuthorize("hasRole('EXPERT')")
public class ExpertController {

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private ExpertReviewService expertReviewService;

    @GetMapping("/tasks")
    public Result<List<ExpertReviewTask>> getMyTasks() {
        User current = securityUtils.getCurrentUser();
        return Result.success(expertReviewService.listTasksForExpert(current.getId()));
    }

    @PostMapping("/respond")
    public Result<String> respondInvitation(@Valid @RequestBody ExpertRespondRequest request) {
        User current = securityUtils.getCurrentUser();
        expertReviewService.respondInvitation(request, current.getId());
        return Result.success(request.getAccept() == 1 ? "接受评审任务成功" : "拒绝评审任务，已触发替换流程");
    }

    @PostMapping("/opinion")
    public Result<String> submitOpinion(@Valid @RequestBody ExpertReviewOpinionRequest request) {
        User current = securityUtils.getCurrentUser();
        expertReviewService.submitReviewOpinion(request, current.getId());
        return Result.success(request.getIsSubmit() == 1 ? "评审意见正式提交成功" : "评审意见暂存草稿成功");
    }
}
