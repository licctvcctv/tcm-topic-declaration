package com.project.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.project.declaration.common.BusinessException;
import com.project.declaration.dto.PeriodConfigRequest;
import com.project.declaration.entity.DeclarationPeriod;
import com.project.declaration.mapper.DeclarationPeriodMapper;
import com.project.declaration.service.DeclarationPeriodService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DeclarationPeriodServiceImpl extends ServiceImpl<DeclarationPeriodMapper, DeclarationPeriod> implements DeclarationPeriodService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdatePeriod(PeriodConfigRequest request) {
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }

        DeclarationPeriod period = this.getOne(new LambdaQueryWrapper<DeclarationPeriod>()
                .eq(DeclarationPeriod::getYear, request.getYear()));

        if (period == null) {
            period = new DeclarationPeriod();
            period.setYear(request.getYear());
        }

        period.setStartTime(request.getStartTime());
        period.setEndTime(request.getEndTime());
        period.setStatus(request.getStatus());
        period.setInstructions(request.getInstructions());

        this.saveOrUpdate(period);

        // 如果该配置设为开启(1)，我们需要将该年份之外的其他年份配置均设为关闭(0)
        if (request.getStatus() == 1) {
            DeclarationPeriod finalPeriod = period;
            this.update(new LambdaUpdateWrapper<DeclarationPeriod>()
                    .ne(DeclarationPeriod::getId, finalPeriod.getId())
                    .set(DeclarationPeriod::getStatus, 0));
        }
    }

    @Override
    public DeclarationPeriod getActivePeriod(Integer year) {
        LambdaQueryWrapper<DeclarationPeriod> query = new LambdaQueryWrapper<>();
        if (year != null) {
            query.eq(DeclarationPeriod::getYear, year);
        } else {
            query.eq(DeclarationPeriod::getStatus, 1);
        }
        return this.getOne(query);
    }

    @Override
    public boolean isDeclarationOpen() {
        DeclarationPeriod activePeriod = this.getOne(new LambdaQueryWrapper<DeclarationPeriod>()
                .eq(DeclarationPeriod::getStatus, 1));
        if (activePeriod == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(activePeriod.getStartTime()) && now.isBefore(activePeriod.getEndTime());
    }
}
