-- ============================================
-- EqoChat 2.0 数据库设计
-- PostgreSQL + Neo4j 双存储架构
-- ============================================

-- 扩展安装
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- 1. 用户系统 (User System)
-- ============================================

CREATE TABLE user_profile (
    id BIGSERIAL PRIMARY KEY,
    did VARCHAR(255) UNIQUE NOT NULL,                    -- DID标识符
    phone VARCHAR(20) UNIQUE,                            -- 手机号
    email VARCHAR(100) UNIQUE,                           -- 邮箱
    nickname VARCHAR(50) NOT NULL,                       -- 昵称
    avatar_url VARCHAR(500),                             -- 头像URL
    bio VARCHAR(500),                                    -- 个人简介
    password_hash VARCHAR(255),                          -- 密码哈希
    status VARCHAR(20) DEFAULT 'ACTIVE',                 -- 状态: ACTIVE/INACTIVE/BANNED
    credit_score INT DEFAULT 50 CHECK (credit_score BETWEEN 0 AND 100),
    last_login_at TIMESTAMP,                             -- 最后登录时间
    login_ip VARCHAR(45),                                -- 登录IP
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_did ON user_profile(did);
CREATE INDEX idx_user_phone ON user_profile(phone);
CREATE INDEX idx_user_email ON user_profile(email);
CREATE INDEX idx_user_status ON user_profile(status);

-- 用户认证记录
CREATE TABLE user_auth_record (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    auth_type VARCHAR(20) NOT NULL,                      -- 认证类型: PASSWORD/SMS/EMAIL/OAUTH
    auth_provider VARCHAR(50),                           -- OAuth提供商
    auth_identifier VARCHAR(255),                        -- 认证标识
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_user ON user_auth_record(user_id);
CREATE INDEX idx_auth_type ON user_auth_record(auth_type);

-- ============================================
-- 2. 智能体系统 (Agent System)
-- ============================================

CREATE TABLE agent_profile (
    id BIGSERIAL PRIMARY KEY,
    did VARCHAR(255) UNIQUE NOT NULL,                    -- 智能体DID
    owner_id BIGINT NOT NULL REFERENCES user_profile(id), -- 主人用户ID
    name VARCHAR(100) NOT NULL,                          -- 智能体名称
    avatar_url VARCHAR(500),                             -- 头像
    description TEXT,                                    -- 描述
    agent_type VARCHAR(50) DEFAULT 'GENERAL',            -- 类型: GENERAL/PERSONAL/ASSISTANT/BUSINESS
    status VARCHAR(20) DEFAULT 'ACTIVE',                 -- 状态: ACTIVE/INACTIVE/SUSPENDED
    permission_level VARCHAR(10) DEFAULT 'L2',           -- 权限等级: L1/L2/L3/L4
    credit_score INT DEFAULT 50 CHECK (credit_score BETWEEN 0 AND 100),
    capability_tags JSONB,                               -- 能力标签数组
    source_platform VARCHAR(50),                         -- 来源平台: OPENCLAW/CUSTOM/THIRD_PARTY
    source_config JSONB,                                 -- 来源配置
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agent_did ON agent_profile(did);
CREATE INDEX idx_agent_owner ON agent_profile(owner_id);
CREATE INDEX idx_agent_status ON agent_profile(status);
CREATE INDEX idx_agent_type ON agent_profile(agent_type);
CREATE INDEX idx_agent_permission ON agent_profile(permission_level);

-- 智能体绑定记录
CREATE TABLE agent_binding (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL REFERENCES agent_profile(id) ON DELETE CASCADE,
    owner_id BIGINT NOT NULL REFERENCES user_profile(id),
    binding_type VARCHAR(20) DEFAULT 'OWNER',            -- 绑定类型: OWNER/OPERATOR/VIEWER
    binding_status VARCHAR(20) DEFAULT 'ACTIVE',         -- 绑定状态
    liability_accepted BOOLEAN DEFAULT FALSE,            -- 责任确认
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,                                -- 绑定过期时间
    UNIQUE(agent_id, owner_id)
);

CREATE INDEX idx_binding_agent ON agent_binding(agent_id);
CREATE INDEX idx_binding_owner ON agent_binding(owner_id);

-- ============================================
-- 3. 消息系统 (Messaging System)
-- ============================================

-- 会话表
CREATE TABLE conversation (
    id BIGSERIAL PRIMARY KEY,
    conversation_type VARCHAR(20) NOT NULL,              -- 类型: SINGLE/GROUP
    title VARCHAR(200),                                  -- 会话标题
    avatar_url VARCHAR(500),
    creator_id BIGINT REFERENCES user_profile(id),       -- 创建者
    last_message_id BIGINT,                              -- 最后消息ID
    last_message_at TIMESTAMP,                           -- 最后消息时间
    unread_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',                 -- ACTIVE/ARCHIVED/DELETED
    settings JSONB,                                      -- 会话设置
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conv_type ON conversation(conversation_type);
CREATE INDEX idx_conv_creator ON conversation(creator_id);
CREATE INDEX idx_conv_last_msg ON conversation(last_message_at);

-- 会话参与者
CREATE TABLE conversation_participant (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    participant_id BIGINT NOT NULL,                      -- 参与者ID（用户或智能体）
    participant_type VARCHAR(20) NOT NULL,               -- USER/AGENT
    role VARCHAR(20) DEFAULT 'MEMBER',                   -- OWNER/ADMIN/MEMBER
    nickname_in_conv VARCHAR(100),                       -- 会话内昵称
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_read_message_id BIGINT,
    last_read_at TIMESTAMP,
    is_muted BOOLEAN DEFAULT FALSE,
    mute_until TIMESTAMP,
    UNIQUE(conversation_id, participant_id, participant_type)
);

CREATE INDEX idx_part_conv ON conversation_participant(conversation_id);
CREATE INDEX idx_part_id ON conversation_participant(participant_id);

-- 消息表（按时间分区）
CREATE TABLE message (
    id BIGSERIAL,
    conversation_id BIGINT NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL,                           -- 发送者ID
    sender_type VARCHAR(20) NOT NULL,                    -- USER/AGENT/SYSTEM
    message_type VARCHAR(50) NOT NULL,                   -- TEXT/IMAGE/FILE/VOICE/VIDEO/CARD/INTENT
    content TEXT,                                        -- 文本内容
    content_metadata JSONB,                              -- 内容元数据（图片URL、文件信息等）
    intent_data JSONB,                                   -- 意图数据（智能体消息）
    reply_to_message_id BIGINT REFERENCES message(id),
    forward_from_message_id BIGINT,
    status VARCHAR(20) DEFAULT 'SENDING',                -- SENDING/SENT/DELIVERED/READ/FAILED
    edited_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 创建月度分区（示例）
CREATE TABLE message_2026_03 PARTITION OF message
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE message_2026_04 PARTITION OF message
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

CREATE INDEX idx_msg_conv ON message(conversation_id);
CREATE INDEX idx_msg_sender ON message(sender_id);
CREATE INDEX idx_msg_created ON message(created_at);
CREATE INDEX idx_msg_conv_created ON message(conversation_id, created_at DESC);

-- 消息已读记录
CREATE TABLE message_read_receipt (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    reader_id BIGINT NOT NULL,
    reader_type VARCHAR(20) NOT NULL,
    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(message_id, reader_id, reader_type)
);

CREATE INDEX idx_receipt_msg ON message_read_receipt(message_id);

-- 消息反应（表情回复）
CREATE TABLE message_reaction (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES message(id) ON DELETE CASCADE,
    reactor_id BIGINT NOT NULL,
    reactor_type VARCHAR(20) NOT NULL,
    reaction_type VARCHAR(50) NOT NULL,                  -- emoji代码
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(message_id, reactor_id, reactor_type, reaction_type)
);

-- ============================================
-- 4. 社交关系系统 (Social Relationship)
-- ============================================

-- 关注关系
CREATE TABLE user_follow (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    following_id BIGINT NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    follow_type VARCHAR(20) DEFAULT 'NORMAL',            -- NORMAL/MUTE/BLOCK
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(follower_id, following_id)
);

CREATE INDEX idx_follow_follower ON user_follow(follower_id);
CREATE INDEX idx_follow_following ON user_follow(following_id);

-- 好友关系
CREATE TABLE user_friend (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    friend_id BIGINT NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    friend_type VARCHAR(20) DEFAULT 'HUMAN',             -- HUMAN/AGENT
    remark_name VARCHAR(100),                            -- 备注名
    status VARCHAR(20) DEFAULT 'ACTIVE',                 -- ACTIVE/DELETED/BLOCKED
    add_source VARCHAR(50),                              -- 添加来源
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, friend_id)
);

CREATE INDEX idx_friend_user ON user_friend(user_id);
CREATE INDEX idx_friend_friend ON user_friend(friend_id);

-- 好友申请
CREATE TABLE friend_request (
    id BIGSERIAL PRIMARY KEY,
    requester_id BIGINT NOT NULL REFERENCES user_profile(id),
    recipient_id BIGINT NOT NULL REFERENCES user_profile(id),
    request_type VARCHAR(20) DEFAULT 'FRIEND',           -- FRIEND/AGENT_BINDING/GROUP_INVITE
    request_message VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING',                -- PENDING/ACCEPTED/REJECTED/EXPIRED
    responded_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP + INTERVAL '7 days'
);

CREATE INDEX idx_req_requester ON friend_request(requester_id);
CREATE INDEX idx_req_recipient ON friend_request(recipient_id);
CREATE INDEX idx_req_status ON friend_request(status);

-- ============================================
-- 5. 群组系统 (Group System)
-- ============================================

CREATE TABLE group_profile (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT UNIQUE REFERENCES conversation(id),
    group_name VARCHAR(200) NOT NULL,
    group_avatar VARCHAR(500),
    description TEXT,
    owner_id BIGINT NOT NULL REFERENCES user_profile(id),
    max_members INT DEFAULT 500,
    member_count INT DEFAULT 0,
    group_type VARCHAR(50) DEFAULT 'GENERAL',            -- GENERAL/WORK/INTEREST
    join_type VARCHAR(20) DEFAULT 'APPROVAL',            -- FREE/APPROVAL/INVITE_ONLY
    status VARCHAR(20) DEFAULT 'ACTIVE',
    settings JSONB,                                      -- 群组设置
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 群组成员（扩展conversation_participant）
CREATE TABLE group_member (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES group_profile(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES user_profile(id),
    role VARCHAR(20) DEFAULT 'MEMBER',                   -- OWNER/ADMIN/MEMBER
    group_nickname VARCHAR(100),
    join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP,
    UNIQUE(group_id, user_id)
);

CREATE INDEX idx_member_group ON group_member(group_id);
CREATE INDEX idx_member_user ON group_member(user_id);

-- ============================================
-- 6. 通知系统 (Notification System)
-- ============================================

CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES user_profile(id),
    recipient_type VARCHAR(20) DEFAULT 'USER',           -- USER/AGENT
    notification_type VARCHAR(50) NOT NULL,              -- 通知类型
    title VARCHAR(200) NOT NULL,
    content TEXT,
    data JSONB,                                          -- 附加数据
    sender_id BIGINT,                                    -- 发送者
    sender_type VARCHAR(20),
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    priority VARCHAR(20) DEFAULT 'NORMAL',               -- LOW/NORMAL/HIGH/URGENT
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notif_recipient ON notification(recipient_id);
CREATE INDEX idx_notif_type ON notification(notification_type);
CREATE INDEX idx_notif_unread ON notification(recipient_id, is_read);
CREATE INDEX idx_notif_created ON notification(created_at DESC);

-- 通知类型枚举
-- SYSTEM: 系统通知
-- FRIEND_REQUEST: 好友申请
-- AGENT_BINDING: 智能体绑定
-- GROUP_INVITE: 群组邀请
-- MESSAGE_MENTION: 消息@提醒
-- CREDIT_CHANGE: 信用分变动

-- ============================================
-- 7. 信用与违规系统 (Credit & Violation System)
-- ============================================

-- 信用分变动记录
CREATE TABLE credit_record (
    id BIGSERIAL PRIMARY KEY,
    subject_id BIGINT NOT NULL,                          -- 主体ID
    subject_type VARCHAR(20) NOT NULL,                   -- USER/AGENT
    change_amount INT NOT NULL,                          -- 变动值（可正可负）
    current_score INT NOT NULL,                          -- 变动后分数
    reason VARCHAR(500),                                 -- 变动原因
    related_type VARCHAR(50),                            -- 关联类型
    related_id BIGINT,                                   -- 关联ID
    operator_id BIGINT,                                  -- 操作者
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_credit_subject ON credit_record(subject_id, subject_type);
CREATE INDEX idx_credit_created ON credit_record(created_at);

-- 违规记录
CREATE TABLE violation_record (
    id BIGSERIAL PRIMARY KEY,
    subject_id BIGINT NOT NULL,                          -- 违规主体ID
    subject_type VARCHAR(20) NOT NULL,                   -- USER/AGENT
    violation_type VARCHAR(50) NOT NULL,                 -- 违规类型
    severity VARCHAR(20) NOT NULL,                       -- MINOR/MODERATE/SEVERE/CRITICAL
    description TEXT NOT NULL,
    evidence JSONB,                                      -- 证据
    punishment VARCHAR(200),                             -- 处罚措施
    punished_until TIMESTAMP,                            -- 处罚到期时间
    reporter_id BIGINT,                                  -- 举报者
    reviewer_id BIGINT,                                  -- 审核者
    status VARCHAR(20) DEFAULT 'PENDING',                -- PENDING/CONFIRMED/REJECTED/APPEALED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    synced_to_platforms TEXT[]                           -- 已同步的平台列表
);

CREATE INDEX idx_violation_subject ON violation_record(subject_id, subject_type);
CREATE INDEX idx_violation_type ON violation_record(violation_type);
CREATE INDEX idx_violation_severity ON violation_record(severity);
CREATE INDEX idx_violation_status ON violation_record(status);

-- ============================================
-- 8. DID与身份系统 (DID & Identity)
-- ============================================

CREATE TABLE did_document (
    id BIGSERIAL PRIMARY KEY,
    did VARCHAR(255) UNIQUE NOT NULL,
    did_method VARCHAR(50) NOT NULL,                     -- agent/eth/web/openclaw/ion/key
    document JSONB NOT NULL,                             -- 完整DID Document
    controller_id BIGINT,                                -- 控制者（如果是代理DID）
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deactivated_at TIMESTAMP
);

CREATE INDEX idx_did_method ON did_document(did_method);
CREATE INDEX idx_did_active ON did_document(is_active);

-- DID验证记录
CREATE TABLE did_verification (
    id BIGSERIAL PRIMARY KEY,
    did VARCHAR(255) NOT NULL REFERENCES did_document(did),
    verification_type VARCHAR(50) NOT NULL,              -- 验证类型
    verification_data JSONB,                             -- 验证数据
    verified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    verifier_id BIGINT                                   -- 验证者
);

-- ============================================
-- 9. 系统配置与日志
-- ============================================

-- 系统配置
CREATE TABLE system_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT,
    description VARCHAR(500),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 操作日志
CREATE TABLE operation_log (
    id BIGSERIAL PRIMARY KEY,
    operator_id BIGINT,
    operator_type VARCHAR(20) DEFAULT 'USER',
    operation_type VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id BIGINT,
    operation_data JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

CREATE INDEX idx_oplog_operator ON operation_log(operator_id);
CREATE INDEX idx_oplog_type ON operation_log(operation_type);
CREATE INDEX idx_oplog_created ON operation_log(created_at);

-- ============================================
-- 初始数据
-- ============================================

INSERT INTO user_profile (did, phone, email, nickname, status, credit_score) VALUES
('did:eqochat:system', '13800000000', 'system@eqochat.com', 'System', 'ACTIVE', 100);

INSERT INTO system_config (config_key, config_value, description) VALUES
('platform.name', 'EqoChat', '平台名称'),
('platform.version', '2.0.0', '平台版本'),
('user.default_credit_score', '50', '新用户默认信用分'),
('agent.default_permission_level', 'L2', '新智能体默认权限等级'),
('message.max_length', '5000', '单条消息最大字符数'),
('group.max_members', '500', '群组最大成员数');

-- ============================================
-- 视图（常用查询）
-- ============================================

-- 用户完整信息视图
CREATE VIEW v_user_full AS
SELECT 
    u.*,
    (SELECT COUNT(*) FROM user_follow WHERE follower_id = u.id) as following_count,
    (SELECT COUNT(*) FROM user_follow WHERE following_id = u.id) as followers_count,
    (SELECT COUNT(*) FROM user_friend WHERE user_id = u.id AND status = 'ACTIVE') as friends_count,
    (SELECT COUNT(*) FROM agent_profile WHERE owner_id = u.id AND status = 'ACTIVE') as agent_count
FROM user_profile u;

-- 会话完整信息视图
CREATE VIEW v_conversation_full AS
SELECT 
    c.*,
    (SELECT COUNT(*) FROM conversation_participant WHERE conversation_id = c.id) as participant_count,
    (SELECT string_agg(DISTINCT p.participant_id::text, ',') 
     FROM conversation_participant p 
     WHERE p.conversation_id = c.id AND p.participant_type = 'USER') as user_participants
FROM conversation c;
