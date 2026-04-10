SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- Project 域数据结构
-- ============================================

-- -------------------------------
-- 项目主表：project
-- -------------------------------
CREATE TABLE project (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,

    name VARCHAR(120) NOT NULL COMMENT '项目名称',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/PAUSED/COMPLETED',
    color VARCHAR(32) NOT NULL COMMENT '用于渐变的主色（hex）',

    revenue VARCHAR(64) DEFAULT '$0' COMMENT '收入/收益展示口径（前端展示用）',
    bid BIGINT NOT NULL DEFAULT 0 COMMENT '竞价金额（无小数，前端按业务解释）',
    deposit_paid BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已支付押金',
    deadline VARCHAR(64) DEFAULT '' COMMENT '截止时间（展示口径）',
    progress INT NOT NULL DEFAULT 0 COMMENT '进度百分比 0-100',

    owner_id BIGINT NOT NULL COMMENT '项目所有者 id',
    owner_type VARCHAR(20) NOT NULL COMMENT '项目所有者类型: HUMAN/AGENT',

    -- 当 owner_type=AGENT 时，记录其人类主人（用于当前用户可见性）
    agent_owner_master_id BIGINT NULL,
    agent_fully_authorized BOOLEAN NOT NULL DEFAULT FALSE,

    -- 预留：待处理智能体决策 / 待处理 bid / 待处理转让
    pending_agent_decisions JSON NULL,
    pending_bid_update JSON NULL,
    pending_ownership_transfer JSON NULL,

    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记: 0=未删除, UUID=已删除',

    INDEX idx_project_owner (owner_id, owner_type),
    INDEX idx_project_deadline (deadline),
    INDEX idx_project_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目主表';

-- -------------------------------
-- 项目成员：project_member
-- -------------------------------
CREATE TABLE project_member (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '项目ID',

    member_id BIGINT NOT NULL COMMENT '成员主体ID',
    member_type VARCHAR(20) NOT NULL COMMENT '成员类型: HUMAN/AGENT',

    name VARCHAR(100) NOT NULL COMMENT '成员名称（冗余，避免复杂 join）',
    avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像URL（冗余）',
    is_online BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否在线（展示用）',
    master_id BIGINT NULL COMMENT '当 member_type=AGENT 时，人类主人ID（冗余）',
    credit_score INT NULL COMMENT '信用分（展示用，可选冗余）',

    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记: 0=未删除, UUID=已删除',

    INDEX idx_project_member_project_id (project_id),
    INDEX idx_project_member_member_id (member_id, member_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成员表';

-- -------------------------------
-- 项目任务：project_task
-- -------------------------------
CREATE TABLE project_task (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '项目ID',

    title VARCHAR(200) NOT NULL COMMENT '任务标题',

    assignee_id BIGINT NOT NULL COMMENT '负责人主体ID',
    assignee_type VARCHAR(20) NOT NULL COMMENT '负责人类型: HUMAN/AGENT',
    assignee_name VARCHAR(100) NOT NULL COMMENT '负责人名称（冗余）',

    deadline VARCHAR(64) DEFAULT '' COMMENT '截止时间（展示口径）',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/IN_PROGRESS/COMPLETED',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级: LOW/MEDIUM/HIGH',

    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记: 0=未删除, UUID=已删除',

    INDEX idx_project_task_project_id (project_id),
    INDEX idx_project_task_status (status),
    INDEX idx_project_task_deadline (deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目任务表';

-- -------------------------------
-- 项目支付：project_payment
-- -------------------------------
CREATE TABLE project_payment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '项目ID',

    amount BIGINT NOT NULL COMMENT '金额（无小数）',

    recipient_id BIGINT NOT NULL COMMENT '收款主体ID',
    recipient_type VARCHAR(20) NOT NULL COMMENT '收款主体类型: HUMAN/AGENT',
    recipient_name VARCHAR(100) NOT NULL COMMENT '收款主体名称（冗余）',

    master_wallet VARCHAR(200) DEFAULT NULL COMMENT '主钱包/主负责人（展示用冗余）',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '支付状态: PENDING/PAID/INVOICED',
    date VARCHAR(64) DEFAULT '' COMMENT '日期展示口径（前端展示用）',

    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记: 0=未删除, UUID=已删除',

    INDEX idx_project_payment_project_id (project_id),
    INDEX idx_project_payment_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目支付表';

-- -------------------------------
-- 项目文件：project_file
-- -------------------------------
CREATE TABLE project_file (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '项目ID',

    file_name VARCHAR(200) NOT NULL COMMENT '文件名',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型（前端展示用）',
    download_url VARCHAR(500) DEFAULT NULL COMMENT '下载 URL（文件服务返回）',

    uploaded_by_id BIGINT NOT NULL COMMENT '上传者主体ID',
    uploaded_by_type VARCHAR(20) NOT NULL COMMENT '上传者类型: HUMAN/AGENT',
    uploaded_by_name VARCHAR(100) NOT NULL COMMENT '上传者名称（冗余）',

    size VARCHAR(50) DEFAULT NULL COMMENT '文件大小展示口径（展示用冗余）',
    date VARCHAR(64) DEFAULT '' COMMENT '时间展示口径（前端展示用）',

    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    del_token VARCHAR(64) DEFAULT '0' COMMENT '删除标记: 0=未删除, UUID=已删除',

    INDEX idx_project_file_project_id (project_id),
    INDEX idx_project_file_uploaded_by (uploaded_by_id, uploaded_by_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目文件表';

SET FOREIGN_KEY_CHECKS = 1;

