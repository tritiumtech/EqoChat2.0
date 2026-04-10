-- ============================================
-- World 回复树 & 回复点赞扩展
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 为 world_post_reply 增加父回复与点赞计数字段
ALTER TABLE world_post_reply
  ADD COLUMN parent_id BIGINT NULL COMMENT '父回复ID，NULL 表示顶层评论' AFTER post_id,
  ADD COLUMN upvote_count INT DEFAULT 0 COMMENT '回复点赞数（冗余）' AFTER content,
  ADD INDEX idx_world_post_reply_post_parent (post_id, parent_id),
  ADD INDEX idx_world_post_reply_author (author_id);

-- 回复点赞表，结构对齐 world_post_upvote
CREATE TABLE IF NOT EXISTS world_post_reply_upvote (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reply_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',

    UNIQUE KEY uk_world_post_reply_upvote (reply_id, user_id),
    INDEX idx_world_reply_upvote_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='世界动态回复点赞';

SET FOREIGN_KEY_CHECKS = 1;

