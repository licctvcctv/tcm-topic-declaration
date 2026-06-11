package com.project.declaration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.project.declaration.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
