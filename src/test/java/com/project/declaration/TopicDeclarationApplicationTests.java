package com.project.declaration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.declaration.common.BusinessException;
import com.project.declaration.dto.AssignAuthRequest;
import com.project.declaration.dto.ExpertRespondRequest;
import com.project.declaration.dto.ExpertReviewOpinionRequest;
import com.project.declaration.dto.RegisterOrgRequest;
import com.project.declaration.entity.ExpertReviewTask;
import com.project.declaration.entity.Institution;
import com.project.declaration.entity.TopicDeclaration;
import com.project.declaration.entity.User;
import com.project.declaration.mapper.ExpertReviewTaskMapper;
import com.project.declaration.mapper.InstitutionMapper;
import com.project.declaration.mapper.SysNotificationMapper;
import com.project.declaration.mapper.TopicDeclarationMapper;
import com.project.declaration.mapper.UserMapper;
import com.project.declaration.service.impl.AuthServiceImpl;
import com.project.declaration.service.impl.ExpertReviewServiceImpl;
import com.project.declaration.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TopicDeclarationApplicationTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private InstitutionMapper institutionMapper;

    @Mock
    private SysNotificationMapper notificationMapper;

    @Mock
    private TopicDeclarationMapper topicDeclarationMapper;

    @Mock
    private ExpertReviewTaskMapper expertReviewTaskMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @InjectMocks
    private UserServiceImpl userService;

    @InjectMocks
    private ExpertReviewServiceImpl expertReviewService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // 通过反射手动将 mock mapper 注入 ServiceImpl 的 protected baseMapper 属性，解决 Mockito @InjectMocks 无法注入父类属性的问题
        setField(userService, "baseMapper", userMapper);
        setField(expertReviewService, "baseMapper", expertReviewTaskMapper);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = null;
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field != null) {
            field.setAccessible(true);
            field.set(target, value);
        }
    }

    @Test
    void testRegisterOrg_success() {
        RegisterOrgRequest request = new RegisterOrgRequest();
        request.setUsername("test_org");
        request.setPassword("123456");
        request.setOrgName("测试中医药机构");
        request.setProvince("北京市");
        request.setAddress("朝阳区1号");
        request.setMobile("13300000000");
        request.setLicenseUrl("/license.png");

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(institutionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("encoded_123456");

        assertDoesNotThrow(() -> authService.registerOrg(request));
        verify(institutionMapper, times(1)).insert(any(Institution.class));
        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    void testAssignDeclarationAuth_quotaExceeded() {
        Long orgId = 1L;
        Institution inst = new Institution();
        inst.setId(orgId);
        inst.setQuota(2);

        User user = new User();
        user.setId(2L);
        user.setOrgId(orgId);
        user.setStatus(1);

        when(userMapper.selectById(2L)).thenReturn(user);
        when(institutionMapper.selectById(orgId)).thenReturn(inst);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        AssignAuthRequest request = new AssignAuthRequest();
        request.setUserId(2L);
        request.setHasAuth(1);

        assertThrows(BusinessException.class, () -> userService.assignDeclarationAuth(request, orgId));
    }

    @Test
    void testRespondInvitation_rejectAndAutoReplace() {
        Long expertId = 10L;
        Long taskId = 100L;
        Long topicId = 500L;

        ExpertReviewTask task = new ExpertReviewTask();
        task.setId(taskId);
        task.setExpertId(expertId);
        task.setTopicId(topicId);
        task.setInvitationStatus(0);

        when(expertReviewTaskMapper.selectById(taskId)).thenReturn(task);

        TopicDeclaration topic = new TopicDeclaration();
        topic.setId(topicId);
        topic.setCategoryId(1L);
        topic.setOrgId(1L);
        topic.setTitle("自选课题");
        when(topicDeclarationMapper.selectById(topicId)).thenReturn(topic);

        User anotherExpert = new User();
        anotherExpert.setId(11L);
        anotherExpert.setRole("EXPERT");
        anotherExpert.setStatus(1);
        anotherExpert.setMajorDirection(1L);
        anotherExpert.setMobile("13800000000");

        List<User> experts = new ArrayList<>();
        experts.add(anotherExpert);
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(experts);

        List<ExpertReviewTask> allocated = new ArrayList<>();
        allocated.add(task);
        when(expertReviewTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(allocated);

        ExpertRespondRequest request = new ExpertRespondRequest();
        request.setTaskId(taskId);
        request.setAccept(2);

        assertDoesNotThrow(() -> expertReviewService.respondInvitation(request, expertId));
        assertEquals(3, task.getInvitationStatus());
        verify(expertReviewTaskMapper, times(1)).insert(any(ExpertReviewTask.class));
    }

    @Test
    void testSubmitReviewOpinion_lockedAfterSubmit() {
        Long expertId = 10L;
        Long taskId = 100L;

        ExpertReviewTask task = new ExpertReviewTask();
        task.setId(taskId);
        task.setExpertId(expertId);
        task.setInvitationStatus(1);
        task.setStatus(2);

        when(expertReviewTaskMapper.selectById(taskId)).thenReturn(task);

        ExpertReviewOpinionRequest request = new ExpertReviewOpinionRequest();
        request.setTaskId(taskId);
        request.setScore(new BigDecimal("90.0"));
        request.setComments("非常好");
        request.setRecommendResult(1);
        request.setIsSubmit(1);

        assertThrows(BusinessException.class, () -> expertReviewService.submitReviewOpinion(request, expertId));
    }
}
