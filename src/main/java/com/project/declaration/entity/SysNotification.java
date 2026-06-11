package com.project.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_sys_notification")
public class SysNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String receiverMobile;
    private String type; // EXPERT_REGISTER, EXPERT_REVIEW_INVITE, REVIEW_RESULT
    private String content;
    private Integer sendStatus; // 1-Mock发送成功
    private LocalDateTime createTime;
}
