-- Sprint 12F: Contact group owner/member identity is subject-aware.
SET @db_name = DATABASE();

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'group_profile' AND COLUMN_NAME = 'owner_type') = 0,
    'ALTER TABLE group_profile ADD COLUMN owner_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Owner subject type: HUMAN/AGENT'' AFTER owner_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE group_profile
SET owner_type = 'HUMAN'
WHERE owner_type IS NULL
   OR owner_type = ''
   OR owner_type = 'USER';

ALTER TABLE group_profile
    MODIFY owner_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Owner subject type: HUMAN/AGENT';

SET @fk_name = (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'group_profile'
      AND COLUMN_NAME = 'owner_id'
      AND REFERENCED_TABLE_NAME = 'user_profile'
    LIMIT 1
);
SET @ddl = IF(@fk_name IS NOT NULL, CONCAT('ALTER TABLE group_profile DROP FOREIGN KEY ', @fk_name), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @fk_name = NULL;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'group_profile' AND INDEX_NAME = 'idx_group_profile_owner_subject') = 0,
    'ALTER TABLE group_profile ADD INDEX idx_group_profile_owner_subject (owner_id, owner_type, status)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'group_member' AND COLUMN_NAME = 'member_type') = 0,
    'ALTER TABLE group_member ADD COLUMN member_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Member subject type: HUMAN/AGENT'' AFTER user_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE group_member
SET member_type = 'HUMAN'
WHERE member_type IS NULL
   OR member_type = ''
   OR member_type = 'USER';

ALTER TABLE group_member
    MODIFY member_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Member subject type: HUMAN/AGENT';

SET @fk_name = (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'group_member'
      AND COLUMN_NAME = 'user_id'
      AND REFERENCED_TABLE_NAME = 'user_profile'
    LIMIT 1
);
SET @ddl = IF(@fk_name IS NOT NULL, CONCAT('ALTER TABLE group_member DROP FOREIGN KEY ', @fk_name), 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @fk_name = NULL;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'group_member' AND INDEX_NAME = 'idx_group_member_group') = 0,
    'ALTER TABLE group_member ADD INDEX idx_group_member_group (group_id)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'group_member' AND INDEX_NAME = 'uk_member') > 0,
    'ALTER TABLE group_member DROP INDEX uk_member',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'group_member' AND INDEX_NAME = 'uk_group_member_subject') = 0,
    'ALTER TABLE group_member ADD UNIQUE KEY uk_group_member_subject (group_id, user_id, member_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'group_member' AND INDEX_NAME = 'idx_group_member_subject') = 0,
    'ALTER TABLE group_member ADD INDEX idx_group_member_subject (user_id, member_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
