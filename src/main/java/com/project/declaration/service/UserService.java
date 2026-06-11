package com.project.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.declaration.dto.AssignAuthRequest;
import com.project.declaration.dto.AuditUserRequest;
import com.project.declaration.entity.User;

import java.util.List;

public interface UserService extends IService<User> {
    void auditUser(AuditUserRequest request, Long currentOrgId);
    void assignDeclarationAuth(AssignAuthRequest request, Long currentOrgId);
    List<User> listInstitutionUsers(Long orgId);
    void adjustQuota(Long orgId, Integer newQuota);
    void auditSystemUser(AuditUserRequest request, String role);
}
