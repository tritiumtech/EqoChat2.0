SET NAMES utf8mb4;

CREATE TABLE world_post_reply (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,

    -- 审计字段
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',

    INDEX idx_world_post_reply_post_id (post_id),
    INDEX idx_world_post_reply_author_id (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='世界动态回复';

