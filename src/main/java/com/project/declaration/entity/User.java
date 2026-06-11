package com.project.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String mobile;
    private String email;
    private String role; // SUPER_ADMIN, ORG_ADMIN, NORMAL_USER, EXPERT
    private Long orgId;
    private String expertSignature;
    private Long majorDirection; // 专家主要研究方向
    private String minorDirections; // 专家次要研究方向 (逗号分隔)
    private Integer status; // 0-待审核，1-启用，2-禁用
    private Integer hasDeclarationAuth; // 0-无，1-有
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
