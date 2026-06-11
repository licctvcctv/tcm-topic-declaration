package com.project.declaration.controller;

import com.project.declaration.common.Result;
import com.project.declaration.dto.AuditOrgRequest;
import com.project.declaration.dto.ExpertAssignRequest;
import com.project.declaration.dto.PeriodConfigRequest;
import com.project.declaration.dto.PublishResultRequest;
import com.project.declaration.dto.TopicAuditRequest;
import com.project.declaration.entity.DeclarationPeriod;
import com.project.declaration.entity.ExpertReviewTask;
import com.project.declaration.entity.Institution;
import com.project.declaration.entity.TopicCategory;
import com.project.declaration.entity.TopicDeclaration;
import com.project.declaration.entity.User;
import com.project.declaration.security.SecurityUtils;
import com.project.declaration.service.DeclarationPeriodService;
import com.project.declaration.service.ExpertReviewService;
import com.project.declaration.service.InstitutionService;
import com.project.declaration.service.TopicCategoryService;
import com.project.declaration.service.TopicDeclarationService;
import com.project.declaration.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private InstitutionService institutionService;

    @Autowired
    private DeclarationPeriodService periodService;

    @Autowired
    private TopicDeclarationService topicService;

    @Autowired
    private ExpertReviewService expertReviewService;

    @Autowired
    private UserService userService;

    @Autowired
    private TopicCategoryService categoryService;

    @GetMapping("/orgs/pending")
    public Result<List<Institution>> getPendingOrgs() {
        return Result.success(institutionService.listAllPending());
    }

    @GetMapping("/orgs")
    public Result<List<Institution>> listAllOrgs() {
        return Result.success(institutionService.list());
    }

    @PostMapping("/orgs/audit")
    public Result<String> auditOrg(@Valid @RequestBody AuditOrgRequest request) {
        institutionService.auditInstitution(request);
        return Result.success("审核机构资质成功");
    }

    @PostMapping("/orgs/quota")
    public Result<String> adjustQuota(@RequestParam Long orgId, @RequestParam Integer quota) {
        userService.adjustQuota(orgId, quota);
        return Result.success("调整机构申报名额上限成功");
    }

    @PostMapping("/period/config")
    public Result<String> configPeriod(@Valid @RequestBody PeriodConfigRequest request) {
        periodService.saveOrUpdatePeriod(request);
        return Result.success("配置申报周期及注意事项成功");
    }

    @GetMapping("/period")
    public Result<DeclarationPeriod> getPeriod(@RequestParam(required = false) Integer year) {
        return Result.success(periodService.getActivePeriod(year));
    }

    @PostMapping("/topics/audit")
    public Result<String> formatAudit(@Valid @RequestBody TopicAuditRequest request) {
        User current = securityUtils.getCurrentUser();
        topicService.auditBySuperAdmin(request, current.getId());
        return Result.success("格式审核处理成功");
    }

    @GetMapping("/topics")
    public Result<List<TopicDeclaration>> listAllTopics() {
        return Result.success(topicService.list());
    }

    @GetMapping("/experts/recommend")
    public Result<List<User>> recommendExperts(@RequestParam Long topicId) {
        return Result.success(expertReviewService.recommendExpertsForTopic(topicId));
    }

    @PostMapping("/experts/assign")
    public Result<String> assignExperts(@Valid @RequestBody ExpertAssignRequest request) {
        expertReviewService.assignExperts(request);
        return Result.success("专家指派成功");
    }

    @GetMapping("/reviews")
    public Result<List<ExpertReviewTask>> getReviewsForTopic(@RequestParam Long topicId) {
        return Result.success(expertReviewService.listTasksForTopic(topicId));
    }

    @PostMapping("/topics/publish-result")
    public Result<String> publishFinalResult(@Valid @RequestBody PublishResultRequest request) {
        User current = securityUtils.getCurrentUser();
        topicService.publishFinalResult(request, current.getId());
        return Result.success("正式发布最终评审结果成功");
    }

    @PostMapping("/topics/add-second-expert")
    public Result<String> addSecondExpert(@RequestParam Long topicId, @RequestParam Long expertId) {
        topicService.addSecondRoundExpert(topicId, expertId);
        return Result.success("追加第二次评审专家成功");
    }

    @GetMapping("/topics/export-csv")
    public void exportPassedTopics(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"results.csv\"; filename*=UTF-8''" + java.net.URLEncoder.encode("课题评审汇总.csv", "UTF-8"));
        
        response.getWriter().write('\ufeff'); // UTF-8 BOM
        response.getWriter().write("课题名称,研究方向,申报单位,平均得分,推荐人数/总人数,立项通过率,系统建议,最终结论,联系人\n");
        
        List<TopicDeclaration> list = topicService.list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TopicDeclaration>()
                .in(TopicDeclaration::getStatus, 5, 6));
        
        for (TopicDeclaration topic : list) {
            Institution inst = institutionService.getById(topic.getOrgId());
            
            String orgName = inst != null ? inst.getName() : "";
            
            String categoryName = "";
            if (topic.getCategoryId() != null) {
                TopicCategory cat = categoryService.getById(topic.getCategoryId());
                if (cat != null) categoryName = cat.getName();
            }
            
            List<ExpertReviewTask> reviews = expertReviewService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExpertReviewTask>()
                    .eq(ExpertReviewTask::getTopicId, topic.getId()));
            int recCount = (int) reviews.stream().filter(r -> r.getRecommendResult() != null && r.getRecommendResult() == 1 && r.getInvitationStatus() != null && r.getInvitationStatus() == 1).count();
            int validCount = (int) reviews.stream().filter(r -> r.getInvitationStatus() != null && r.getInvitationStatus() == 1).count();
            int passRate = validCount > 0 ? Math.round((recCount * 100f) / validCount) : 0;
            String suggest = passRate >= 50 ? "建议立项" : "建议不予立项";
            
            String finalResult;
            if (topic.getStatus() == 5) finalResult = "评审中";
            else if (topic.getFinalPass() != null && topic.getFinalPass() == 1) finalResult = "立项通过";
            else if (topic.getFinalPass() != null && topic.getFinalPass() == 2) finalResult = "立项不通过";
            else if (topic.getStatus() == 7) finalResult = "格式审核不通过";
            else finalResult = "未发布";
            
            response.getWriter().write(String.format("%s,%s,%s,%s,%s/%s,%s%%,%s,%s,%s\n",
                    csvEscape(topic.getTitle()),
                    csvEscape(categoryName),
                    csvEscape(orgName),
                    topic.getAverageScore() != null ? String.format("%.2f", topic.getAverageScore()) : "0.00",
                    recCount, validCount,
                    passRate,
                    suggest,
                    finalResult,
                    csvEscape(topic.getContactMobile() != null ? topic.getContactMobile() : "")
            ));
        }
        response.getWriter().flush();
    }
    
    private String csvEscape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
