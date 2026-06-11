package com.project.declaration.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.declaration.entity.User;
import com.project.declaration.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    @Autowired
    private UserMapper userMapper;

    public String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public User getCurrentUser() {
        String username = getCurrentUsername();
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
}
