-- ============================================
-- EqoChat 2.0 数据库设计 (MySQL 8.0) - 含审计字段
-- 所有表统一包含: create_time, update_time, del_token
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 用户系统 (User System)
-- ============================================

CREATE TABLE user_profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    did VARCHAR(255) NOT NULL UNIQUE COMMENT 'DID标识符',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    locale VARCHAR(20) DEFAULT 'zh-CN' COMMENT '语言偏好',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    bio VARCHAR(500) COMMENT '个人简介',
    password_hash VARCHAR(255) COMMENT '密码哈希',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE/BANNED',
    credit_score INT DEFAULT 50 COMMENT '信用分0-100',
    CHECK (credit_score BETWEEN 0 AND 100),
    last_login_at TIMESTAMP NULL COMMENT '最后登录时间',
    login_ip VARCHAR(45) COMMENT '登录IP',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记: 0=未删除, UUID=已删除',
    
    INDEX idx_user_phone (phone),
    INDEX idx_user_email (email),
    INDEX idx_user_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户资料表';

CREATE TABLE user_auth_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    auth_type VARCHAR(20) NOT NULL COMMENT '认证类型: PASSWORD/SMS/EMAIL/OAUTH',
    auth_provider VARCHAR(50) COMMENT 'OAuth提供商',
    auth_identifier VARCHAR(255) COMMENT '认证标识',
    verified BOOLEAN DEFAULT FALSE,
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    FOREIGN KEY (user_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    INDEX idx_auth_type (auth_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户认证记录';

-- ============================================
-- 2. 智能体系统 (Agent System)
-- ============================================

CREATE TABLE agent_profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    did VARCHAR(255) NOT NULL UNIQUE COMMENT '智能体DID',
    owner_id BIGINT NOT NULL COMMENT '主人用户ID',
    name VARCHAR(100) NOT NULL COMMENT '智能体名称',
    avatar_url VARCHAR(500) COMMENT '头像',
    description TEXT COMMENT '描述',
    agent_type VARCHAR(50) DEFAULT 'GENERAL' COMMENT '类型: GENERAL/PERSONAL/ASSISTANT/BUSINESS',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE/SUSPENDED',
    permission_level VARCHAR(10) DEFAULT 'L2' COMMENT '权限等级: L1/L2/L3/L4',
    credit_score INT DEFAULT 50 COMMENT '信用分0-100',
    CHECK (credit_score BETWEEN 0 AND 100),
    capability_tags JSON COMMENT '能力标签数组',
    source_platform VARCHAR(50) COMMENT '来源平台: OPENCLAW/CUSTOM/THIRD_PARTY',
    source_config JSON COMMENT '来源配置',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    FOREIGN KEY (owner_id) REFERENCES user_profile(id),
    INDEX idx_agent_type (agent_type),
    INDEX idx_agent_permission (permission_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体资料表';

CREATE TABLE agent_binding (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL COMMENT '智能体ID',
    owner_id BIGINT NOT NULL COMMENT '主人ID',
    binding_type VARCHAR(20) DEFAULT 'OWNER' COMMENT '绑定类型: OWNER/OPERATOR/VIEWER',
    binding_status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '绑定状态',
    liability_accepted BOOLEAN DEFAULT FALSE COMMENT '责任确认',
    expires_at TIMESTAMP NULL COMMENT '绑定过期时间',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    FOREIGN KEY (agent_id) REFERENCES agent_profile(id) ON DELETE CASCADE,
    FOREIGN KEY (owner_id) REFERENCES user_profile(id),
    UNIQUE KEY uk_binding (agent_id, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体绑定记录';

-- ============================================
-- 3. 消息系统 (Messaging System)
-- ============================================

CREATE TABLE conversation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    conversation_type VARCHAR(20) NOT NULL COMMENT '类型: SINGLE/GROUP',
    title VARCHAR(200) COMMENT '会话标题',
    avatar_url VARCHAR(500),
    creator_id BIGINT COMMENT '创建者',
    last_message_id BIGINT COMMENT '最后消息ID',
    last_message_at TIMESTAMP NULL COMMENT '最后消息时间',
    unread_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ARCHIVED/DELETED',
    settings JSON COMMENT '会话设置',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    FOREIGN KEY (creator_id) REFERENCES user_profile(id),
    INDEX idx_conv_type (conversation_type),
    INDEX idx_conv_last_msg (last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

CREATE TABLE conversation_participant (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    participant_id BIGINT NOT NULL COMMENT '参与者ID（用户或智能体）',
    participant_type VARCHAR(20) NOT NULL COMMENT 'USER/AGENT',
    role VARCHAR(20) DEFAULT 'MEMBER' COMMENT 'OWNER/ADMIN/MEMBER',
    nickname_in_conv VARCHAR(100) COMMENT '会话内昵称',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_read_message_id BIGINT,
    last_read_at TIMESTAMP NULL,
    is_muted BOOLEAN DEFAULT FALSE,
    mute_until TIMESTAMP NULL,
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    UNIQUE KEY uk_participant (conversation_id, participant_id, participant_type),
    INDEX idx_part_id (participant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话参与者';

CREATE TABLE message (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    sender_type VARCHAR(20) NOT NULL COMMENT 'USER/AGENT/SYSTEM',
    message_type VARCHAR(50) NOT NULL COMMENT 'TEXT/IMAGE/FILE/VOICE/VIDEO/CARD/INTENT',
    content TEXT COMMENT '文本内容',
    content_metadata JSON COMMENT '内容元数据（图片URL、文件信息等）',
    intent_data JSON COMMENT '意图数据（智能体消息）',
    reply_to_message_id BIGINT,
    forward_from_message_id BIGINT,
    status VARCHAR(20) DEFAULT 'SENDING' COMMENT 'SENDING/SENT/DELIVERED/READ/FAILED',
    edited_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    msg_date DATE GENERATED ALWAYS AS (DATE(create_time)) STORED,
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    INDEX idx_msg_sender (sender_id),
    INDEX idx_msg_create_time (create_time),
    INDEX idx_msg_date (msg_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

CREATE TABLE message_read_receipt (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    reader_id BIGINT NOT NULL,
    reader_type VARCHAR(20) NOT NULL,
    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    UNIQUE KEY uk_receipt (message_id, reader_id, reader_type),
    INDEX idx_receipt_msg (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息已读记录';

CREATE TABLE message_reaction (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL COMMENT '消息ID',
    reactor_id BIGINT NOT NULL,
    reactor_type VARCHAR(20) NOT NULL,
    reaction_type VARCHAR(50) NOT NULL COMMENT 'emoji代码',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    FOREIGN KEY (message_id) REFERENCES message(id) ON DELETE CASCADE,
    UNIQUE KEY uk_reaction (message_id, reactor_id, reactor_type, reaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息反应';

-- ============================================
-- 4. 社交关系系统 (Social Relationship)
-- ============================================

CREATE TABLE user_follow (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL COMMENT '关注者',
    following_id BIGINT NOT NULL COMMENT '被关注者',
    follow_type VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'NORMAL/MUTE/BLOCK',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    FOREIGN KEY (follower_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    UNIQUE KEY uk_follow (follower_id, following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注关系';

CREATE TABLE user_friend (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    friend_id BIGINT NOT NULL COMMENT '好友ID',
    friend_type VARCHAR(20) DEFAULT 'HUMAN' COMMENT 'HUMAN/AGENT',
    remark_name VARCHAR(100) COMMENT '备注名',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DELETED/BLOCKED',
    add_source VARCHAR(50) COMMENT '添加来源',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    FOREIGN KEY (user_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    UNIQUE KEY uk_friend (user_id, friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系';

CREATE TABLE friend_request (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    requester_id BIGINT NOT NULL COMMENT '请求者',
    recipient_id BIGINT NOT NULL COMMENT '接收者',
    request_type VARCHAR(20) DEFAULT 'FRIEND' COMMENT 'FRIEND/AGENT_BINDING/GROUP_INVITE',
    request_message VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED/EXPIRED',
    responded_at TIMESTAMP NULL,
    expires_at TIMESTAMP DEFAULT (CURRENT_TIMESTAMP + INTERVAL 7 DAY),
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    FOREIGN KEY (requester_id) REFERENCES user_profile(id),
    FOREIGN KEY (recipient_id) REFERENCES user_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请';

-- ============================================
-- 5. 群组系统 (Group System)
-- ============================================

CREATE TABLE group_profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT UNIQUE COMMENT '关联会话ID',
    group_name VARCHAR(200) NOT NULL COMMENT '群组名称',
    group_avatar VARCHAR(500),
    description TEXT,
    owner_id BIGINT NOT NULL COMMENT '群主',
    max_members INT DEFAULT 500,
    member_count INT DEFAULT 0,
    group_type VARCHAR(50) DEFAULT 'GENERAL' COMMENT 'GENERAL/WORK/INTEREST',
    join_type VARCHAR(20) DEFAULT 'APPROVAL' COMMENT 'FREE/APPROVAL/INVITE_ONLY',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    settings JSON COMMENT '群组设置',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    FOREIGN KEY (conversation_id) REFERENCES conversation(id),
    FOREIGN KEY (owner_id) REFERENCES user_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组资料';

CREATE TABLE group_member (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL COMMENT '群组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) DEFAULT 'MEMBER' COMMENT 'OWNER/ADMIN/MEMBER',
    group_nickname VARCHAR(100),
    join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP NULL,
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    FOREIGN KEY (group_id) REFERENCES group_profile(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user_profile(id),
    UNIQUE KEY uk_member (group_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组成员';

-- ============================================
-- 6. 通知系统 (Notification System)
-- ============================================

CREATE TABLE notification (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL COMMENT '接收者ID',
    recipient_type VARCHAR(20) DEFAULT 'USER' COMMENT 'USER/AGENT',
    notification_type VARCHAR(50) NOT NULL COMMENT '通知类型',
    title VARCHAR(200) NOT NULL,
    content TEXT,
    data JSON COMMENT '附加数据',
    sender_id BIGINT COMMENT '发送者',
    sender_type VARCHAR(20),
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    priority VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'LOW/NORMAL/HIGH/URGENT',
    expires_at TIMESTAMP NULL,
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    INDEX idx_notif_recipient (recipient_id),
    INDEX idx_notif_type (notification_type),
    INDEX idx_notif_unread (recipient_id, is_read),
    INDEX idx_notif_create_time (create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ============================================
-- 7. 信用与违规系统 (Credit & Violation)
-- ============================================

CREATE TABLE credit_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL COMMENT '主体ID',
    subject_type VARCHAR(20) NOT NULL COMMENT 'USER/AGENT',
    change_amount INT NOT NULL COMMENT '变动值（可正可负）',
    current_score INT NOT NULL COMMENT '变动后分数',
    reason VARCHAR(500) COMMENT '变动原因',
    related_type VARCHAR(50) COMMENT '关联类型',
    related_id BIGINT COMMENT '关联ID',
    operator_id BIGINT COMMENT '操作者',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    INDEX idx_credit_subject (subject_id, subject_type),
    INDEX idx_credit_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信用记录';

CREATE TABLE violation_record (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL COMMENT '违规主体ID',
    subject_type VARCHAR(20) NOT NULL COMMENT 'USER/AGENT',
    violation_type VARCHAR(50) NOT NULL COMMENT '违规类型',
    severity VARCHAR(20) NOT NULL COMMENT 'MINOR/MODERATE/SEVERE/CRITICAL',
    description TEXT NOT NULL,
    evidence JSON COMMENT '证据',
    punishment VARCHAR(200) COMMENT '处罚措施',
    punished_until TIMESTAMP NULL COMMENT '处罚到期时间',
    reporter_id BIGINT COMMENT '举报者',
    reviewer_id BIGINT COMMENT '审核者',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/REJECTED/APPEALED',
    reviewed_at TIMESTAMP NULL,
    synced_to_platforms JSON COMMENT '已同步的平台列表',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    INDEX idx_violation_subject (subject_id, subject_type),
    INDEX idx_violation_type (violation_type),
    INDEX idx_violation_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='违规记录';

-- ============================================
-- 8. DID与身份系统 (DID & Identity)
-- ============================================

CREATE TABLE did_document (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    did VARCHAR(255) NOT NULL UNIQUE,
    did_method VARCHAR(50) NOT NULL COMMENT 'agent/eth/web/openclaw/ion/key',
    document JSON NOT NULL COMMENT '完整DID Document',
    controller_id BIGINT COMMENT '控制者（如果是代理DID）',
    is_active BOOLEAN DEFAULT TRUE,
    deactivated_at TIMESTAMP NULL,
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    INDEX idx_did_method (did_method),
    INDEX idx_did_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DID文档';

CREATE TABLE did_verification (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    did VARCHAR(255) NOT NULL,
    verification_type VARCHAR(50) NOT NULL COMMENT '验证类型',
    verification_data JSON COMMENT '验证数据',
    verified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    verifier_id BIGINT COMMENT '验证者',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    FOREIGN KEY (did) REFERENCES did_document(did)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='DID验证记录';

-- ============================================
-- 9. 多语言资源 (I18n)
-- ============================================

CREATE TABLE i18n_text (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resource_key VARCHAR(200) NOT NULL COMMENT '资源键',
    locale VARCHAR(20) NOT NULL COMMENT '语言代码，如 zh-CN/en-US',
    text TEXT NOT NULL COMMENT '翻译内容',
    description VARCHAR(500) COMMENT '说明',
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    UNIQUE KEY uk_i18n_text (resource_key, locale)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='多语言资源';

-- ============================================
-- 10. 系统配置与日志
-- ============================================

CREATE TABLE system_config (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    description VARCHAR(500),
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';

CREATE TABLE operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT,
    operator_type VARCHAR(20) DEFAULT 'USER',
    operation_type VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id BIGINT,
    operation_data JSON,
    ip_address VARCHAR(45),
    user_agent TEXT,
    log_date DATE GENERATED ALWAYS AS (DATE(create_time)) STORED,
    
    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    
    INDEX idx_oplog_operator (operator_id),
    INDEX idx_oplog_type (operation_type),
    INDEX idx_oplog_create_time (create_time),
    INDEX idx_oplog_date (log_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';

-- ============================================
-- 视图
-- ============================================

CREATE VIEW v_user_full AS
SELECT 
    u.*,
    (SELECT COUNT(*) FROM user_follow WHERE follower_id = u.id AND del_token = '0') as following_count,
    (SELECT COUNT(*) FROM user_follow WHERE following_id = u.id AND del_token = '0') as followers_count,
    (SELECT COUNT(*) FROM user_friend WHERE user_id = u.id AND status = 'ACTIVE' AND del_token = '0') as friends_count,
    (SELECT COUNT(*) FROM agent_profile WHERE owner_id = u.id AND status = 'ACTIVE' AND del_token = '0') as agent_count
FROM user_profile u
WHERE u.del_token = '0';

CREATE VIEW v_conversation_full AS
SELECT 
    c.*,
    (SELECT COUNT(*) FROM conversation_participant WHERE conversation_id = c.id AND del_token = '0') as participant_count
FROM conversation c
WHERE c.del_token = '0';

-- ============================================
-- 初始数据
-- ============================================

INSERT INTO user_profile (id, did, phone, email, nickname, status, credit_score, create_time, update_time) VALUES
(1, 'did:eqochat:system', '13800000000', 'system@eqochat.com', 'System', 'ACTIVE', 100, NOW(), NOW());

INSERT INTO system_config (config_key, config_value, description, create_time, update_time) VALUES
('platform.name', 'EqoChat', '平台名称', NOW(), NOW()),
('platform.version', '2.0.0', '平台版本', NOW(), NOW()),
('user.default_credit_score', '50', '新用户默认信用分', NOW(), NOW()),
('agent.default_permission_level', 'L2', '新智能体默认权限等级', NOW(), NOW()),
('message.max_length', '5000', '单条消息最大字符数', NOW(), NOW()),
('group.max_members', '500', '群组最大成员数', NOW(), NOW());

SET FOREIGN_KEY_CHECKS = 1;
