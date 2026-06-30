-- Sprint 3C: Notification runtime queries use recipient subject identity.
SET @db_name = DATABASE();

UPDATE notification
SET recipient_type = 'HUMAN'
WHERE recipient_type IS NULL
   OR recipient_type = ''
   OR recipient_type = 'USER';

UPDATE notification
SET sender_type = 'HUMAN'
WHERE sender_type = 'USER';

UPDATE notification
SET sender_type = 'HUMAN'
WHERE (sender_type IS NULL OR sender_type = '')
  AND sender_id IS NOT NULL;

UPDATE notification
SET sender_type = 'SYSTEM'
WHERE sender_type IS NULL
   OR sender_type = '';

UPDATE notification
SET sender_id = 0
WHERE sender_type = 'SYSTEM'
  AND sender_id IS NULL;

ALTER TABLE notification
    MODIFY recipient_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Recipient subject type: HUMAN/AGENT',
    MODIFY sender_id BIGINT NOT NULL DEFAULT 0 COMMENT 'Sender subject id, 0 for SYSTEM',
    MODIFY sender_type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' COMMENT 'Sender subject type: HUMAN/AGENT/SYSTEM';

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'notification' AND INDEX_NAME = 'idx_notif_recipient_subject') = 0,
    'ALTER TABLE notification ADD INDEX idx_notif_recipient_subject (recipient_id, recipient_type, create_time)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'notification' AND INDEX_NAME = 'idx_notif_unread_subject') = 0,
    'ALTER TABLE notification ADD INDEX idx_notif_unread_subject (recipient_id, recipient_type, is_read)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
