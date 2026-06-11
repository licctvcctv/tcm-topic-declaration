package com.project.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_topic_audit_log")
public class TopicAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long topicId;
    private Long auditorId;
    private String action; // ORG_APPROVE, ORG_REJECT, SUPER_APPROVE, SUPER_REJECT
    private String reason;
    private LocalDateTime createTime;
}
