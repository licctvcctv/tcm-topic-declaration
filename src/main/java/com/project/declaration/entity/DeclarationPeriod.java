package com.project.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_declaration_period")
public class DeclarationPeriod {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer year;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status; // 0-关闭，1-启用
    private String instructions;
}
