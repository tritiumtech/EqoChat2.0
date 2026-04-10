-- ============================================
-- 8. World（动态/话题）系统
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE world_post (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    author_id BIGINT NOT NULL COMMENT '作者用户ID（暂不含AGENT）',
    content TEXT NOT NULL COMMENT '动态内容',
    reply_count INT DEFAULT 0 COMMENT '回复数',
    upvote_count INT DEFAULT 0 COMMENT '点赞数（冗余）',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DELETED',

    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',

    INDEX idx_world_post_author (author_id),
    INDEX idx_world_post_create_time (create_time),
    INDEX idx_world_post_upvote (upvote_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='世界动态';

CREATE TABLE world_topic (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL COMMENT '话题名（不含#）',
    post_count INT DEFAULT 0 COMMENT '动态数（冗余）',
    follower_count INT DEFAULT 0 COMMENT '关注数（冗余）',

    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',

    UNIQUE KEY uk_world_topic_name (name),
    INDEX idx_world_topic_post_count (post_count),
    INDEX idx_world_topic_follower_count (follower_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='世界话题';

CREATE TABLE world_post_topic (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,

    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',

    UNIQUE KEY uk_world_post_topic (post_id, topic_id),
    INDEX idx_world_post_topic_topic (topic_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态-话题关联';

CREATE TABLE world_post_upvote (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',

    UNIQUE KEY uk_world_post_upvote (post_id, user_id),
    INDEX idx_world_upvote_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态点赞';

CREATE TABLE world_topic_follow (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',

    UNIQUE KEY uk_world_topic_follow (topic_id, user_id),
    INDEX idx_world_topic_follow_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题关注';

-- 示例数据（用于开发预览；生产可删除）
INSERT INTO world_topic (name, post_count, follower_count, del_token)
VALUES ('backend', 1, 10, '0')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO world_post (author_id, content, reply_count, upvote_count, status, del_token)
VALUES (1, 'Welcome to World! #backend', 0, 0, 'ACTIVE', '0');

INSERT INTO world_post_topic (post_id, topic_id, del_token)
SELECT p.id, t.id, '0'
FROM world_post p
JOIN world_topic t ON t.name = 'backend'
ORDER BY p.id DESC
LIMIT 1;

SET FOREIGN_KEY_CHECKS = 1;

