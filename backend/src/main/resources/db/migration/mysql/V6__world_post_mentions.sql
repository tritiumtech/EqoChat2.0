SET NAMES utf8mb4;

CREATE TABLE world_post_mention (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    mentioned_user_id BIGINT NOT NULL,

    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',

    UNIQUE KEY uk_world_post_mention (post_id, mentioned_user_id),
    INDEX idx_world_post_mention_user (mentioned_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='世界动态@提及记录';
