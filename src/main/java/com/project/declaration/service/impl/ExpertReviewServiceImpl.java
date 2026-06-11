package com.project.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.declaration.common.BusinessException;
import com.project.declaration.dto.ExpertAssignRequest;
import com.project.declaration.dto.ExpertRespondRequest;
import com.project.declaration.dto.ExpertReviewOpinionRequest;
import com.project.declaration.entity.ExpertReviewTask;
import com.project.declaration.entity.SysNotification;
import com.project.declaration.entity.TopicDeclaration;
import com.project.declaration.entity.User;
import com.project.declaration.mapper.ExpertReviewTaskMapper;
import com.project.declaration.mapper.SysNotificationMapper;
import com.project.declaration.mapper.TopicDeclarationMapper;
import com.project.declaration.mapper.UserMapper;
import com.project.declaration.service.ExpertReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExpertReviewServiceImpl extends ServiceImpl<ExpertReviewTaskMapper, ExpertReviewTask> implements ExpertReviewService {

    @Autowired
    private TopicDeclarationMapper topicDeclarationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SysNotificationMapper notificationMapper;

    @Override
    public List<User> recommendExpertsForTopic(Long topicId) {
        TopicDeclaration topic = topicDeclarationMapper.selectById(topicId);
        if (topic == null) {
            throw new BusinessException("课题不存在");
        }

        // 1. 查询所有启用的评审专家
        List<User> allExperts = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "EXPERT")
                .eq(User::getStatus, 1));

        // 2. 根据单位回避原则和研究方向过滤
        return allExperts.stream().filter(expert -> {
            // 回避原则：不能与课题申报人是同一单位（若均有orgId且相等则回避）
            if (topic.getOrgId() != null && topic.getOrgId().equals(expert.getOrgId())) {
                return false;
            }
            
            // 研究方向匹配：专家主方向或次要方向包含课题的 categoryId
            boolean majorMatch = topic.getCategoryId().equals(expert.getMajorDirection());
            boolean minorMatch = false;
            if (expert.getMinorDirections() != null && !expert.getMinorDirections().trim().isEmpty()) {
                String[] dirs = expert.getMinorDirections().split(",");
                minorMatch = Arrays.asList(dirs).contains(String.valueOf(topic.getCategoryId()));
            }
            
            return majorMatch || minorMatch;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignExperts(ExpertAssignRequest request) {
        TopicDeclaration topic = topicDeclarationMapper.selectById(request.getTopicId());
        if (topic == null) {
            throw new BusinessException("课题不存在");
        }
        if (topic.getStatus() != 4 && topic.getStatus() != 5) {
            throw new BusinessException("该课题当前状态不支持分配专家");
        }

        // 1. 清理已有评审任务 (如果有调整重新分配)
        this.remove(new LambdaQueryWrapper<ExpertReviewTask>().eq(ExpertReviewTask::getTopicId, topic.getId()));

        // 2. 验证选定的三个专家
        for (Long expertId : request.getExpertIds()) {
            User expert = userMapper.selectById(expertId);
            if (expert == null || !"EXPERT".equals(expert.getRole()) || expert.getStatus() != 1) {
                throw new BusinessException("选定的专家无效或未启用: ID " + expertId);
            }
            if (topic.getOrgId() != null && topic.getOrgId().equals(expert.getOrgId())) {
                throw new BusinessException("专家与课题主要研究人员单位相同，违反回避原则: " + expert.getRealName());
            }

            // 创建分配任务
            ExpertReviewTask task = new ExpertReviewTask();
            task.setTopicId(topic.getId());
            task.setExpertId(expertId);
            task.setInvitationStatus(0); // 待确认
            task.setStatus(0); // 未开始
            task.setCreateTime(LocalDateTime.now());
            this.save(task);

            // 发送虚拟评审邀请短信
            String smsContent = String.format("【专家评审邀请】尊敬的%s专家：您好！您已被邀请参加“%s”课题的评审工作，请登录平台进行确认并开展线上评审。感谢支持！", expert.getRealName(), topic.getTitle());
            SysNotification notification = new SysNotification();
            notification.setReceiverMobile(expert.getMobile());
            notification.setType("EXPERT_REVIEW_INVITE");
            notification.setContent(smsContent);
            notification.setSendStatus(1);
            notification.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(notification);

            log.info("专家分配成功，发送邀请通知: 课题[{}], 专家[{}]", topic.getTitle(), expert.getRealName());
        }

        // 3. 更新课题状态为评审中 (5)
        topic.setStatus(5);
        topic.setUpdateTime(LocalDateTime.now());
        topicDeclarationMapper.updateById(topic);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void respondInvitation(ExpertRespondRequest request, Long currentExpertId) {
        ExpertReviewTask task = this.getById(request.getTaskId());
        if (task == null) {
            throw new BusinessException("评审任务不存在");
        }
        if (!task.getExpertId().equals(currentExpertId)) {
            throw new BusinessException("无权操作他人的评审任务");
        }
        if (task.getInvitationStatus() != 0) {
            throw new BusinessException("该评审邀请已被确认或响应过");
        }

        task.setInvitationStatus(request.getAccept());
        task.setReplyTime(LocalDateTime.now());
        this.updateById(task);

        // 如果专家拒绝了邀请，则触发系统自动补充/候选专家逻辑
        if (request.getAccept() == 2) {
            log.info("专家[ID:{}]拒绝了评审任务[ID:{}], 系统开始自动寻找候选专家替换...", currentExpertId, task.getId());
            autoReplaceExpert(task);
        }
    }

    private void autoReplaceExpert(ExpertReviewTask rejectedTask) {
        Long topicId = rejectedTask.getTopicId();
        
        // 1. 获取所有匹配方向且符合单位回避原则的推荐专家
        List<User> recommended = recommendExpertsForTopic(topicId);
        
        // 2. 查询当前课题已经分配的所有专家ID (包含已同意、待确认以及刚拒绝的专家)
        List<ExpertReviewTask> currentTasks = this.list(new LambdaQueryWrapper<ExpertReviewTask>()
                .eq(ExpertReviewTask::getTopicId, topicId));
        Set<Long> allocatedExpertIds = currentTasks.stream()
                .map(ExpertReviewTask::getExpertId)
                .collect(Collectors.toSet());
        
        // 3. 过滤掉这些已分配/拒绝过的专家，得到纯净的候选专家池
        List<User> candidatePool = recommended.stream()
                .filter(expert -> !allocatedExpertIds.contains(expert.getId()))
                .collect(Collectors.toList());
        
        if (candidatePool.isEmpty()) {
            log.warn("无法自动补充专家: 匹配该方向且符合回避原则的可用专家池已枯竭，请超级管理员手动干预调整。课题ID:[{}]", topicId);
            return;
        }
        
        // 4. 随机挑选一名候选专家
        Collections.shuffle(candidatePool);
        User replacementExpert = candidatePool.get(0);
        
        // 5. 将原被拒绝任务的状态修改为已替换
        rejectedTask.setInvitationStatus(3); // 超时/拒绝未响应被替换
        this.updateById(rejectedTask);
        
        // 6. 为替换专家创建新的评审任务
        ExpertReviewTask newTask = new ExpertReviewTask();
        newTask.setTopicId(topicId);
        newTask.setExpertId(replacementExpert.getId());
        newTask.setInvitationStatus(0); // 待确认
        newTask.setStatus(0); // 未开始
        newTask.setCreateTime(LocalDateTime.now());
        this.save(newTask);
        
        // 7. 发送虚拟评审邀请短信
        TopicDeclaration topic = topicDeclarationMapper.selectById(topicId);
        String smsContent = String.format("【专家评审邀请】尊敬的%s专家：您好！您已被补选邀请参加“%s”课题的评审工作，请登录平台进行确认并开展线上评审。感谢支持！", replacementExpert.getRealName(), topic.getTitle());
        SysNotification notification = new SysNotification();
        notification.setReceiverMobile(replacementExpert.getMobile());
        notification.setType("EXPERT_REVIEW_INVITE");
        notification.setContent(smsContent);
        notification.setSendStatus(1);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
        
        log.info("系统自动补充专家成功: 课题[{}], 原专家[ID:{}], 替换专家[{}]", topic.getTitle(), rejectedTask.getExpertId(), replacementExpert.getRealName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReviewOpinion(ExpertReviewOpinionRequest request, Long currentExpertId) {
        ExpertReviewTask task = this.getById(request.getTaskId());
        if (task == null) {
            throw new BusinessException("评审任务不存在");
        }
        if (!task.getExpertId().equals(currentExpertId)) {
            throw new BusinessException("无权操作他人的评审任务");
        }
        if (task.getInvitationStatus() != 1) {
            throw new BusinessException("您尚未确认接受该评审邀请，无法提交评审意见");
        }
        if (task.getStatus() == 2) {
            throw new BusinessException("该评审意见已正式提交，无法再次修改");
        }

        task.setScore(request.getScore());
        task.setComments(request.getComments());
        task.setRecommendResult(request.getRecommendResult());
        
        if (request.getIsSubmit() == 1) {
            task.setStatus(2); // 正式提交意见
            task.setSubmitTime(LocalDateTime.now());
        } else {
            task.setStatus(1); // 暂存草稿
        }
        
        this.updateById(task);

        // 如果正式提交，检查是否该课题的所有专家都已经提交了意见
        if (request.getIsSubmit() == 1) {
            List<ExpertReviewTask> tasks = this.list(new LambdaQueryWrapper<ExpertReviewTask>()
                    .eq(ExpertReviewTask::getTopicId, task.getTopicId()));
            
            // 筛选出确认参与(1)的有效评审任务
            List<ExpertReviewTask> activeTasks = tasks.stream()
                    .filter(t -> t.getInvitationStatus() == 1)
                    .collect(Collectors.toList());
            
            boolean allSubmitted = activeTasks.stream().allMatch(t -> t.getStatus() == 2);
            if (activeTasks.size() >= 3 && allSubmitted) {
                double totalScore = 0;
                long recommendCount = 0;
                for (ExpertReviewTask t : activeTasks) {
                    totalScore += t.getScore().doubleValue();
                    if (t.getRecommendResult() == 1) {
                        recommendCount++;
                    }
                }
                double avgScore = totalScore / activeTasks.size();
                int autoPass = ((double) recommendCount / activeTasks.size()) >= 0.5 ? 1 : 0;

                TopicDeclaration topic = topicDeclarationMapper.selectById(task.getTopicId());
                if (topic != null) {
                    topic.setAverageScore(new java.math.BigDecimal(avgScore).setScale(2, java.math.RoundingMode.HALF_UP));
                    topic.setAutoPass(autoPass);
                    topic.setStatus(6); // 评审结束
                    topic.setFinalPass(0); // 待发布
                    topic.setUpdateTime(LocalDateTime.now());
                    topicDeclarationMapper.updateById(topic);
                    log.info("课题[ID:{}]的所有专家评审已完成。平均分:[{}], 推荐数:[{}], 系统建议立项:[{}]", 
                            topic.getId(), avgScore, recommendCount, autoPass);
                }
            }
        }
    }

    @Override
    public List<ExpertReviewTask> listTasksForExpert(Long expertId) {
        return this.list(new LambdaQueryWrapper<ExpertReviewTask>()
                .eq(ExpertReviewTask::getExpertId, expertId)
                .in(ExpertReviewTask::getInvitationStatus, 0, 1)); // 只展示待确认和已接受的
    }

    @Override
    public List<ExpertReviewTask> listTasksForTopic(Long topicId) {
        return this.list(new LambdaQueryWrapper<ExpertReviewTask>()
                .eq(ExpertReviewTask::getTopicId, topicId));
    }
}
