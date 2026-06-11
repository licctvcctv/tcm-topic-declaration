# 🌿 TCM Topic Declaration Platform

**中医药自选课题申报管理平台** — 基于 Spring Boot 3.3.4 的全栈课题申报管理系统，覆盖机构注册、课题填报、两级审核、专家双盲评审、结果发布和通过课题导出。

---

## Tech Stack

| 层次 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.3.4 + Spring Security + JWT |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8.0 |
| API 文档 | Knife4j 4.5.0 (OpenAPI 3) |
| 前端 | 纯 HTML / CSS / JS (无框架) |
| 构建工具 | Maven 3.9+ |
| Java 版本 | 21 |

---

## Quick Start

### 1. 初始化数据库

```sql
-- 登录 MySQL 后执行
source src/main/resources/db/schema.sql
```

脚本自动创建 `topic_db` 数据库及全部表结构，并初始化超级管理员账号：

- **用户名**: `admin`
- **密码**: `admin123`

### 2. 启动服务

```bash
JWT_SECRET=please-change-this-secret-at-least-32-chars DB_PASSWORD= FILE_UPLOAD_DIR=/tmp/tcm-uploads ./mvnw spring-boot:run
```

> 如果 MySQL 设置了密码，替换 `DB_PASSWORD=` 为 `DB_PASSWORD=your_password`。
> `JWT_SECRET` 必须提供，且长度不少于 32 个字符；生产环境必须使用独立随机值。

### 3. 访问

| 页面 | 地址 |
|------|------|
| 门户首页 | `http://localhost:8080/` |
| 登录 | `http://localhost:8080/login.html` |
| 注册 | `http://localhost:8080/register.html` |
| API 文档 | `http://localhost:8080/doc.html` |

---

## 系统角色

| 角色 | 职责 |
|------|------|
| **SUPER_ADMIN** (超级管理员) | 管理申报周期、审批机构/专家、格式初审、指派专家、发布结果 |
| **ORG_ADMIN** (机构管理员) | 审核本机构用户、审核本机构课题、管理名额 |
| **NORMAL_USER** (申报人) | 填报课题、上传附件、查看审核进度 |
| **EXPERT** (评审专家) | 接受/拒绝评审任务、打分、提交评审意见 |

---

## 业务流程

```
申报人填报课题 ──→ 机构审核 ──→ 超管格式初审 ──→ 专家指派 ──→ 专家评审 ──→ 结果发布
     ↑                ↑                ↑                  ↑              ↑
   草稿可修改       通过/退回       通过/退回       智能回避指派     统计+公告
```

### 状态流转

| 状态码 | 说明 |
|--------|------|
| 0 | 草稿 — 申报人保存未提交 |
| 1 | 待机构审核 — 已提交，等待机构管理员审核 |
| 2 | 待格式审核 — 机构通过，等待超管格式初审 |
| 3 | 机构退回 — 机构审核不通过，退回修改 |
| 4 | 待专家分配 — 格式通过，等待超管指派专家 |
| 5 | 专家评审中 — 已指派专家，等待打分 |
| 6 | 评审结束待发布 — 专家意见已提交完毕，等待超管发布 |
| 7 | 格式退回 — 超管格式审核不通过，终止 |
| 8 | 已发布 — 超管已发布最终立项结果 |

---

## API 概览

### 认证 (`/api/auth/**`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录获取 JWT Token |
| POST | `/api/auth/register-user` | 申报人注册 |
| POST | `/api/auth/register-expert` | 专家注册，注册后需超管审核启用 |
| POST | `/api/auth/register-org` | 机构注册 |
| GET  | `/api/auth/active-orgs` | 获取已激活机构列表 |

### 课题 (`/api/topics/**`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/topics/save-submit` | 保存草稿或提交课题 |
| GET  | `/api/topics/my` | 我的课题列表 |
| GET  | `/api/topics/detail/{id}` | 课题详情 |
| GET  | `/api/topics/categories` | 启用中的课题分类 |
| GET  | `/api/topics/active-period` | 当前启用申报周期 |
| GET  | `/api/topics/is-open` | 申报通道是否开放 |

### 机构管理 (`/api/institution/**`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | `/api/institution/users` | 本机构用户列表 |
| POST | `/api/institution/users/audit` | 审核或禁用本机构用户 |
| POST | `/api/institution/users/assign-auth` | 分配申报名额 |
| GET  | `/api/institution/topics` | 本机构课题列表 |
| POST | `/api/institution/topics/audit` | 机构审核课题 |

### 专家 (`/api/expert/**`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | `/api/expert/tasks` | 我的评审任务 |
| POST | `/api/expert/respond` | 接受/拒绝评审 |
| POST | `/api/expert/opinion` | 暂存或提交评分意见 |

### 超级管理员 (`/api/admin/**`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | `/api/admin/orgs` | 机构列表 |
| POST | `/api/admin/orgs/audit` | 审批机构注册 |
| POST | `/api/admin/orgs/quota` | 调整机构申报名额 |
| GET  | `/api/admin/experts` | 专家列表，可按状态筛选 |
| POST | `/api/admin/experts/audit` | 审核或禁用专家 |
| POST | `/api/admin/categories` | 新增或更新课题分类 |
| POST | `/api/admin/categories/{id}/disable` | 禁用课题分类 |
| POST | `/api/admin/topics/audit` | 格式初审 |
| POST | `/api/admin/experts/assign` | 指派专家 |
| POST | `/api/admin/topics/add-second-expert` | 追加第二轮专家 |
| POST | `/api/admin/topics/publish-result` | 发布评审结果 |
| GET  | `/api/admin/topics/export-csv` | 导出已立项通过课题 CSV |

### 文件 (`/api/files/**`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/files/upload` | 上传文件 (PDF/DOCX/图片)，注册材料上传允许匿名 |
| GET  | `/api/files/download/{filename}` | 下载文件 (含权限校验) |

---

## 项目结构

```
src/main/java/com/project/declaration/
├── TopicDeclarationApplication.java    # 启动类
├── common/                             # 公共组件 (异常、响应、全局处理)
├── config/                             # 配置 (Knife4j)
├── controller/                         # REST 控制器
├── dto/                                # 请求/响应 DTO
├── entity/                             # 数据库实体
├── mapper/                             # MyBatis Mapper
├── security/                           # Spring Security + JWT
└── service/                            # 业务逻辑接口 + 实现

src/main/resources/
├── application.yml                     # 应用配置
├── db/schema.sql                       # 数据库建表 + 种子数据
└── static/                             # 前端静态资源
    ├── *.html                          # 7 个页面
    ├── css/                            # 3 个样式文件
    └── js/                             # 10 个 JS 文件
```

---

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_HOST` | `localhost` | MySQL 主机 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `topic_db` | 数据库名 |
| `DB_USERNAME` | `root` | 数据库用户 |
| `DB_PASSWORD` | `root_password` | 数据库密码 |
| `FILE_UPLOAD_DIR` | `./uploads` | 文件上传保存路径 |
| `JWT_SECRET` | 无，必填 | JWT 签名密钥，至少 32 个字符 |
| `JWT_EXPIRATION` | `86400` | Token 过期时间 (秒) |
| `REVIEW_INVITATION_TIMEOUT_HOURS` | `72` | 专家邀请超时自动替换小时数 |

---

## 开发说明

### 首次运行

```bash
# 1. 初始化数据库
mysql -u root < src/main/resources/db/schema.sql

# 2. 测试
./mvnw clean test

# 3. 启动
JWT_SECRET=please-change-this-secret-at-least-32-chars DB_PASSWORD= FILE_UPLOAD_DIR=/tmp/tcm-uploads ./mvnw spring-boot:run
```

### 静态资源修改

前端页面为服务器渲染的 HTML，修改后需重新编译：

```bash
./mvnw clean package -DskipTests
# 或直接复制到 target/classes/static/
cp src/main/resources/static/*.html target/classes/static/
cp src/main/resources/static/js/*.js target/classes/static/js/
cp src/main/resources/static/css/*.css target/classes/static/css/
```

### 测试账号

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `admin123` | 超级管理员 |
| `zjadmin` | `123456` | 浙江省中医药研究院管理员 |
| `liwu` | `123456` | 申报人 (浙江省中医药研究院) |
| `13800000001` | `123456` | 专家 (以手机号登录) |

> 测试账号需要在首次启动后通过注册 + 审批流程创建，或导入完整种子数据。
> 默认 schema.sql 仅包含超级管理员账号。

---

## License

MIT
