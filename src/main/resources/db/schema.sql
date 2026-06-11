-- 创建数据库
CREATE DATABASE IF NOT EXISTS `topic_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `topic_db`;

-- 1. 机构表
CREATE TABLE IF NOT EXISTS `tb_institution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '机构ID',
  `name` VARCHAR(150) NOT NULL UNIQUE COMMENT '机构/单位名称',
  `province` VARCHAR(50) NOT NULL COMMENT '省份（下拉选择）',
  `address` VARCHAR(255) NOT NULL COMMENT '单位详细地址',
  `license_url` VARCHAR(500) NOT NULL COMMENT '资质材料上传路径 (法人证书/营业执照等)',
  `quota` INT NOT NULL DEFAULT 3 COMMENT '当年课题申报名额数量（超管可单独调整）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '认证状态：0-待审核，1-已认证，2-审核驳回',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '机构审核驳回原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机构表';

-- 2. 用户表
CREATE TABLE IF NOT EXISTS `tb_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
  `password` VARCHAR(100) NOT NULL COMMENT '加密密码',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名/联系人',
  `mobile` VARCHAR(20) NOT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '电子邮箱',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：SUPER_ADMIN(超管), ORG_ADMIN(机构管理员), NORMAL_USER(普通用户), EXPERT(评审专家)',
  `org_id` BIGINT DEFAULT NULL COMMENT '所属机构ID (超管和非机构专家可为空)',
  `expert_signature` VARCHAR(500) DEFAULT NULL COMMENT '专家手写签字图片 URL (仅专家角色使用)',
  `major_direction` BIGINT DEFAULT NULL COMMENT '专家主要研究方向ID',
  `minor_directions` VARCHAR(255) DEFAULT NULL COMMENT '专家次要研究方向ID串（逗号分隔）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待审核（普通用户需机构管理员审核），1-启用，2-禁用',
  `has_declaration_auth` TINYINT NOT NULL DEFAULT 0 COMMENT '是否拥有当年申报权限：0-无，1-有（机构管理员分配）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 3. 课题分类表
CREATE TABLE IF NOT EXISTS `tb_topic_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(100) NOT NULL COMMENT '分类名称（例如：医保支付方式改革）',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课题分类表';

-- 4. 课题申报表
CREATE TABLE IF NOT EXISTS `tb_topic_declaration` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课题ID',
  `title` VARCHAR(200) NOT NULL COMMENT '课题名称',
  `org_id` BIGINT NOT NULL COMMENT '申报机构ID',
  `declarer_id` BIGINT NOT NULL COMMENT '申报人ID',
  `category_id` BIGINT NOT NULL COMMENT '课题分类ID（主研究方向）',
  `secondary_categories` VARCHAR(255) DEFAULT NULL COMMENT '次要研究方向ID串（逗号分隔）',
  `contact_mobile` VARCHAR(20) NOT NULL COMMENT '接收通知的手机号码',
  `task_book_url` VARCHAR(500) NOT NULL COMMENT '《任务书》文件路径',
  `anonymous_page_url` VARCHAR(500) NOT NULL COMMENT '《活页》文件路径 (用于专家评审)',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-草稿，1-已提交待机构审核，2-机构审核通过待超管格式审核，3-退回修改，4-超管审核通过（待分配专家），5-评审中，6-评审结束',
  `average_score` DECIMAL(5,2) DEFAULT NULL COMMENT '评审平均分',
  `auto_pass` TINYINT DEFAULT NULL COMMENT '系统建议立项：0-不通过，1-通过',
  `final_pass` TINYINT DEFAULT NULL COMMENT '超管最终立项发布状态：0-未发布，1-立项通过，2-立项不通过',
  `admin_publish_opinion` VARCHAR(500) DEFAULT NULL COMMENT '超管发布意见/立项批复',
  `announcement_content` VARCHAR(1000) DEFAULT NULL COMMENT '通过可见的公告通知内容',
  `publish_time` DATETIME DEFAULT NULL COMMENT '结果发布时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_org_id` (`org_id`),
  KEY `idx_declarer_id` (`declarer_id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课题申报表';

-- 5. 课题审核日志表
CREATE TABLE IF NOT EXISTS `tb_topic_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `topic_id` BIGINT NOT NULL COMMENT '课题ID',
  `auditor_id` BIGINT NOT NULL COMMENT '审核人ID',
  `action` VARCHAR(50) NOT NULL COMMENT '动作：ORG_APPROVE(机构通过), ORG_REJECT(机构退回), SUPER_APPROVE(超管通过), SUPER_REJECT(超管不通过)',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '审核意见/退回原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  PRIMARY KEY (`id`),
  KEY `idx_topic_id` (`topic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课题审核日志表';

-- 6. 专家评审任务表
CREATE TABLE IF NOT EXISTS `tb_expert_review_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `topic_id` BIGINT NOT NULL COMMENT '被评审课题ID',
  `expert_id` BIGINT NOT NULL COMMENT '评审专家ID',
  `invitation_status` TINYINT NOT NULL DEFAULT 0 COMMENT '邀请状态：0-待确认，1-接受评审，2-拒绝评审，3-超时未响应被替换',
  `score` DECIMAL(5,2) DEFAULT NULL COMMENT '评审打分',
  `comments` TEXT DEFAULT NULL COMMENT '评审意见',
  `recommend_result` TINYINT DEFAULT 1 COMMENT '评审结论：1-推荐立项，2-不推荐立项',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '评审状态：0-未开始，1-暂存草稿，2-已提交意见',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  `reply_time` DATETIME DEFAULT NULL COMMENT '专家接受/拒绝时间',
  `submit_time` DATETIME DEFAULT NULL COMMENT '意见提交时间',
  PRIMARY KEY (`id`),
  KEY `idx_topic_id` (`topic_id`),
  KEY `idx_expert_id` (`expert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专家评审任务表';

-- 7. 申报周期配置表
CREATE TABLE IF NOT EXISTS `tb_declaration_period` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `year` INT NOT NULL UNIQUE COMMENT '申报年份',
  `start_time` DATETIME NOT NULL COMMENT '申报开始时间',
  `end_time` DATETIME NOT NULL COMMENT '申报结束时间',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用：0-关闭，1-启用',
  `instructions` TEXT DEFAULT NULL COMMENT '申报注意事项内容（前台展示，支持后台维护）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申报周期配置表';

-- 8. 虚拟短信通知历史表
CREATE TABLE IF NOT EXISTS `tb_sys_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `receiver_mobile` VARCHAR(20) NOT NULL COMMENT '接收手机号',
  `type` VARCHAR(50) NOT NULL COMMENT '模板类型：EXPERT_REGISTER, EXPERT_REVIEW_INVITE, REVIEW_RESULT等',
  `content` VARCHAR(1000) NOT NULL COMMENT '发送的短信内容',
  `send_status` TINYINT NOT NULL DEFAULT 1 COMMENT '发送状态：1-Mock发送成功',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='虚拟短信/通知历史表';

-- 9. 初始数据填充 (主要研究方向/课题分类)
INSERT INTO `tb_topic_category` (`name`, `parent_id`, `status`) VALUES
('医保支付方式改革', 0, 1),
('医疗服务价格改革', 0, 1),
('医保目录和支付标准研究', 0, 1),
('药品集中带量采购', 0, 1),
('中医优势病种遴选', 0, 1),
('中医分级诊疗制度建设', 0, 1),
('公立中医类医院薪酬制度改革', 0, 1),
('基层中医药服务能力提升', 0, 1),
('中医药适宜技术推广', 0, 1),
('医疗机构中药制剂管理', 0, 1),
('医养结合发展', 0, 1),
('中医药政策文件研究及监测评估', 0, 1),
('中医药数据分析', 0, 1),
('其他', 0, 1);

-- 10. 初始化一个超级管理员账号 (账号: admin, 密码明文: admin123, 角色: SUPER_ADMIN)
INSERT INTO `tb_user` (`username`, `password`, `real_name`, `mobile`, `email`, `role`, `status`) VALUES
('admin', '$2a$10$l5A5DS/0.rTiJl3kABPxtuCoan3Djr.DKn.g6VEBesS.Fvy5looVm', '系统超级管理员', '18888888888', 'admin@chinesemedicine.org', 'SUPER_ADMIN', 1);
