-- Sprint 3A: World reply authors use canonical subject identity.
SET @db_name = DATABASE();

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_reply' AND COLUMN_NAME = 'author_type') = 0,
    'ALTER TABLE world_post_reply ADD COLUMN author_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Reply author subject type: HUMAN/AGENT/SYSTEM'' AFTER author_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE world_post_reply
SET author_type = 'HUMAN'
WHERE author_type IS NULL
   OR author_type = ''
   OR author_type = 'USER';

UPDATE world_post
SET author_type = 'HUMAN'
WHERE author_type IS NULL
   OR author_type = ''
   OR author_type = 'USER';

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_reply' AND INDEX_NAME = 'idx_world_reply_author_subject') = 0,
    'ALTER TABLE world_post_reply ADD INDEX idx_world_reply_author_subject (author_id, author_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE world_post_reply
    MODIFY COLUMN author_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Reply author subject type: HUMAN/AGENT/SYSTEM';

ALTER TABLE world_post
    MODIFY COLUMN author_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Post author subject type: HUMAN/AGENT/SYSTEM';
