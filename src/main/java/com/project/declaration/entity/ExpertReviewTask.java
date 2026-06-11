package com.project.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_expert_review_task")
public class ExpertReviewTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long topicId;
    private Long expertId;
    private Integer invitationStatus; // 0-待确认，1-接受评审，2-拒绝评审，3-超时未响应被替换
    private BigDecimal score;
    private String comments;
    private Integer recommendResult; // 1-推荐立项，2-不推荐立项
    private Integer status; // 0-未开始，1-暂存草稿，2-已提交意见
    private LocalDateTime createTime;
    private LocalDateTime replyTime;
    private LocalDateTime submitTime;
}
