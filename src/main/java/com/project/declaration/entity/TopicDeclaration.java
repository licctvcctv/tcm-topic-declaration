package com.project.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_topic_declaration")
public class TopicDeclaration {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Long orgId;
    private Long declarerId;
    private Long categoryId;
    private String secondaryCategories; // Comma separated IDs
    private String contactMobile;
    private String taskBookUrl;
    private String anonymousPageUrl;
    private Integer status; // 0-草稿, 1-待机构审核, 2-待格式审核, 3-机构退回, 4-待专家分配, 5-评审中, 6-待发布, 7-格式不通过, 8-已发布
    private BigDecimal averageScore;
    private Integer autoPass; // 0-不通过, 1-通过
    private Integer finalPass; // 0-待发布/未发布, 1-立项通过, 2-立项不通过
    private String adminPublishOpinion;
    private String announcementContent;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
