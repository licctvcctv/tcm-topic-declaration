package com.project.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.project.declaration.dto.PeriodConfigRequest;
import com.project.declaration.entity.DeclarationPeriod;

public interface DeclarationPeriodService extends IService<DeclarationPeriod> {
    void saveOrUpdatePeriod(PeriodConfigRequest request);
    DeclarationPeriod getActivePeriod(Integer year);
    boolean isDeclarationOpen();
}
