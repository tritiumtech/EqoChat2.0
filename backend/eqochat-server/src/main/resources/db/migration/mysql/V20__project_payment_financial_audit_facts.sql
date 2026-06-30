-- Sprint 5B: Project payment rows persist immutable wallet and liability audit facts.
SET NAMES utf8mb4;
SET @db_name = DATABASE();

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND COLUMN_NAME = 'wallet_routing') = 0,
    'ALTER TABLE project_payment ADD COLUMN wallet_routing VARCHAR(40) NULL COMMENT ''Frozen wallet routing: HUMAN_WALLET/AGENT_DIRECT/AGENT_TO_OWNER/NONE'' AFTER master_wallet',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND COLUMN_NAME = 'direct_recipient_id') = 0,
    'ALTER TABLE project_payment ADD COLUMN direct_recipient_id BIGINT NULL COMMENT ''Frozen direct wallet recipient subject id'' AFTER wallet_routing',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND COLUMN_NAME = 'direct_recipient_type') = 0,
    'ALTER TABLE project_payment ADD COLUMN direct_recipient_type VARCHAR(20) NULL COMMENT ''Frozen direct wallet recipient subject type: HUMAN/AGENT'' AFTER direct_recipient_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND COLUMN_NAME = 'settlement_subject_id') = 0,
    'ALTER TABLE project_payment ADD COLUMN settlement_subject_id BIGINT NULL COMMENT ''Frozen settlement subject id'' AFTER direct_recipient_type',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND COLUMN_NAME = 'settlement_subject_type') = 0,
    'ALTER TABLE project_payment ADD COLUMN settlement_subject_type VARCHAR(20) NULL COMMENT ''Frozen settlement subject type: HUMAN/AGENT'' AFTER settlement_subject_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND COLUMN_NAME = 'settlement_human_id') = 0,
    'ALTER TABLE project_payment ADD COLUMN settlement_human_id BIGINT NULL COMMENT ''Frozen settlement human id when funds route through a human wallet'' AFTER settlement_subject_type',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND COLUMN_NAME = 'financial_autonomy') = 0,
    'ALTER TABLE project_payment ADD COLUMN financial_autonomy BOOLEAN NULL COMMENT ''Frozen financial autonomy flag at payment creation'' AFTER settlement_human_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND COLUMN_NAME = 'wallet_policy_state') = 0,
    'ALTER TABLE project_payment ADD COLUMN wallet_policy_state VARCHAR(40) NULL COMMENT ''Frozen wallet policy capability state'' AFTER financial_autonomy',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND COLUMN_NAME = 'wallet_policy_reason') = 0,
    'ALTER TABLE project_payment ADD COLUMN wallet_policy_reason VARCHAR(200) NULL COMMENT ''Frozen wallet policy reason'' AFTER wallet_policy_state',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND COLUMN_NAME = 'liable_human_id') = 0,
    'ALTER TABLE project_payment ADD COLUMN liable_human_id BIGINT NULL COMMENT ''Frozen human liable for the payment recipient subject'' AFTER wallet_policy_reason',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND COLUMN_NAME = 'liability_route') = 0,
    'ALTER TABLE project_payment ADD COLUMN liability_route VARCHAR(200) NULL COMMENT ''Frozen liability route at payment creation'' AFTER liable_human_id',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND COLUMN_NAME = 'liability_reason') = 0,
    'ALTER TABLE project_payment ADD COLUMN liability_reason VARCHAR(200) NULL COMMENT ''Frozen liability resolution reason when not fully resolved'' AFTER liability_route',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE project_payment
SET recipient_type = 'HUMAN'
WHERE recipient_type IS NULL
   OR recipient_type = ''
   OR recipient_type = 'USER';

UPDATE project_payment p
LEFT JOIN agent_profile ap
  ON p.recipient_type = 'AGENT'
 AND ap.id = p.recipient_id
 AND ap.del_token = '0'
LEFT JOIN agent_wallet_state aws
  ON p.recipient_type = 'AGENT'
 AND aws.agent_id = p.recipient_id
 AND aws.del_token = '0'
LEFT JOIN (
    SELECT subject_id, MAX(current_points) AS current_points
    FROM subject_point_ledger
    WHERE subject_type = 'AGENT'
      AND del_token = '0'
    GROUP BY subject_id
) points
  ON p.recipient_type = 'AGENT'
 AND points.subject_id = p.recipient_id
SET
    p.wallet_routing = CASE
        WHEN p.recipient_type = 'HUMAN' THEN 'HUMAN_WALLET'
        WHEN p.recipient_type = 'AGENT' AND ap.owner_id IS NULL THEN 'NONE'
        WHEN p.recipient_type = 'AGENT'
             AND COALESCE(points.current_points, 0) >= 500
             AND (
                 (aws.agent_id IS NOT NULL AND aws.wallet_enabled = TRUE AND aws.enabled_by = ap.owner_id)
                 OR (aws.agent_id IS NULL AND JSON_UNQUOTE(JSON_EXTRACT(ap.source_config, '$.wallet')) = 'enabled')
             )
            THEN 'AGENT_DIRECT'
        WHEN p.recipient_type = 'AGENT' THEN 'AGENT_TO_OWNER'
        ELSE 'NONE'
    END,
    p.direct_recipient_id = p.recipient_id,
    p.direct_recipient_type = CASE WHEN p.recipient_type = 'AGENT' THEN 'AGENT' ELSE 'HUMAN' END,
    p.settlement_subject_id = CASE
        WHEN p.recipient_type = 'HUMAN' THEN p.recipient_id
        WHEN p.recipient_type = 'AGENT'
             AND COALESCE(points.current_points, 0) >= 500
             AND (
                 (aws.agent_id IS NOT NULL AND aws.wallet_enabled = TRUE AND aws.enabled_by = ap.owner_id)
                 OR (aws.agent_id IS NULL AND JSON_UNQUOTE(JSON_EXTRACT(ap.source_config, '$.wallet')) = 'enabled')
             )
            THEN p.recipient_id
        ELSE COALESCE(ap.owner_id, 0)
    END,
    p.settlement_subject_type = CASE
        WHEN p.recipient_type = 'AGENT'
             AND COALESCE(points.current_points, 0) >= 500
             AND (
                 (aws.agent_id IS NOT NULL AND aws.wallet_enabled = TRUE AND aws.enabled_by = ap.owner_id)
                 OR (aws.agent_id IS NULL AND JSON_UNQUOTE(JSON_EXTRACT(ap.source_config, '$.wallet')) = 'enabled')
             )
            THEN 'AGENT'
        ELSE 'HUMAN'
    END,
    p.settlement_human_id = CASE
        WHEN p.recipient_type = 'HUMAN' THEN p.recipient_id
        WHEN p.recipient_type = 'AGENT'
             AND COALESCE(points.current_points, 0) >= 500
             AND (
                 (aws.agent_id IS NOT NULL AND aws.wallet_enabled = TRUE AND aws.enabled_by = ap.owner_id)
                 OR (aws.agent_id IS NULL AND JSON_UNQUOTE(JSON_EXTRACT(ap.source_config, '$.wallet')) = 'enabled')
             )
            THEN NULL
        ELSE ap.owner_id
    END,
    p.financial_autonomy = CASE
        WHEN p.recipient_type = 'HUMAN' THEN TRUE
        WHEN p.recipient_type = 'AGENT'
             AND COALESCE(points.current_points, 0) >= 500
             AND (
                 (aws.agent_id IS NOT NULL AND aws.wallet_enabled = TRUE AND aws.enabled_by = ap.owner_id)
                 OR (aws.agent_id IS NULL AND JSON_UNQUOTE(JSON_EXTRACT(ap.source_config, '$.wallet')) = 'enabled')
             )
            THEN TRUE
        ELSE FALSE
    END,
    p.wallet_policy_state = CASE
        WHEN p.recipient_type = 'HUMAN' THEN 'ENABLED'
        WHEN p.recipient_type = 'AGENT'
             AND COALESCE(points.current_points, 0) >= 500
             AND (
                 (aws.agent_id IS NOT NULL AND aws.wallet_enabled = TRUE AND aws.enabled_by = ap.owner_id)
                 OR (aws.agent_id IS NULL AND JSON_UNQUOTE(JSON_EXTRACT(ap.source_config, '$.wallet')) = 'enabled')
             )
            THEN 'ENABLED'
        ELSE 'DISABLED'
    END,
    p.wallet_policy_reason = CASE
        WHEN p.recipient_type = 'HUMAN' THEN 'backfilled human wallet'
        WHEN p.recipient_type = 'AGENT' AND ap.owner_id IS NULL THEN 'backfill missing agent owner'
        WHEN p.recipient_type = 'AGENT' AND COALESCE(points.current_points, 0) < 500 THEN 'agent points below 500'
        WHEN p.recipient_type = 'AGENT'
             AND aws.agent_id IS NOT NULL
             AND aws.wallet_enabled = TRUE
             AND aws.enabled_by = ap.owner_id
            THEN 'backfilled agent direct wallet'
        WHEN p.recipient_type = 'AGENT'
             AND aws.agent_id IS NULL
             AND JSON_UNQUOTE(JSON_EXTRACT(ap.source_config, '$.wallet')) = 'enabled'
            THEN 'legacy/local source_config wallet enabled fallback'
        ELSE COALESCE(NULLIF(aws.status_reason, ''), 'owner has not enabled agent wallet')
    END,
    p.liable_human_id = CASE
        WHEN p.recipient_type = 'HUMAN' THEN p.recipient_id
        ELSE COALESCE(ap.owner_id, 0)
    END,
    p.liability_route = CASE
        WHEN p.recipient_type = 'HUMAN' THEN CONCAT('human:', p.recipient_id)
        ELSE CONCAT('agent:', p.recipient_id, '->human:', COALESCE(CAST(ap.owner_id AS CHAR), 'unknown'))
    END,
    p.liability_reason = CASE
        WHEN p.recipient_type = 'AGENT' AND ap.owner_id IS NULL THEN 'backfill missing agent owner'
        ELSE NULL
    END
WHERE p.wallet_routing IS NULL
   OR p.wallet_routing = ''
   OR p.direct_recipient_id IS NULL
   OR p.direct_recipient_type IS NULL
   OR p.direct_recipient_type = ''
   OR p.settlement_subject_id IS NULL
   OR p.settlement_subject_type IS NULL
   OR p.settlement_subject_type = ''
   OR p.financial_autonomy IS NULL
   OR p.wallet_policy_state IS NULL
   OR p.wallet_policy_state = ''
   OR p.liable_human_id IS NULL
   OR p.liability_route IS NULL
   OR p.liability_route = '';

UPDATE project_payment
SET master_wallet = wallet_routing
WHERE (master_wallet IS NULL OR master_wallet = '')
  AND wallet_routing IS NOT NULL;

ALTER TABLE project_payment
    MODIFY recipient_type VARCHAR(20) NOT NULL COMMENT 'Payment earned/display recipient subject type: HUMAN/AGENT',
    MODIFY wallet_routing VARCHAR(40) NOT NULL DEFAULT 'NONE' COMMENT 'Frozen wallet routing: HUMAN_WALLET/AGENT_DIRECT/AGENT_TO_OWNER/NONE',
    MODIFY direct_recipient_id BIGINT NOT NULL COMMENT 'Frozen direct wallet recipient subject id',
    MODIFY direct_recipient_type VARCHAR(20) NOT NULL COMMENT 'Frozen direct wallet recipient subject type: HUMAN/AGENT',
    MODIFY settlement_subject_id BIGINT NOT NULL COMMENT 'Frozen settlement subject id',
    MODIFY settlement_subject_type VARCHAR(20) NOT NULL COMMENT 'Frozen settlement subject type: HUMAN/AGENT',
    MODIFY settlement_human_id BIGINT NULL COMMENT 'Frozen settlement human id when funds route through a human wallet',
    MODIFY financial_autonomy BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Frozen financial autonomy flag at payment creation',
    MODIFY wallet_policy_state VARCHAR(40) NOT NULL DEFAULT 'DISABLED' COMMENT 'Frozen wallet policy capability state',
    MODIFY wallet_policy_reason VARCHAR(200) NULL COMMENT 'Frozen wallet policy reason',
    MODIFY liable_human_id BIGINT NOT NULL DEFAULT 0 COMMENT 'Frozen human liable for the payment recipient subject',
    MODIFY liability_route VARCHAR(200) NOT NULL COMMENT 'Frozen liability route at payment creation',
    MODIFY liability_reason VARCHAR(200) NULL COMMENT 'Frozen liability resolution reason when not fully resolved';

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND INDEX_NAME = 'idx_project_payment_recipient_subject') = 0,
    'ALTER TABLE project_payment ADD INDEX idx_project_payment_recipient_subject (recipient_id, recipient_type, project_id)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND INDEX_NAME = 'idx_project_payment_settlement_subject') = 0,
    'ALTER TABLE project_payment ADD INDEX idx_project_payment_settlement_subject (settlement_subject_id, settlement_subject_type)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'project_payment' AND INDEX_NAME = 'idx_project_payment_liable_human') = 0,
    'ALTER TABLE project_payment ADD INDEX idx_project_payment_liable_human (liable_human_id)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
