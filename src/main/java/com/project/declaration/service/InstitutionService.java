package com.project.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.declaration.dto.AuditOrgRequest;
import com.project.declaration.entity.Institution;

import java.util.List;

public interface InstitutionService extends IService<Institution> {
    void auditInstitution(AuditOrgRequest request);
    List<Institution> listAllPending();
}
