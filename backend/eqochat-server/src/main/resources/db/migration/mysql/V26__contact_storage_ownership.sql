-- Sprint 12H: Contact owns relationship and tag storage names.
SET @db_name = DATABASE();

DROP VIEW IF EXISTS v_user_full;

SET @ddl = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_friend' AND TABLE_TYPE = 'BASE TABLE') > 0
    AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contact_relationship' AND TABLE_TYPE = 'BASE TABLE') = 0,
    'RENAME TABLE user_friend TO contact_relationship',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_contact_tag' AND TABLE_TYPE = 'BASE TABLE') > 0
    AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contact_tag' AND TABLE_TYPE = 'BASE TABLE') = 0,
    'RENAME TABLE user_contact_tag TO contact_tag',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contact_relationship' AND INDEX_NAME = 'uk_user_friend_subject') > 0
    AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contact_relationship' AND INDEX_NAME = 'uk_contact_relationship_subject') = 0,
    'ALTER TABLE contact_relationship RENAME INDEX uk_user_friend_subject TO uk_contact_relationship_subject',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contact_relationship' AND INDEX_NAME = 'idx_user_friend_owner_subject') > 0
    AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contact_relationship' AND INDEX_NAME = 'idx_contact_relationship_owner_subject') = 0,
    'ALTER TABLE contact_relationship RENAME INDEX idx_user_friend_owner_subject TO idx_contact_relationship_owner_subject',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contact_relationship' AND INDEX_NAME = 'idx_user_friend_target_subject') > 0
    AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contact_relationship' AND INDEX_NAME = 'idx_contact_relationship_target_subject') = 0,
    'ALTER TABLE contact_relationship RENAME INDEX idx_user_friend_target_subject TO idx_contact_relationship_target_subject',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contact_tag' AND INDEX_NAME = 'uk_user_contact_tag_subject') > 0
    AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contact_tag' AND INDEX_NAME = 'uk_contact_tag_subject') = 0,
    'ALTER TABLE contact_tag RENAME INDEX uk_user_contact_tag_subject TO uk_contact_tag_subject',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contact_tag' AND INDEX_NAME = 'idx_user_contact_tag_subject') > 0
    AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contact_tag' AND INDEX_NAME = 'idx_contact_tag_subject') = 0,
    'ALTER TABLE contact_tag RENAME INDEX idx_user_contact_tag_subject TO idx_contact_tag_subject',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contact_relationship' AND TABLE_TYPE = 'BASE TABLE') > 0
    AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_friend' AND TABLE_TYPE = 'BASE TABLE') = 0,
    'CREATE OR REPLACE VIEW user_friend AS SELECT * FROM contact_relationship',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'contact_tag' AND TABLE_TYPE = 'BASE TABLE') > 0
    AND (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user_contact_tag' AND TABLE_TYPE = 'BASE TABLE') = 0,
    'CREATE OR REPLACE VIEW user_contact_tag AS SELECT * FROM contact_tag',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE OR REPLACE VIEW v_user_full AS
SELECT
    u.*,
    (SELECT COUNT(*) FROM user_follow WHERE follower_id = u.id AND del_token = '0') as following_count,
    (SELECT COUNT(*) FROM user_follow WHERE following_id = u.id AND del_token = '0') as followers_count,
    (SELECT COUNT(*) FROM contact_relationship WHERE user_id = u.id AND user_type = 'HUMAN' AND status = 'ACTIVE' AND del_token = '0') as friends_count,
    (SELECT COUNT(*) FROM agent_profile WHERE owner_id = u.id AND status = 'ACTIVE' AND del_token = '0') as agent_count
FROM user_profile u
WHERE u.del_token = '0';
