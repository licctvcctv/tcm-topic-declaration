package com.project.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.declaration.entity.TopicCategory;
import com.project.declaration.mapper.TopicCategoryMapper;
import com.project.declaration.service.TopicCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TopicCategoryServiceImpl extends ServiceImpl<TopicCategoryMapper, TopicCategory> implements TopicCategoryService {

    @Override
    public List<TopicCategory> listActiveCategories() {
        return this.list(new LambdaQueryWrapper<TopicCategory>().eq(TopicCategory::getStatus, 1));
    }
}
