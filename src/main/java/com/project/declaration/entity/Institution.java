package com.project.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_institution")
public class Institution {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String province;
    private String address;
    private String licenseUrl;
    private Integer quota;
    private Integer status; // 0-待审核，1-已认证，2-审核驳回
    private String rejectReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
