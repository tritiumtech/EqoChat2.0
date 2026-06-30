-- Sprint 3B: World engagement rows use canonical subject identity.
SET @db_name = DATABASE();

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_mention' AND COLUMN_NAME = 'mentioned_subject_id') = 0,
    'ALTER TABLE world_post_mention ADD COLUMN mentioned_subject_id BIGINT NULL COMMENT ''Mentioned subject id'' AFTER mentioned_user_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_mention' AND COLUMN_NAME = 'mentioned_subject_type') = 0,
    'ALTER TABLE world_post_mention ADD COLUMN mentioned_subject_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Mentioned subject type: HUMAN/AGENT/SYSTEM'' AFTER mentioned_subject_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE world_post_mention
SET mentioned_subject_id = mentioned_user_id,
    mentioned_subject_type = 'HUMAN'
WHERE mentioned_subject_id IS NULL
  AND mentioned_user_id IS NOT NULL;

ALTER TABLE world_post_mention
    MODIFY COLUMN mentioned_user_id BIGINT NULL COMMENT 'Historical mentioned human id; runtime uses mentioned_subject_id/mentioned_subject_type',
    MODIFY COLUMN mentioned_subject_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Mentioned subject type: HUMAN/AGENT/SYSTEM';

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_mention' AND INDEX_NAME = 'uk_world_post_mention') > 0,
    'ALTER TABLE world_post_mention DROP INDEX uk_world_post_mention',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_mention' AND INDEX_NAME = 'uk_world_post_mention_subject') = 0,
    'ALTER TABLE world_post_mention ADD UNIQUE KEY uk_world_post_mention_subject (post_id, mentioned_subject_id, mentioned_subject_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_mention' AND INDEX_NAME = 'idx_world_post_mention_subject') = 0,
    'ALTER TABLE world_post_mention ADD INDEX idx_world_post_mention_subject (mentioned_subject_id, mentioned_subject_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_upvote' AND COLUMN_NAME = 'voter_id') = 0,
    'ALTER TABLE world_post_upvote ADD COLUMN voter_id BIGINT NULL COMMENT ''Voter subject id'' AFTER user_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_upvote' AND COLUMN_NAME = 'voter_type') = 0,
    'ALTER TABLE world_post_upvote ADD COLUMN voter_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Voter subject type: HUMAN/AGENT/SYSTEM'' AFTER voter_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE world_post_upvote
SET voter_id = user_id,
    voter_type = 'HUMAN'
WHERE voter_id IS NULL
  AND user_id IS NOT NULL;

ALTER TABLE world_post_upvote
    MODIFY COLUMN user_id BIGINT NULL COMMENT 'Historical human voter id; runtime uses voter_id/voter_type',
    MODIFY COLUMN voter_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Voter subject type: HUMAN/AGENT/SYSTEM';

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_upvote' AND INDEX_NAME = 'uk_world_post_upvote') > 0,
    'ALTER TABLE world_post_upvote DROP INDEX uk_world_post_upvote',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_upvote' AND INDEX_NAME = 'uk_world_post_upvote_voter') = 0,
    'ALTER TABLE world_post_upvote ADD UNIQUE KEY uk_world_post_upvote_voter (post_id, voter_id, voter_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_upvote' AND INDEX_NAME = 'idx_world_upvote_voter') = 0,
    'ALTER TABLE world_post_upvote ADD INDEX idx_world_upvote_voter (voter_id, voter_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_reply_upvote' AND COLUMN_NAME = 'voter_id') = 0,
    'ALTER TABLE world_post_reply_upvote ADD COLUMN voter_id BIGINT NULL COMMENT ''Voter subject id'' AFTER user_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_reply_upvote' AND COLUMN_NAME = 'voter_type') = 0,
    'ALTER TABLE world_post_reply_upvote ADD COLUMN voter_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Voter subject type: HUMAN/AGENT/SYSTEM'' AFTER voter_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE world_post_reply_upvote
SET voter_id = user_id,
    voter_type = 'HUMAN'
WHERE voter_id IS NULL
  AND user_id IS NOT NULL;

ALTER TABLE world_post_reply_upvote
    MODIFY COLUMN user_id BIGINT NULL COMMENT 'Historical human voter id; runtime uses voter_id/voter_type',
    MODIFY COLUMN voter_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Voter subject type: HUMAN/AGENT/SYSTEM';

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_reply_upvote' AND INDEX_NAME = 'uk_world_post_reply_upvote') > 0,
    'ALTER TABLE world_post_reply_upvote DROP INDEX uk_world_post_reply_upvote',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_reply_upvote' AND INDEX_NAME = 'uk_world_post_reply_upvote_voter') = 0,
    'ALTER TABLE world_post_reply_upvote ADD UNIQUE KEY uk_world_post_reply_upvote_voter (reply_id, voter_id, voter_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post_reply_upvote' AND INDEX_NAME = 'idx_world_reply_upvote_voter') = 0,
    'ALTER TABLE world_post_reply_upvote ADD INDEX idx_world_reply_upvote_voter (voter_id, voter_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_topic_follow' AND COLUMN_NAME = 'follower_id') = 0,
    'ALTER TABLE world_topic_follow ADD COLUMN follower_id BIGINT NULL COMMENT ''Follower subject id'' AFTER user_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_topic_follow' AND COLUMN_NAME = 'follower_type') = 0,
    'ALTER TABLE world_topic_follow ADD COLUMN follower_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Follower subject type: HUMAN/AGENT/SYSTEM'' AFTER follower_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE world_topic_follow
SET follower_id = user_id,
    follower_type = 'HUMAN'
WHERE follower_id IS NULL
  AND user_id IS NOT NULL;

ALTER TABLE world_topic_follow
    MODIFY COLUMN user_id BIGINT NULL COMMENT 'Historical human follower id; runtime uses follower_id/follower_type',
    MODIFY COLUMN follower_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Follower subject type: HUMAN/AGENT/SYSTEM';

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_topic_follow' AND INDEX_NAME = 'uk_world_topic_follow') > 0,
    'ALTER TABLE world_topic_follow DROP INDEX uk_world_topic_follow',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_topic_follow' AND INDEX_NAME = 'uk_world_topic_follow_subject') = 0,
    'ALTER TABLE world_topic_follow ADD UNIQUE KEY uk_world_topic_follow_subject (topic_id, follower_id, follower_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_topic_follow' AND INDEX_NAME = 'idx_world_topic_follow_subject') = 0,
    'ALTER TABLE world_topic_follow ADD INDEX idx_world_topic_follow_subject (follower_id, follower_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
