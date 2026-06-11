package com.project.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.declaration.common.BusinessException;
import com.project.declaration.dto.LoginRequest;
import com.project.declaration.dto.RegisterExpertRequest;
import com.project.declaration.dto.RegisterOrgRequest;
import com.project.declaration.dto.RegisterUserRequest;
import com.project.declaration.entity.Institution;
import com.project.declaration.entity.SysNotification;
import com.project.declaration.entity.User;
import com.project.declaration.mapper.InstitutionMapper;
import com.project.declaration.mapper.SysNotificationMapper;
import com.project.declaration.mapper.UserMapper;
import com.project.declaration.security.JwtUtils;
import com.project.declaration.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private InstitutionMapper institutionMapper;

    @Autowired
    private SysNotificationMapper notificationMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("账号正在审核中，暂不可登录");
        }
        if (user.getStatus() == 2) {
            throw new BusinessException("账号已被禁用");
        }

        // 如果是机构管理员，检验机构状态
        if ("ORG_ADMIN".equals(user.getRole())) {
            Institution inst = institutionMapper.selectById(user.getOrgId());
            if (inst == null) {
                throw new BusinessException("机构信息不存在");
            }
            if (inst.getStatus() == 0) {
                throw new BusinessException("该机构正在认证审核中，管理员账号暂不可登录");
            }
            if (inst.getStatus() == 2) {
                throw new BusinessException("该机构认证已被驳回，原因：" + inst.getRejectReason());
            }
        }

        // 生成 JWT Token
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());
        result.put("orgId", user.getOrgId());
        result.put("hasDeclarationAuth", user.getHasDeclarationAuth());
        Map<String, Object> userObj = new HashMap<>();
        userObj.put("id", user.getId());
        userObj.put("username", user.getUsername());
        userObj.put("realName", user.getRealName());
        userObj.put("role", user.getRole());
        userObj.put("orgId", user.getOrgId());
        userObj.put("hasDeclarationAuth", user.getHasDeclarationAuth());
        userObj.put("majorDirection", user.getMajorDirection());
        result.put("user", userObj);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerOrg(RegisterOrgRequest request) {
        // 1. 验证账号是否被注册
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException("登录账号已存在");
        }

        // 2. 验证机构名称是否已存在
        Long instCount = institutionMapper.selectCount(new LambdaQueryWrapper<Institution>().eq(Institution::getName, request.getOrgName()));
        if (instCount > 0) {
            throw new BusinessException("机构单位名称已被注册");
        }

        // 3. 创建机构 (状态为待审核)
        Institution inst = new Institution();
        inst.setName(request.getOrgName());
        inst.setProvince(request.getProvince());
        inst.setAddress(request.getAddress());
        inst.setLicenseUrl(request.getLicenseUrl());
        inst.setQuota(3); // 默认3个名额
        inst.setStatus(0); // 待审核
        inst.setCreateTime(LocalDateTime.now());
        inst.setUpdateTime(LocalDateTime.now());
        institutionMapper.insert(inst);

        // 4. 创建机构管理员用户 (管理员账号在机构认证通过后才可以启用)
        User admin = new User();
        admin.setUsername(request.getUsername());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRealName(request.getRealName());
        admin.setMobile(request.getMobile());
        admin.setEmail(request.getEmail());
        admin.setRole("ORG_ADMIN");
        admin.setOrgId(inst.getId());
        admin.setStatus(1); // 用户启用状态，但受机构状态制约
        admin.setHasDeclarationAuth(1); // 机构管理员默认有申报权限，占用机构名额
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        userMapper.insert(admin);

        log.info("机构管理员及机构注册完成，等待超级管理员审核: 机构[{}], 账号[{}]", inst.getName(), admin.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerExpert(RegisterExpertRequest request) {
        // 1. 验证账号/手机号是否被注册 (专家以手机号为登录用户名)
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getMobile()));
        if (count > 0) {
            throw new BusinessException("该手机号已注册为系统用户");
        }

        // 2. 注册专家
        User expert = new User();
        expert.setUsername(request.getMobile());
        expert.setPassword(passwordEncoder.encode(request.getPassword()));
        expert.setRealName(request.getRealName());
        expert.setMobile(request.getMobile());
        expert.setRole("EXPERT");
        expert.setStatus(0); // 专家注册后需超级管理员审核启用
        expert.setExpertSignature(request.getExpertSignature());
        expert.setMajorDirection(request.getMajorDirection());
        expert.setMinorDirections(request.getMinorDirections());
        expert.setCreateTime(LocalDateTime.now());
        expert.setUpdateTime(LocalDateTime.now());
        userMapper.insert(expert);

        // 3. 记录虚拟注册邀请短信
        String smsContent = String.format("【专家注册邀请】尊敬的%s专家：您好！诚挚邀请您作为自选课题的评审专家，请登录系统并提交电子签名。感谢支持！", expert.getRealName());
        SysNotification notification = new SysNotification();
        notification.setReceiverMobile(expert.getMobile());
        notification.setType("EXPERT_REGISTER");
        notification.setContent(smsContent);
        notification.setSendStatus(1); // Mock 成功
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);

        log.info("评审专家注册成功，等待超级管理员审核 & 记录虚拟邀请短信: 专家[{}], 手机号[{}]", expert.getRealName(), expert.getMobile());
    }

    @Override
    public void registerUser(RegisterUserRequest request) {
        // 1. 验证用户名是否存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException("登录账号已存在");
        }

        // 2. 检查机构是否存在
        Institution inst = institutionMapper.selectById(request.getOrgId());
        if (inst == null) {
            throw new BusinessException("指定的申报机构不存在");
        }

        // 3. 注册普通用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setMobile(request.getMobile());
        user.setEmail(request.getEmail());
        user.setRole("NORMAL_USER");
        user.setOrgId(inst.getId());
        user.setStatus(0); // 待机构管理员审核
        user.setHasDeclarationAuth(0); // 默认没有申报权限，需管理员指派
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        log.info("机构普通员工注册成功，等待机构管理员审核: 姓名[{}], 账号[{}], 属于机构[{}]", user.getRealName(), user.getUsername(), inst.getName());
    }
}
