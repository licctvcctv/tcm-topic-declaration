package com.project.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.declaration.common.BusinessException;
import com.project.declaration.dto.AuditOrgRequest;
import com.project.declaration.entity.Institution;
import com.project.declaration.mapper.InstitutionMapper;
import com.project.declaration.service.InstitutionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InstitutionServiceImpl extends ServiceImpl<InstitutionMapper, Institution> implements InstitutionService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditInstitution(AuditOrgRequest request) {
        Institution inst = this.getById(request.getOrgId());
        if (inst == null) {
            throw new BusinessException("机构单位不存在");
        }
        if (inst.getStatus() != 0) {
            throw new BusinessException("机构单位已被审核过，不可重复审核");
        }

        inst.setStatus(request.getStatus());
        if (request.getStatus() == 2) {
            if (request.getRejectReason() == null || request.getRejectReason().trim().isEmpty()) {
                throw new BusinessException("请填写拒绝驳回的原因");
            }
            inst.setRejectReason(request.getRejectReason());
        }
        inst.setUpdateTime(LocalDateTime.now());
        this.updateById(inst);
    }

    @Override
    public List<Institution> listAllPending() {
        return this.list(new LambdaQueryWrapper<Institution>().eq(Institution::getStatus, 0));
    }
}
