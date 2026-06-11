package com.project.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.declaration.dto.TopicAuditRequest;
import com.project.declaration.dto.TopicDeclarationRequest;
import com.project.declaration.entity.TopicDeclaration;

import java.util.List;

public interface TopicDeclarationService extends IService<TopicDeclaration> {
    void saveOrSubmitTopic(TopicDeclarationRequest request, Long declarerId, Long orgId);
    void auditByOrg(TopicAuditRequest request, Long auditorId, Long orgId);
    void auditBySuperAdmin(TopicAuditRequest request, Long auditorId);
    TopicDeclaration getTopicDetail(Long id, Long currentUserId, String currentRole, Long currentOrgId);
    List<TopicDeclaration> listTopicsForCurrentUser(Long currentUserId, String currentRole, Long currentOrgId);
    void publishFinalResult(com.project.declaration.dto.PublishResultRequest request, Long auditorId);
    void addSecondRoundExpert(Long topicId, Long expertId);
}
