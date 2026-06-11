package com.project.declaration.service;

import com.project.declaration.dto.LoginRequest;
import com.project.declaration.dto.RegisterExpertRequest;
import com.project.declaration.dto.RegisterOrgRequest;
import com.project.declaration.dto.RegisterUserRequest;

import java.util.Map;

public interface AuthService {
    Map<String, Object> login(LoginRequest request);
    void registerOrg(RegisterOrgRequest request);
    void registerExpert(RegisterExpertRequest request);
    void registerUser(RegisterUserRequest request);
}
