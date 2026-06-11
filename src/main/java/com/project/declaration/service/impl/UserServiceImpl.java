package com.project.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.declaration.common.BusinessException;
import com.project.declaration.dto.AssignAuthRequest;
import com.project.declaration.dto.AuditUserRequest;
import com.project.declaration.entity.Institution;
import com.project.declaration.entity.User;
import com.project.declaration.mapper.InstitutionMapper;
import com.project.declaration.mapper.UserMapper;
import com.project.declaration.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private InstitutionMapper institutionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditUser(AuditUserRequest request, Long currentOrgId) {
        User user = this.getById(request.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!user.getOrgId().equals(currentOrgId)) {
            throw new BusinessException("无权操作其他机构的用户");
        }
        if (user.getStatus() != 0) {
            throw new BusinessException("该用户已被审核过，不可重复审核");
        }

        user.setStatus(request.getStatus());
        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDeclarationAuth(AssignAuthRequest request, Long currentOrgId) {
        User user = this.getById(request.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!user.getOrgId().equals(currentOrgId)) {
            throw new BusinessException("无权操作其他机构的用户");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException("该用户未处于启用状态，无法分配申报权限");
        }

        if (request.getHasAuth() == 1) {
            // 检查额度
            Institution inst = institutionMapper.selectById(currentOrgId);
            if (inst == null) {
                throw new BusinessException("机构不存在");
            }
            
            // 查询当前已授权的账号数
            Long count = this.count(new LambdaQueryWrapper<User>()
                    .eq(User::getOrgId, currentOrgId)
                    .eq(User::getHasDeclarationAuth, 1));
            
            if (count >= inst.getQuota()) {
                throw new BusinessException(String.format("授权名额已满。当前机构最大申报名额为 %d，已指派 %d 个账号", inst.getQuota(), count));
            }
            
            user.setHasDeclarationAuth(1);
        } else {
            user.setHasDeclarationAuth(0);
        }

        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
    }

    @Override
    public List<User> listInstitutionUsers(Long orgId) {
        return this.list(new LambdaQueryWrapper<User>()
                .eq(User::getOrgId, orgId)
                .in(User::getRole, "ORG_ADMIN", "NORMAL_USER"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustQuota(Long orgId, Integer newQuota) {
        Institution inst = institutionMapper.selectById(orgId);
        if (inst == null) {
            throw new BusinessException("机构单位不存在");
        }
        if (newQuota < 0) {
            throw new BusinessException("名额数量不能为负数");
        }
        inst.setQuota(newQuota);
        inst.setUpdateTime(LocalDateTime.now());
        institutionMapper.updateById(inst);
    }
}
