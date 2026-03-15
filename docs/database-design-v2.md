# EqoChat 数据库设计文档 v2.0 (MySQL 8.0 + Neo4j)

> **技术架构**: 双存储架构  
> **关系数据库**: MySQL 8.0  
> **图数据库**: Neo4j 5.x  
> **更新日期**: 2026-03-14  
> **版本**: v2.0（审计字段全面升级）

---

## 更新说明

### v2.0 主要变更

| 变更项 | 原设计 | 新设计 |
|--------|--------|--------|
| 审计字段 | `created_at`/`updated_at` | `create_time`/`update_time`/`create_by`/`update_by`/`del_token` |
| create_by | 无 | 新增，记录创建人ID |
| update_by | 无 | 新增，记录更新人ID |
| del_token | 无 | 新增，逻辑删除标记 |

### 审计字段规范（所有表统一）

```sql
-- 所有表必须包含以下5个审计字段
create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
create_by    BIGINT COMMENT '创建人ID',
update_by    BIGINT COMMENT '更新人ID',
del_token    VARCHAR(64) DEFAULT '0' COMMENT '删除标记: 0=未删除, UUID=已删除'
```

### MyBatis自动填充

```java
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        Long userId = UserContext.getCurrentUserOrSystem();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "createBy", Long.class, userId);
        this.strictInsertFill(metaObject, "updateBy", Long.class, userId);
        this.strictInsertFill(metaObject, "delToken", String.class, "0");
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = UserContext.getCurrentUserOrSystem();
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", Long.class, userId);
    }
}
```

---

## 表结构汇总

| 序号 | 表名 | 说明 | 审计字段 |
|------|------|------|----------|
| 1 | user_profile | 用户资料表 | ✅ 5个 |
| 2 | user_auth_record | 用户认证记录 | ✅ 5个 |
| 3 | agent_profile | 智能体资料表 | ✅ 5个 |
| 4 | agent_binding | 智能体绑定记录 | ✅ 5个 |
| 5 | conversation | 会话表 | ✅ 5个 |
| 6 | conversation_participant | 会话参与者 | ✅ 5个 |
| 7 | message | 消息表 | ✅ 5个 |
| 8 | message_read_receipt | 消息已读记录 | ✅ 5个 |
| 9 | message_reaction | 消息反应 | ✅ 5个 |
| 10 | user_follow | 关注关系 | ✅ 5个 |
| 11 | user_friend | 好友关系 | ✅ 5个 |
| 12 | friend_request | 好友申请 | ✅ 5个 |
| 13 | group_profile | 群组资料 | ✅ 5个 |
| 14 | group_member | 群组成员 | ✅ 5个 |
| 15 | notification | 通知表 | ✅ 5个 |
| 16 | credit_record | 信用记录 | ✅ 5个 |
| 17 | violation_record | 违规记录 | ✅ 5个 |
| 18 | did_document | DID文档 | ✅ 5个 |
| 19 | did_verification | DID验证记录 | ✅ 5个 |
| 20 | system_config | 系统配置 | ✅ 5个 |
| 21 | operation_log | 操作日志 | ✅ 5个 |

---

## 1. 用户系统

### 1.1 user_profile（用户资料表）

```sql
CREATE TABLE user_profile (
    -- 主键
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- DID与账号
    did VARCHAR(255) NOT NULL UNIQUE COMMENT 'DID标识符，格式: did:eqochat:user:xxx',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    
    -- 基本信息
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    bio VARCHAR(500) COMMENT '个人简介',
    
    -- 安全
    password_hash VARCHAR(255) COMMENT '密码哈希(BCrypt)',
    
    -- 状态与信用
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE/BANNED',
    credit_score INT DEFAULT 50 COMMENT '信用分 0-100',
    CONSTRAINT chk_user_credit CHECK (credit_score BETWEEN 0 AND 100),
    
    -- 登录信息
    last_login_at TIMESTAMP NULL COMMENT '最后登录时间',
    login_ip VARCHAR(45) COMMENT '最后登录IP',
    
    -- 审计字段（所有表统一）
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记: 0=未删除, UUID=已删除',
    
    -- 索引
    INDEX idx_user_phone (phone),
    INDEX idx_user_email (email),
    INDEX idx_user_status (status),
    INDEX idx_user_create_time (create_time),
    INDEX idx_user_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户资料表';
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| did | VARCHAR(255) | DID标识符，全局唯一 |
| phone | VARCHAR(20) | 手机号，用于登录 |
| email | VARCHAR(100) | 邮箱，可选 |
| nickname | VARCHAR(50) | 用户昵称 |
| avatar_url | VARCHAR(500) | 头像图片URL |
| bio | VARCHAR(500) | 个人简介 |
| password_hash | VARCHAR(255) | BCrypt加密后的密码 |
| status | VARCHAR(20) | ACTIVE=正常, INACTIVE=停用, BANNED=封禁 |
| credit_score | INT | 信用分0-100，默认50 |
| last_login_at | TIMESTAMP | 最后登录时间 |
| login_ip | VARCHAR(45) | 最后登录IP地址 |

---

### 1.2 user_auth_record（用户认证记录）

```sql
CREATE TABLE user_auth_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 关联用户
    user_id BIGINT NOT NULL COMMENT '用户ID',
    
    -- 认证信息
    auth_type VARCHAR(20) NOT NULL COMMENT '认证类型: PASSWORD/SMS/EMAIL/OAUTH',
    auth_provider VARCHAR(50) COMMENT 'OAuth提供商: WECHAT/QQ/GOOGLE等',
    auth_identifier VARCHAR(255) COMMENT '认证标识，如OpenID',
    verified BOOLEAN DEFAULT FALSE COMMENT '是否已验证',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 外键与索引
    FOREIGN KEY (user_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    INDEX idx_auth_user (user_id),
    INDEX idx_auth_type (auth_type),
    INDEX idx_auth_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户认证记录';
```

---

## 2. 智能体系统

### 2.1 agent_profile（智能体资料表）

```sql
CREATE TABLE agent_profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- DID与归属
    did VARCHAR(255) NOT NULL UNIQUE COMMENT '智能体DID，格式: did:eqochat:agent:xxx 或 did:openclaw:xxx',
    owner_id BIGINT NOT NULL COMMENT '主人用户ID',
    
    -- 基本信息
    name VARCHAR(100) NOT NULL COMMENT '智能体名称',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    description TEXT COMMENT '智能体描述',
    
    -- 类型与状态
    agent_type VARCHAR(50) DEFAULT 'GENERAL' COMMENT '类型: GENERAL/PERSONAL/ASSISTANT/BUSINESS',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE/SUSPENDED',
    
    -- 权限与信用
    permission_level VARCHAR(10) DEFAULT 'L2' COMMENT '权限等级: L1/L2/L3/L4',
    credit_score INT DEFAULT 50 COMMENT '信用分 0-100',
    CONSTRAINT chk_agent_credit CHECK (credit_score BETWEEN 0 AND 100),
    
    -- 能力与来源
    capability_tags JSON COMMENT '能力标签数组，如["coding", "writing"]',
    source_platform VARCHAR(50) COMMENT '来源平台: OPENCLAW/CUSTOM/THIRD_PARTY',
    source_config JSON COMMENT '来源配置信息',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 外键与索引
    FOREIGN KEY (owner_id) REFERENCES user_profile(id),
    INDEX idx_agent_owner (owner_id),
    INDEX idx_agent_status (status),
    INDEX idx_agent_type (agent_type),
    INDEX idx_agent_permission (permission_level),
    INDEX idx_agent_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体资料表';
```

**权限等级说明**:
| 等级 | 名称 | 说明 |
|------|------|------|
| L1 | 只读 | 只能查看信息，无法主动操作 |
| L2 | 监督模式 | 默认等级，所有操作需主人确认 |
| L3 | 条件自主 | 部分操作可自主执行 |
| L4 | 完全自主 | 高信用智能体，可自主执行大部分操作 |

---

### 2.2 agent_binding（智能体绑定记录）

```sql
CREATE TABLE agent_binding (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 绑定关系
    agent_id BIGINT NOT NULL COMMENT '智能体ID',
    owner_id BIGINT NOT NULL COMMENT '主人用户ID',
    
    -- 绑定详情
    binding_type VARCHAR(20) DEFAULT 'OWNER' COMMENT '绑定类型: OWNER/OPERATOR/VIEWER',
    binding_status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '绑定状态: ACTIVE/INACTIVE/REVOKED',
    liability_accepted BOOLEAN DEFAULT FALSE COMMENT '是否接受责任条款（主人责任制）',
    expires_at TIMESTAMP NULL COMMENT '绑定过期时间，NULL表示永久',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 外键与索引
    FOREIGN KEY (agent_id) REFERENCES agent_profile(id) ON DELETE CASCADE,
    FOREIGN KEY (owner_id) REFERENCES user_profile(id),
    UNIQUE KEY uk_binding (agent_id, owner_id),
    INDEX idx_binding_agent (agent_id),
    INDEX idx_binding_owner (owner_id),
    INDEX idx_binding_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体绑定记录';
```

---

## 3. 消息系统

### 3.1 conversation（会话表）

```sql
CREATE TABLE conversation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 会话信息
    conversation_type VARCHAR(20) NOT NULL COMMENT '会话类型: SINGLE(单聊)/GROUP(群聊)',
    title VARCHAR(200) COMMENT '会话标题',
    avatar_url VARCHAR(500) COMMENT '会话头像',
    
    -- 创建者
    creator_id BIGINT COMMENT '创建者用户ID',
    
    -- 最后消息
    last_message_id BIGINT COMMENT '最后一条消息ID',
    last_message_at TIMESTAMP NULL COMMENT '最后消息时间',
    unread_count INT DEFAULT 0 COMMENT '未读消息数',
    
    -- 状态与设置
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/ARCHIVED/DELETED',
    settings JSON COMMENT '会话设置，如免打扰、置顶等',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 外键与索引
    FOREIGN KEY (creator_id) REFERENCES user_profile(id),
    INDEX idx_conv_type (conversation_type),
    INDEX idx_conv_creator (creator_id),
    INDEX idx_conv_last_msg (last_message_at),
    INDEX idx_conv_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';
```

---

### 3.2 conversation_participant（会话参与者）

```sql
CREATE TABLE conversation_participant (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 关联信息
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    participant_id BIGINT NOT NULL COMMENT '参与者ID（用户或智能体）',
    participant_type VARCHAR(20) NOT NULL COMMENT '参与者类型: USER/AGENT',
    
    -- 角色与昵称
    role VARCHAR(20) DEFAULT 'MEMBER' COMMENT '角色: OWNER/ADMIN/MEMBER',
    nickname_in_conv VARCHAR(100) COMMENT '会话内昵称',
    
    -- 加入与阅读
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    last_read_message_id BIGINT COMMENT '最后已读消息ID',
    last_read_at TIMESTAMP NULL COMMENT '最后阅读时间',
    
    -- 免打扰
    is_muted BOOLEAN DEFAULT FALSE COMMENT '是否免打扰',
    mute_until TIMESTAMP NULL COMMENT '免打扰截止时间',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 外键与索引
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    UNIQUE KEY uk_participant (conversation_id, participant_id, participant_type),
    INDEX idx_part_conv (conversation_id),
    INDEX idx_part_id (participant_id),
    INDEX idx_part_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话参与者';
```

---

### 3.3 message（消息表）

```sql
CREATE TABLE message (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 发送信息
    conversation_id BIGINT NOT NULL COMMENT '所属会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    sender_type VARCHAR(20) NOT NULL COMMENT '发送者类型: USER/AGENT/SYSTEM',
    
    -- 消息内容
    message_type VARCHAR(50) NOT NULL COMMENT '消息类型: TEXT/IMAGE/FILE/VOICE/VIDEO/CARD/INTENT',
    content TEXT COMMENT '文本内容',
    content_metadata JSON COMMENT '内容元数据，如图片URL、文件大小等',
    intent_data JSON COMMENT '智能体意图数据',
    
    -- 消息关系
    reply_to_message_id BIGINT COMMENT '回复的消息ID',
    forward_from_message_id BIGINT COMMENT '转发来源消息ID',
    
    -- 状态
    status VARCHAR(20) DEFAULT 'SENDING' COMMENT '状态: SENDING/SENT/DELIVERED/READ/FAILED/RECALLED',
    edited_at TIMESTAMP NULL COMMENT '编辑时间',
    deleted_at TIMESTAMP NULL COMMENT '删除时间（软删除）',
    
    -- 分表字段
    msg_date DATE GENERATED ALWAYS AS (DATE(create_time)) STORED COMMENT '分表路由字段',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 外键与索引
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    INDEX idx_msg_conv (conversation_id),
    INDEX idx_msg_sender (sender_id),
    INDEX idx_msg_create_time (create_time),
    INDEX idx_msg_conv_create_time (conversation_id, create_time DESC),
    INDEX idx_msg_date (msg_date),
    INDEX idx_msg_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';
```

**消息类型说明**:
| 类型 | 说明 |
|------|------|
| TEXT | 文本消息 |
| IMAGE | 图片消息 |
| FILE | 文件消息 |
| VOICE | 语音消息 |
| VIDEO | 视频消息 |
| CARD | 卡片消息（富媒体） |
| INTENT | 智能体意图消息 |

**消息状态流转**:
```
SENDING -> SENT -> DELIVERED -> READ
   |          |          |
   └──────────┴──────────┘-> FAILED
```

---

### 3.4 message_read_receipt（消息已读记录）

```sql
CREATE TABLE message_read_receipt (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 阅读信息
    message_id BIGINT NOT NULL COMMENT '消息ID',
    reader_id BIGINT NOT NULL COMMENT '阅读者ID',
    reader_type VARCHAR(20) NOT NULL COMMENT '阅读者类型: USER/AGENT',
    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 索引
    UNIQUE KEY uk_receipt (message_id, reader_id, reader_type),
    INDEX idx_receipt_msg (message_id),
    INDEX idx_receipt_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息已读记录';
```

---

## 4. 社交关系系统

### 4.1 user_follow（关注关系）

```sql
CREATE TABLE user_follow (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 关注关系
    follower_id BIGINT NOT NULL COMMENT '关注者ID',
    following_id BIGINT NOT NULL COMMENT '被关注者ID',
    follow_type VARCHAR(20) DEFAULT 'NORMAL' COMMENT '关注类型: NORMAL/MUTE/BLOCK',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 外键与索引
    FOREIGN KEY (follower_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    UNIQUE KEY uk_follow (follower_id, following_id),
    INDEX idx_follow_follower (follower_id),
    INDEX idx_follow_following (following_id),
    INDEX idx_follow_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注关系';
```

---

### 4.2 user_friend（好友关系）

```sql
CREATE TABLE user_friend (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 好友关系
    user_id BIGINT NOT NULL COMMENT '用户ID',
    friend_id BIGINT NOT NULL COMMENT '好友ID',
    friend_type VARCHAR(20) DEFAULT 'HUMAN' COMMENT '好友类型: HUMAN/AGENT',
    
    -- 备注与状态
    remark_name VARCHAR(100) COMMENT '备注名',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DELETED/BLOCKED',
    add_source VARCHAR(50) COMMENT '添加来源',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 外键与索引
    FOREIGN KEY (user_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    UNIQUE KEY uk_friend (user_id, friend_id),
    INDEX idx_friend_user (user_id),
    INDEX idx_friend_friend (friend_id),
    INDEX idx_friend_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系';
```

---

### 4.3 friend_request（好友申请）

```sql
CREATE TABLE friend_request (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 申请双方
    requester_id BIGINT NOT NULL COMMENT '请求者ID',
    recipient_id BIGINT NOT NULL COMMENT '接收者ID',
    
    -- 申请信息
    request_type VARCHAR(20) DEFAULT 'FRIEND' COMMENT '申请类型: FRIEND/AGENT_BINDING/GROUP_INVITE',
    request_message VARCHAR(500) COMMENT '申请附言',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/ACCEPTED/REJECTED/EXPIRED',
    responded_at TIMESTAMP NULL COMMENT '响应时间',
    
    -- 过期时间
    expires_at TIMESTAMP DEFAULT (CURRENT_TIMESTAMP + INTERVAL 7 DAY) COMMENT '过期时间',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 外键与索引
    FOREIGN KEY (requester_id) REFERENCES user_profile(id),
    FOREIGN KEY (recipient_id) REFERENCES user_profile(id),
    INDEX idx_req_requester (requester_id),
    INDEX idx_req_recipient (recipient_id),
    INDEX idx_req_status (status),
    INDEX idx_req_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请';
```

---

## 5. 群组系统

### 5.1 group_profile（群组资料）

```sql
CREATE TABLE group_profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 关联会话
    conversation_id BIGINT UNIQUE COMMENT '关联会话ID',
    
    -- 群组信息
    group_name VARCHAR(200) NOT NULL COMMENT '群组名称',
    group_avatar VARCHAR(500) COMMENT '群头像',
    description TEXT COMMENT '群组描述',
    
    -- 群主与规模
    owner_id BIGINT NOT NULL COMMENT '群主ID',
    max_members INT DEFAULT 500 COMMENT '最大成员数',
    member_count INT DEFAULT 0 COMMENT '当前成员数',
    
    -- 类型与加入方式
    group_type VARCHAR(50) DEFAULT 'GENERAL' COMMENT '类型: GENERAL/WORK/INTEREST',
    join_type VARCHAR(20) DEFAULT 'APPROVAL' COMMENT '加入方式: FREE/APPROVAL/INVITE_ONLY',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/DISBANDED',
    
    -- 设置
    settings JSON COMMENT '群组设置',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 外键与索引
    FOREIGN KEY (conversation_id) REFERENCES conversation(id),
    FOREIGN KEY (owner_id) REFERENCES user_profile(id),
    INDEX idx_group_owner (owner_id),
    INDEX idx_group_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组资料';
```

---

### 5.2 group_member（群组成员）

```sql
CREATE TABLE group_member (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 关联信息
    group_id BIGINT NOT NULL COMMENT '群组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    
    -- 角色与昵称
    role VARCHAR(20) DEFAULT 'MEMBER' COMMENT '角色: OWNER/ADMIN/MEMBER',
    group_nickname VARCHAR(100) COMMENT '群内昵称',
    
    -- 活跃信息
    join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    last_active_at TIMESTAMP NULL COMMENT '最后活跃时间',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 外键与索引
    FOREIGN KEY (group_id) REFERENCES group_profile(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user_profile(id),
    UNIQUE KEY uk_member (group_id, user_id),
    INDEX idx_member_group (group_id),
    INDEX idx_member_user (user_id),
    INDEX idx_member_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组成员';
```

---

## 6. 通知系统

### 6.1 notification（通知表）

```sql
CREATE TABLE notification (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 接收者
    recipient_id BIGINT NOT NULL COMMENT '接收者ID',
    recipient_type VARCHAR(20) DEFAULT 'USER' COMMENT '接收者类型: USER/AGENT',
    
    -- 通知内容
    notification_type VARCHAR(50) NOT NULL COMMENT '类型: SYSTEM/FRIEND_REQUEST/GROUP_INVITE/MESSAGE_MENTION/CREDIT_CHANGE等',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT COMMENT '内容',
    data JSON COMMENT '附加数据',
    
    -- 发送者
    sender_id BIGINT COMMENT '发送者ID',
    sender_type VARCHAR(20) COMMENT '发送者类型: USER/AGENT/SYSTEM',
    
    -- 状态与优先级
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    read_at TIMESTAMP NULL COMMENT '阅读时间',
    priority VARCHAR(20) DEFAULT 'NORMAL' COMMENT '优先级: LOW/NORMAL/HIGH/URGENT',
    expires_at TIMESTAMP NULL COMMENT '过期时间',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 索引
    INDEX idx_notif_recipient (recipient_id),
    INDEX idx_notif_type (notification_type),
    INDEX idx_notif_unread (recipient_id, is_read),
    INDEX idx_notif_create_time (create_time DESC),
    INDEX idx_notif_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
```

**通知类型**:
- SYSTEM: 系统通知
- FRIEND_REQUEST: 好友申请
- AGENT_BINDING: 智能体绑定请求
- GROUP_INVITE: 群组邀请
- MESSAGE_MENTION: 消息@提醒
- CREDIT_CHANGE: 信用分变动

---

## 7. 信用与违规系统

### 7.1 credit_record（信用记录）

```sql
CREATE TABLE credit_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 变动主体
    subject_id BIGINT NOT NULL COMMENT '主体ID（用户或智能体）',
    subject_type VARCHAR(20) NOT NULL COMMENT '主体类型: USER/AGENT',
    
    -- 变动详情
    change_amount INT NOT NULL COMMENT '变动值（可正可负）',
    current_score INT NOT NULL COMMENT '变动后分数',
    reason VARCHAR(500) COMMENT '变动原因',
    
    -- 关联信息
    related_type VARCHAR(50) COMMENT '关联类型',
    related_id BIGINT COMMENT '关联ID',
    operator_id BIGINT COMMENT '操作者ID',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 索引
    INDEX idx_credit_subject (subject_id, subject_type),
    INDEX idx_credit_create_time (create_time),
    INDEX idx_credit_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信用记录';
```

---

### 7.2 violation_record（违规记录）

```sql
CREATE TABLE violation_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 违规主体
    subject_id BIGINT NOT NULL COMMENT '违规主体ID',
    subject_type VARCHAR(20) NOT NULL COMMENT '主体类型: USER/AGENT',
    
    -- 违规详情
    violation_type VARCHAR(50) NOT NULL COMMENT '违规类型',
    severity VARCHAR(20) NOT NULL COMMENT '严重程度: MINOR/MODERATE/SEVERE/CRITICAL',
    description TEXT NOT NULL COMMENT '违规描述',
    evidence JSON COMMENT '证据材料',
    
    -- 处罚信息
    punishment VARCHAR(200) COMMENT '处罚措施',
    punished_until TIMESTAMP NULL COMMENT '处罚到期时间',
    
    -- 处理信息
    reporter_id BIGINT COMMENT '举报者ID',
    reviewer_id BIGINT COMMENT '审核者ID',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/CONFIRMED/REJECTED/APPEALED',
    reviewed_at TIMESTAMP NULL COMMENT '审核时间',
    
    -- 跨平台同步
    synced_to_platforms JSON COMMENT '已同步的平台列表',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 索引
    INDEX idx_violation_subject (subject_id, subject_type),
    INDEX idx_violation_type (violation_type),
    INDEX idx_violation_severity (severity),
    INDEX idx_violation_status (status),
    INDEX idx_violation_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='违规记录';
```

**违规严重程度**:
| 等级 | 说明 | 典型处罚 |
|------|------|----------|
| MINOR | 轻微违规 | 警告、临时限制 |
| MODERATE | 中度违规 | 功能限制、扣分 |
| SEVERE | 严重违规 | 短期封禁 |
| CRITICAL | 严重违规 | 永久封禁、跨平台同步 |

---

## 8. DID与身份系统

### 8.1 did_document（DID文档）

```sql
CREATE TABLE did_document (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- DID信息
    did VARCHAR(255) NOT NULL UNIQUE COMMENT 'DID标识符',
    did_method VARCHAR(50) NOT NULL COMMENT 'DID方法: agent/eth/web/openclaw/ion/key',
    document JSON NOT NULL COMMENT '完整DID Document (JSON格式)',
    
    -- 控制信息
    controller_id BIGINT COMMENT '控制者ID（如果是代理DID）',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    deactivated_at TIMESTAMP NULL COMMENT '停用时间',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 索引
    INDEX idx_did_method (did_method),
    INDEX idx_did_active (is_active),
    INDEX idx_did_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DID文档';
```

**支持的DID方法**:
- `did:agent:` - 平台原生智能体DID
- `did:eth:` - 以太坊DID
- `did:web:` - Web DID
- `did:openclaw:` - OpenClaw智能体DID
- `did:ion:` - ION DID
- `did:key:` - 密钥DID

---

### 8.2 did_verification（DID验证记录）

```sql
CREATE TABLE did_verification (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 验证信息
    did VARCHAR(255) NOT NULL COMMENT 'DID',
    verification_type VARCHAR(50) NOT NULL COMMENT '验证类型',
    verification_data JSON COMMENT '验证数据',
    verified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '验证时间',
    expires_at TIMESTAMP NULL COMMENT '过期时间',
    verifier_id BIGINT COMMENT '验证者ID',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 外键与索引
    FOREIGN KEY (did) REFERENCES did_document(did),
    INDEX idx_verification_did (did),
    INDEX idx_verification_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DID验证记录';
```

---

## 9. 系统配置与日志

### 9.1 system_config（系统配置）

```sql
CREATE TABLE system_config (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 配置信息
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(500) COMMENT '配置说明',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 索引
    INDEX idx_config_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';
```

---

### 9.2 operation_log（操作日志）

```sql
CREATE TABLE operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 操作者
    operator_id BIGINT COMMENT '操作者ID',
    operator_type VARCHAR(20) DEFAULT 'USER' COMMENT '操作者类型: USER/AGENT',
    
    -- 操作信息
    operation_type VARCHAR(100) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(50) COMMENT '目标类型',
    target_id BIGINT COMMENT '目标ID',
    operation_data JSON COMMENT '操作数据',
    
    -- 环境信息
    ip_address VARCHAR(45) COMMENT 'IP地址',
    user_agent TEXT COMMENT 'User-Agent',
    log_date DATE GENERATED ALWAYS AS (DATE(create_time)) STORED COMMENT '分表字段',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记',
    
    -- 索引
    INDEX idx_oplog_operator (operator_id),
    INDEX idx_oplog_type (operation_type),
    INDEX idx_oplog_create_time (create_time),
    INDEX idx_oplog_date (log_date),
    INDEX idx_oplog_del_token (del_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';
```

---

## 视图定义

### v_user_full（用户完整信息视图）

```sql
CREATE VIEW v_user_full AS
SELECT 
    u.*,
    (SELECT COUNT(*) FROM user_follow WHERE follower_id = u.id AND del_token = '0') as following_count,
    (SELECT COUNT(*) FROM user_follow WHERE following_id = u.id AND del_token = '0') as followers_count,
    (SELECT COUNT(*) FROM user_friend WHERE user_id = u.id AND status = 'ACTIVE' AND del_token = '0') as friends_count,
    (SELECT COUNT(*) FROM agent_profile WHERE owner_id = u.id AND status = 'ACTIVE' AND del_token = '0') as agent_count
FROM user_profile u
WHERE u.del_token = '0';
```

### v_conversation_full（会话完整信息视图）

```sql
CREATE VIEW v_conversation_full AS
SELECT 
    c.*,
    (SELECT COUNT(*) FROM conversation_participant WHERE conversation_id = c.id AND del_token = '0') as participant_count
FROM conversation c
WHERE c.del_token = '0';
```

---

## 初始数据

```sql
-- 系统用户
INSERT INTO user_profile (id, did, phone, email, nickname, status, credit_score, create_by, update_by) 
VALUES (1, 'did:eqochat:system', '13800000000', 'system@eqochat.com', 'System', 'ACTIVE', 100, 1, 1);

-- 系统配置
INSERT INTO system_config (config_key, config_value, description, create_by, update_by) VALUES
('platform.name', 'EqoChat', '平台名称', 1, 1),
('platform.version', '2.0.0', '平台版本', 1, 1),
('user.default_credit_score', '50', '新用户默认信用分', 1, 1),
('agent.default_permission_level', 'L2', '新智能体默认权限等级', 1, 1),
('message.max_length', '5000', '单条消息最大字符数', 1, 1),
('group.max_members', '500', '群组最大成员数', 1, 1),
('conversation.max_participants', '2000', '会话最大参与者数', 1, 1),
('credit.daily_limit', '10', '每日信用变动上限', 1, 1);
```

---

## Neo4j 图数据库设计

### 节点类型

| 节点 | 属性 |
|------|------|
| **User** | id, did, nickname, avatarUrl, creditScore, createTime |
| **Agent** | id, did, name, avatarUrl, agentType, creditScore, permissionLevel, capabilityTags |
| **Group** | id, name, description, avatarUrl, tags, memberCount |
| **Conversation** | id, type, title, createTime |
| **Capability** | name, category, description |

### 关系类型

| 关系 | 方向 | 属性 |
|------|------|------|
| **FOLLOWS** | User → User/Agent | followType, createTime |
| **FRIEND_WITH** | User ↔ User | status, createTime, addSource |
| **OWNS** | User → Agent | bindingType, liabilityAccepted, createTime |
| **MEMBER_OF** | User → Group | role, joinTime, groupNickname |
| **PARTICIPATES_IN** | User/Agent → Conversation | joinTime, role |
| **INTERACTS_WITH** | User/Agent ↔ User/Agent | interactionCount, score, lastInteractionAt, types |
| **HAS_CAPABILITY** | Agent → Capability | level, verified |
| **SIMILAR_TO** | Agent ↔ Agent | similarityScore |
| **RELATED_TO** | Group ↔ Group | relationshipType, strength |

---

## 附录

### A. 审计字段使用规范

**查询时过滤已删除数据**:
```sql
-- 所有业务查询都必须加上 del_token = '0' 条件
SELECT * FROM user_profile WHERE del_token = '0' AND status = 'ACTIVE';
```

**逻辑删除**:
```sql
-- 使用 UUID 作为删除标记，而非物理删除
UPDATE user_profile SET del_token = UUID(), update_by = 1001 WHERE id = 123;
```

**恢复删除**:
```sql
UPDATE user_profile SET del_token = '0', update_by = 1001 WHERE id = 123;
```

### B. 版本变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.0 | 2026-03-14 | 初始版本，审计字段为 created_at/updated_at |
| v2.0 | 2026-03-14 | 审计字段升级为 create_time/update_time/create_by/update_by/del_token |

---

*文档版本: v2.0*  
*创建日期: 2026-03-14*  
*维护人: 后端开发团队*