package com.project.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.declaration.common.BusinessException;
import com.project.declaration.dto.TopicAuditRequest;
import com.project.declaration.dto.TopicDeclarationRequest;
import com.project.declaration.entity.Institution;
import com.project.declaration.entity.TopicAuditLog;
import com.project.declaration.entity.TopicDeclaration;
import com.project.declaration.entity.User;
import com.project.declaration.mapper.InstitutionMapper;
import com.project.declaration.mapper.TopicAuditLogMapper;
import com.project.declaration.mapper.TopicDeclarationMapper;
import com.project.declaration.mapper.UserMapper;
import com.project.declaration.service.DeclarationPeriodService;
import com.project.declaration.service.TopicDeclarationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.declaration.dto.PublishResultRequest;
import com.project.declaration.entity.ExpertReviewTask;
import com.project.declaration.entity.SysNotification;
import com.project.declaration.mapper.ExpertReviewTaskMapper;
import com.project.declaration.mapper.SysNotificationMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TopicDeclarationServiceImpl extends ServiceImpl<TopicDeclarationMapper, TopicDeclaration> implements TopicDeclarationService {

    @Autowired
    private DeclarationPeriodService periodService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private InstitutionMapper institutionMapper;

    @Autowired
    private TopicAuditLogMapper auditLogMapper;

    @Autowired
    private ExpertReviewTaskMapper expertReviewTaskMapper;

    @Autowired
    private SysNotificationMapper notificationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized void saveOrSubmitTopic(TopicDeclarationRequest request, Long declarerId, Long orgId) {
        // 1. 校验申报周期是否开启
        if (!periodService.isDeclarationOpen()) {
            throw new BusinessException("课题申报通道当前处于关闭状态（不在开放期内）");
        }

        // 2. 校验用户是否拥有申报权限
        User user = userMapper.selectById(declarerId);
        if (user == null || user.getHasDeclarationAuth() != 1) {
            throw new BusinessException("您没有本年度的课题申报权限");
        }

        Institution inst = institutionMapper.selectById(orgId);
        if (inst == null) {
            throw new BusinessException("填报机构不存在");
        }

        TopicDeclaration topic;
        if (request.getId() != null) {
            topic = this.getById(request.getId());
            if (topic == null) {
                throw new BusinessException("要修改的课题不存在");
            }
            if (!topic.getDeclarerId().equals(declarerId)) {
                throw new BusinessException("无权修改他人的课题");
            }
            // 只有草稿(0)和退回修改(3)状态才可以修改
            if (topic.getStatus() != 0 && topic.getStatus() != 3) {
                throw new BusinessException("课题已进入审核评审阶段，无法修改");
            }
        } else {
            topic = new TopicDeclaration();
            topic.setDeclarerId(declarerId);
            topic.setOrgId(orgId);
            topic.setCreateTime(LocalDateTime.now());
        }

        // 3. 如果是正式提交，校验机构额度限制
        if (request.getIsSubmit() == 1) {
            // 查询本机构已提交的课题数量 (状态为 1, 2, 4, 5, 6 均属于已提交课题)
            // 排除当前修改的这一个课题，避免自己占额度导致无法重新提交
            Long submittedCount = this.count(new LambdaQueryWrapper<TopicDeclaration>()
                    .eq(TopicDeclaration::getOrgId, orgId)
                    .ne(request.getId() != null, TopicDeclaration::getId, request.getId())
                    .in(TopicDeclaration::getStatus, 1, 2, 4, 5, 6, 8));

            if (submittedCount >= inst.getQuota()) {
                throw new BusinessException(String.format("提交失败，本机构已提交的课题数量（%d个）已达到当年上限（%d个）", submittedCount, inst.getQuota()));
            }

            topic.setStatus(1); // 已提交待机构审核
        } else {
            topic.setStatus(0); // 保存草稿
        }

        topic.setTitle(request.getTitle());
        topic.setCategoryId(request.getCategoryId());
        topic.setSecondaryCategories(request.getSecondaryCategories());
        topic.setContactMobile(request.getContactMobile());
        topic.setTaskBookUrl(request.getTaskBookUrl());
        topic.setAnonymousPageUrl(request.getAnonymousPageUrl());
        topic.setUpdateTime(LocalDateTime.now());

        this.saveOrUpdate(topic);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditByOrg(TopicAuditRequest request, Long auditorId, Long orgId) {
        TopicDeclaration topic = this.getById(request.getTopicId());
        if (topic == null) {
            throw new BusinessException("课题不存在");
        }
        if (!topic.getOrgId().equals(orgId)) {
            throw new BusinessException("无权审核其他机构的课题");
        }
        if (topic.getStatus() != 1) {
            throw new BusinessException("课题当前状态不支持机构管理员审核");
        }

        TopicAuditLog log = new TopicAuditLog();
        log.setTopicId(topic.getId());
        log.setAuditorId(auditorId);
        log.setCreateTime(LocalDateTime.now());

        if (request.getApprove() == 1) {
            topic.setStatus(2); // 机构审核通过，待超管审核
            log.setAction("ORG_APPROVE");
        } else {
            topic.setStatus(3); // 退回修改
            log.setAction("ORG_REJECT");
            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                throw new BusinessException("请填写退回修改的原因/审核意见");
            }
            log.setReason(request.getReason());
        }

        topic.setUpdateTime(LocalDateTime.now());
        this.updateById(topic);
        auditLogMapper.insert(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditBySuperAdmin(TopicAuditRequest request, Long auditorId) {
        TopicDeclaration topic = this.getById(request.getTopicId());
        if (topic == null) {
            throw new BusinessException("课题不存在");
        }
        if (topic.getStatus() != 2) {
            throw new BusinessException("课题当前状态不支持超级管理员格式审核");
        }

        TopicAuditLog log = new TopicAuditLog();
        log.setTopicId(topic.getId());
        log.setAuditorId(auditorId);
        log.setCreateTime(LocalDateTime.now());

        if (request.getApprove() == 1) {
            topic.setStatus(4); // 超管审核通过（待分配专家）
            log.setAction("SUPER_APPROVE");
        } else {
            topic.setStatus(7); // 格式审核不通过（流程直接终止/不通过）
            log.setAction("SUPER_REJECT");
            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                throw new BusinessException("请填写格式审核不通过的原因");
            }
            log.setReason(request.getReason());
        }

        topic.setUpdateTime(LocalDateTime.now());
        this.updateById(topic);
        auditLogMapper.insert(log);
    }

    @Override
    public TopicDeclaration getTopicDetail(Long id, Long currentUserId, String currentRole, Long currentOrgId) {
        TopicDeclaration topic = this.getById(id);
        if (topic == null) {
            throw new BusinessException("课题不存在");
        }

        // 校验查看权限
        if ("SUPER_ADMIN".equals(currentRole)) {
            // 超管可看全部，并且全部字段可见
            return topic;
        } else if ("ORG_ADMIN".equals(currentRole)) {
            // 机构管理员只能看本机构课题
            if (!topic.getOrgId().equals(currentOrgId)) {
                throw new BusinessException("无权查看其他机构的课题");
            }
            return topic;
        } else if ("NORMAL_USER".equals(currentRole)) {
            // 普通申报人只能看自己发起的课题
            if (!topic.getDeclarerId().equals(currentUserId)) {
                throw new BusinessException("无权查看他人的课题");
            }
            return topic;
        } else if ("EXPERT".equals(currentRole)) {
            Long taskCount = expertReviewTaskMapper.selectCount(new LambdaQueryWrapper<ExpertReviewTask>()
                    .eq(ExpertReviewTask::getTopicId, topic.getId())
                    .eq(ExpertReviewTask::getExpertId, currentUserId)
                    .in(ExpertReviewTask::getInvitationStatus, 0, 1));
            if (taskCount == 0) {
                throw new BusinessException("无权查看未分配给您的评审课题");
            }

            TopicDeclaration masked = new TopicDeclaration();
            masked.setId(topic.getId());
            masked.setTitle(topic.getTitle());
            masked.setCategoryId(topic.getCategoryId());
            masked.setSecondaryCategories(topic.getSecondaryCategories());
            masked.setStatus(topic.getStatus());
            masked.setCreateTime(topic.getCreateTime());
            
            // 评审结束后专家只能查看自己的评审意见，无法再查看课题《活页》内容
            if (topic.getStatus() >= 6) {
                masked.setAnonymousPageUrl(null); // 评审已结束，设为空阻止查看
            } else {
                masked.setAnonymousPageUrl(topic.getAnonymousPageUrl());
            }
            return masked;
        }

        throw new BusinessException("无效的角色身份，无权访问");
    }

    @Override
    public List<TopicDeclaration> listTopicsForCurrentUser(Long currentUserId, String currentRole, Long currentOrgId) {
        if ("SUPER_ADMIN".equals(currentRole)) {
            return this.list();
        } else if ("ORG_ADMIN".equals(currentRole)) {
            return this.list(new LambdaQueryWrapper<TopicDeclaration>().eq(TopicDeclaration::getOrgId, currentOrgId));
        } else if ("NORMAL_USER".equals(currentRole)) {
            return this.list(new LambdaQueryWrapper<TopicDeclaration>().eq(TopicDeclaration::getDeclarerId, currentUserId));
        }
        return List.of();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishFinalResult(PublishResultRequest request, Long auditorId) {
        TopicDeclaration topic = this.getById(request.getTopicId());
        if (topic == null) {
            throw new BusinessException("课题不存在");
        }
        if (topic.getStatus() != 6) {
            throw new BusinessException("课题评审未结束，无法发布最终评审结果");
        }

        topic.setFinalPass(request.getFinalPass());
        topic.setAdminPublishOpinion(request.getAdminOpinion());
        topic.setPublishTime(LocalDateTime.now());
        topic.setStatus(8);
        if (request.getFinalPass() == 1) {
            topic.setAnnouncementContent(request.getAnnouncementContent());
        } else {
            topic.setAnnouncementContent(null);
        }
        topic.setUpdateTime(LocalDateTime.now());
        this.updateById(topic);

        User declarer = userMapper.selectById(topic.getDeclarerId());
        if (declarer != null) {
            SysNotification notification = new SysNotification();
            notification.setReceiverMobile(topic.getContactMobile() != null ? topic.getContactMobile() : declarer.getMobile());
            notification.setType("REVIEW_RESULT");
            notification.setContent(String.format("【课题评审结果】您申报的课题“%s”最终结果为：%s。%s",
                    topic.getTitle(),
                    request.getFinalPass() == 1 ? "立项通过" : "立项不通过",
                    request.getAdminOpinion() == null ? "" : request.getAdminOpinion()));
            notification.setSendStatus(1);
            notification.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(notification);
        }

        TopicAuditLog logEntry = new TopicAuditLog();
        logEntry.setTopicId(topic.getId());
        logEntry.setAuditorId(auditorId);
        logEntry.setAction(request.getFinalPass() == 1 ? "FINAL_PASS" : "FINAL_REJECT");
        logEntry.setReason(request.getAdminOpinion());
        logEntry.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(logEntry);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSecondRoundExpert(Long topicId, Long expertId) {
        TopicDeclaration topic = this.getById(topicId);
        if (topic == null) {
            throw new BusinessException("课题不存在");
        }
        if (topic.getStatus() != 5 && topic.getStatus() != 6) {
            throw new BusinessException("该课题当前状态不支持追加第二次评审专家");
        }
        if (topic.getFinalPass() != null && topic.getFinalPass() != 0) {
            throw new BusinessException("最终结果已发布，不能再追加第二次评审专家");
        }

        List<ExpertReviewTask> existing = expertReviewTaskMapper.selectList(
                new LambdaQueryWrapper<ExpertReviewTask>().eq(ExpertReviewTask::getTopicId, topicId));
        boolean alreadyAssigned = existing.stream().anyMatch(t -> t.getExpertId().equals(expertId));
        if (alreadyAssigned) {
            throw new BusinessException("该专家已经参与了该课题的评审，第二次评审专家不可重复");
        }

        User expert = userMapper.selectById(expertId);
        if (expert == null || !"EXPERT".equals(expert.getRole()) || expert.getStatus() != 1) {
            throw new BusinessException("指定的评审专家不存在或未启用");
        }
        
        if (topic.getOrgId() != null && topic.getOrgId().equals(expert.getOrgId())) {
            throw new BusinessException("专家与课题主要研究人员单位相同，违反回避原则: " + expert.getRealName());
        }
        if (!matchesDirection(topic, expert)) {
            throw new BusinessException("专家研究方向与课题方向不匹配: " + expert.getRealName());
        }

        ExpertReviewTask task = new ExpertReviewTask();
        task.setTopicId(topicId);
        task.setExpertId(expertId);
        task.setInvitationStatus(0); // 待确认
        task.setStatus(0); // 未开始
        task.setCreateTime(LocalDateTime.now());
        expertReviewTaskMapper.insert(task);

        if (topic.getStatus() == 6) {
            topic.setStatus(5);
            topic.setFinalPass(0);
            topic.setUpdateTime(LocalDateTime.now());
            this.updateById(topic);
        }

        String smsContent = String.format("【专家评审邀请】尊敬的%s专家：您好！您被追加为“%s”课题的第二次评审专家，请登录平台确认并线上评审。", expert.getRealName(), topic.getTitle());
        SysNotification notification = new SysNotification();
        notification.setReceiverMobile(expert.getMobile());
        notification.setType("EXPERT_REVIEW_INVITE");
        notification.setContent(smsContent);
        notification.setSendStatus(1);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
    }

    private boolean matchesDirection(TopicDeclaration topic, User expert) {
        if (topic.getCategoryId() == null) {
            return true;
        }
        if (topic.getCategoryId().equals(expert.getMajorDirection())) {
            return true;
        }
        if (expert.getMinorDirections() == null || expert.getMinorDirections().trim().isEmpty()) {
            return false;
        }
        return java.util.Arrays.asList(expert.getMinorDirections().split(",")).contains(String.valueOf(topic.getCategoryId()));
    }
}
