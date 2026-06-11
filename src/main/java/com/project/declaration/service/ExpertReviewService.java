package com.project.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.declaration.dto.ExpertAssignRequest;
import com.project.declaration.dto.ExpertRespondRequest;
import com.project.declaration.dto.ExpertReviewOpinionRequest;
import com.project.declaration.entity.ExpertReviewTask;
import com.project.declaration.entity.User;

import java.util.List;

public interface ExpertReviewService extends IService<ExpertReviewTask> {
    List<User> recommendExpertsForTopic(Long topicId);
    void assignExperts(ExpertAssignRequest request);
    void respondInvitation(ExpertRespondRequest request, Long currentExpertId);
    void submitReviewOpinion(ExpertReviewOpinionRequest request, Long currentExpertId);
    List<ExpertReviewTask> listTasksForExpert(Long expertId);
    List<ExpertReviewTask> listTasksForTopic(Long topicId);
}
