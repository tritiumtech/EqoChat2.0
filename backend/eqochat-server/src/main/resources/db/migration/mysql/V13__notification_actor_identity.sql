UPDATE notification
SET recipient_type = 'HUMAN'
WHERE recipient_type = 'USER';

UPDATE notification
SET sender_type = 'HUMAN'
WHERE sender_type = 'USER';

ALTER TABLE notification
    MODIFY recipient_type VARCHAR(20) DEFAULT 'HUMAN' COMMENT 'HUMAN/AGENT',
    MODIFY sender_type VARCHAR(20) NULL COMMENT 'HUMAN/AGENT/SYSTEM';
