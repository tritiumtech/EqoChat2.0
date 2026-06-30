SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

SET @db_name = DATABASE();

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'message' AND COLUMN_NAME = 'liable_human_id') = 0,
    'ALTER TABLE message ADD COLUMN liable_human_id BIGINT NULL COMMENT ''Human liable for this message action'' AFTER sender_type',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE conversation_participant
SET participant_type = 'HUMAN'
WHERE participant_type = 'USER';

UPDATE message
SET sender_type = 'HUMAN'
WHERE sender_type = 'USER';

UPDATE message_read_receipt
SET reader_type = 'HUMAN'
WHERE reader_type = 'USER';

UPDATE message_reaction
SET reactor_type = 'HUMAN'
WHERE reactor_type = 'USER';

UPDATE message
SET liable_human_id = CASE
    WHEN sender_type = 'HUMAN' THEN sender_id
    WHEN sender_type = 'AGENT' THEN (
        SELECT ab.owner_id
        FROM agent_binding ab
        WHERE ab.agent_id = message.sender_id
          AND ab.binding_type = 'OWNER'
          AND ab.binding_status = 'ACTIVE'
          AND ab.del_token = '0'
        ORDER BY ab.id DESC
        LIMIT 1
    )
    ELSE NULL
END
WHERE liable_human_id IS NULL
  AND sender_type IN ('HUMAN', 'AGENT');

ALTER TABLE conversation_participant
    MODIFY participant_type VARCHAR(20) NOT NULL COMMENT 'HUMAN/AGENT/SYSTEM';

ALTER TABLE message
    MODIFY sender_type VARCHAR(20) NOT NULL COMMENT 'HUMAN/AGENT/SYSTEM';

ALTER TABLE message_read_receipt
    MODIFY reader_type VARCHAR(20) NOT NULL COMMENT 'HUMAN/AGENT/SYSTEM';

ALTER TABLE message_reaction
    MODIFY reactor_type VARCHAR(20) NOT NULL COMMENT 'HUMAN/AGENT/SYSTEM';

SET FOREIGN_KEY_CHECKS = 1;
