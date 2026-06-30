-- Sprint 4A: Contact and friend requests use canonical subject identity.
SET @db_name = DATABASE();

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_friend' AND COLUMN_NAME = 'user_type') = 0,
    'ALTER TABLE user_friend ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Owner subject type: HUMAN/AGENT'' AFTER user_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE user_friend
SET user_type = 'HUMAN'
WHERE user_type IS NULL
   OR user_type = '';

UPDATE user_friend
SET friend_type = 'HUMAN'
WHERE friend_type IS NULL
   OR friend_type = ''
   OR friend_type = 'USER';

ALTER TABLE user_friend
    MODIFY user_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Owner subject type: HUMAN/AGENT',
    MODIFY friend_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Target subject type: HUMAN/AGENT';

SET @fk_name = (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'user_friend'
      AND COLUMN_NAME = 'user_id'
      AND REFERENCED_TABLE_NAME = 'user_profile'
    LIMIT 1
);
SET @ddl = IF(@fk_name IS NOT NULL, CONCAT('ALTER TABLE user_friend DROP FOREIGN KEY ', @fk_name), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @fk_name = NULL;

SET @fk_name = (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'user_friend'
      AND COLUMN_NAME = 'friend_id'
      AND REFERENCED_TABLE_NAME = 'user_profile'
    LIMIT 1
);
SET @ddl = IF(@fk_name IS NOT NULL, CONCAT('ALTER TABLE user_friend DROP FOREIGN KEY ', @fk_name), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @fk_name = NULL;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_friend' AND INDEX_NAME = 'uk_friend') > 0,
    'ALTER TABLE user_friend DROP INDEX uk_friend',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_friend' AND INDEX_NAME = 'uk_user_friend_subject') = 0,
    'ALTER TABLE user_friend ADD UNIQUE KEY uk_user_friend_subject (user_id, user_type, friend_id, friend_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_friend' AND INDEX_NAME = 'idx_user_friend_owner_subject') = 0,
    'ALTER TABLE user_friend ADD INDEX idx_user_friend_owner_subject (user_id, user_type, status)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_friend' AND INDEX_NAME = 'idx_user_friend_target_subject') = 0,
    'ALTER TABLE user_friend ADD INDEX idx_user_friend_target_subject (friend_id, friend_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'friend_request' AND COLUMN_NAME = 'requester_type') = 0,
    'ALTER TABLE friend_request ADD COLUMN requester_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Requester subject type: HUMAN/AGENT'' AFTER requester_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'friend_request' AND COLUMN_NAME = 'recipient_type') = 0,
    'ALTER TABLE friend_request ADD COLUMN recipient_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Recipient subject type: HUMAN/AGENT'' AFTER recipient_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE friend_request
SET requester_type = 'HUMAN'
WHERE requester_type IS NULL
   OR requester_type = ''
   OR requester_type = 'USER';

UPDATE friend_request
SET recipient_type = 'HUMAN'
WHERE recipient_type IS NULL
   OR recipient_type = ''
   OR recipient_type = 'USER';

ALTER TABLE friend_request
    MODIFY requester_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Requester subject type: HUMAN/AGENT',
    MODIFY recipient_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Recipient subject type: HUMAN/AGENT';

SET @fk_name = (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'friend_request'
      AND COLUMN_NAME = 'requester_id'
      AND REFERENCED_TABLE_NAME = 'user_profile'
    LIMIT 1
);
SET @ddl = IF(@fk_name IS NOT NULL, CONCAT('ALTER TABLE friend_request DROP FOREIGN KEY ', @fk_name), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @fk_name = NULL;

SET @fk_name = (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'friend_request'
      AND COLUMN_NAME = 'recipient_id'
      AND REFERENCED_TABLE_NAME = 'user_profile'
    LIMIT 1
);
SET @ddl = IF(@fk_name IS NOT NULL, CONCAT('ALTER TABLE friend_request DROP FOREIGN KEY ', @fk_name), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @fk_name = NULL;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'friend_request' AND INDEX_NAME = 'idx_friend_request_requester_subject') = 0,
    'ALTER TABLE friend_request ADD INDEX idx_friend_request_requester_subject (requester_id, requester_type, create_time)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'friend_request' AND INDEX_NAME = 'idx_friend_request_recipient_subject') = 0,
    'ALTER TABLE friend_request ADD INDEX idx_friend_request_recipient_subject (recipient_id, recipient_type, status, create_time)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_contact_tag' AND COLUMN_NAME = 'user_type') = 0,
    'ALTER TABLE user_contact_tag ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Owner subject type: HUMAN/AGENT'' AFTER user_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_contact_tag' AND COLUMN_NAME = 'friend_type') = 0,
    'ALTER TABLE user_contact_tag ADD COLUMN friend_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Target subject type: HUMAN/AGENT'' AFTER friend_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE user_contact_tag
SET user_type = 'HUMAN'
WHERE user_type IS NULL
   OR user_type = ''
   OR user_type = 'USER';

UPDATE user_contact_tag
SET friend_type = 'HUMAN'
WHERE friend_type IS NULL
   OR friend_type = ''
   OR friend_type = 'USER';

ALTER TABLE user_contact_tag
    MODIFY user_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Owner subject type: HUMAN/AGENT',
    MODIFY friend_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Target subject type: HUMAN/AGENT';

SET @fk_name = (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'user_contact_tag'
      AND COLUMN_NAME = 'user_id'
      AND REFERENCED_TABLE_NAME = 'user_profile'
    LIMIT 1
);
SET @ddl = IF(@fk_name IS NOT NULL, CONCAT('ALTER TABLE user_contact_tag DROP FOREIGN KEY ', @fk_name), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @fk_name = NULL;

SET @fk_name = (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'user_contact_tag'
      AND COLUMN_NAME = 'friend_id'
      AND REFERENCED_TABLE_NAME = 'user_profile'
    LIMIT 1
);
SET @ddl = IF(@fk_name IS NOT NULL, CONCAT('ALTER TABLE user_contact_tag DROP FOREIGN KEY ', @fk_name), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @fk_name = NULL;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_contact_tag' AND INDEX_NAME = 'uk_user_contact_tag') > 0,
    'ALTER TABLE user_contact_tag DROP INDEX uk_user_contact_tag',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_contact_tag' AND INDEX_NAME = 'uk_user_contact_tag_subject') = 0,
    'ALTER TABLE user_contact_tag ADD UNIQUE KEY uk_user_contact_tag_subject (user_id, user_type, friend_id, friend_type, tag_name)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_contact_tag' AND INDEX_NAME = 'idx_user_contact_tag_user_friend') > 0,
    'ALTER TABLE user_contact_tag DROP INDEX idx_user_contact_tag_user_friend',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_contact_tag' AND INDEX_NAME = 'idx_user_contact_tag_subject') = 0,
    'ALTER TABLE user_contact_tag ADD INDEX idx_user_contact_tag_subject (user_id, user_type, friend_id, friend_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
