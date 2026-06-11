package com.project.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.declaration.entity.TopicCategory;

import java.util.List;

public interface TopicCategoryService extends IService<TopicCategory> {
    List<TopicCategory> listActiveCategories();
}
