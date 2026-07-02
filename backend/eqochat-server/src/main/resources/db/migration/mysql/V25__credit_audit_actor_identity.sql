-- Sprint 12G: Credit audit actors are subject-aware.
SET @db_name = DATABASE();

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'credit_record' AND COLUMN_NAME = 'operator_type') = 0,
    'ALTER TABLE credit_record ADD COLUMN operator_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Operator subject type: HUMAN/AGENT/SYSTEM'' AFTER operator_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE credit_record
SET operator_type = 'HUMAN'
WHERE operator_type IS NULL
   OR operator_type = ''
   OR operator_type = 'USER';

ALTER TABLE credit_record
    MODIFY operator_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Operator subject type: HUMAN/AGENT/SYSTEM';

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'credit_record' AND INDEX_NAME = 'idx_credit_operator_subject') = 0,
    'ALTER TABLE credit_record ADD INDEX idx_credit_operator_subject (operator_id, operator_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'violation_record' AND COLUMN_NAME = 'reporter_type') = 0,
    'ALTER TABLE violation_record ADD COLUMN reporter_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Reporter subject type: HUMAN/AGENT/SYSTEM'' AFTER reporter_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'violation_record' AND COLUMN_NAME = 'reviewer_type') = 0,
    'ALTER TABLE violation_record ADD COLUMN reviewer_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Reviewer subject type: HUMAN/AGENT/SYSTEM'' AFTER reviewer_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE violation_record
SET reporter_type = 'HUMAN'
WHERE reporter_type IS NULL
   OR reporter_type = ''
   OR reporter_type = 'USER';

UPDATE violation_record
SET reviewer_type = 'HUMAN'
WHERE reviewer_type IS NULL
   OR reviewer_type = ''
   OR reviewer_type = 'USER';

ALTER TABLE violation_record
    MODIFY reporter_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Reporter subject type: HUMAN/AGENT/SYSTEM',
    MODIFY reviewer_type VARCHAR(20) NOT NULL DEFAULT 'HUMAN' COMMENT 'Reviewer subject type: HUMAN/AGENT/SYSTEM';

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'violation_record' AND INDEX_NAME = 'idx_violation_reporter_subject') = 0,
    'ALTER TABLE violation_record ADD INDEX idx_violation_reporter_subject (reporter_id, reporter_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'violation_record' AND INDEX_NAME = 'idx_violation_reviewer_subject') = 0,
    'ALTER TABLE violation_record ADD INDEX idx_violation_reviewer_subject (reviewer_id, reviewer_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
