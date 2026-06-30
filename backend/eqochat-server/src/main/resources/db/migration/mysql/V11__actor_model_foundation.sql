SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

SET @db_name = DATABASE();

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'world_post' AND COLUMN_NAME = 'author_type') = 0,
    'ALTER TABLE world_post ADD COLUMN author_type VARCHAR(20) NOT NULL DEFAULT ''HUMAN'' COMMENT ''Author subject type: HUMAN/AGENT/SYSTEM'' AFTER author_id',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS subject_point_ledger (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    subject_type VARCHAR(20) NOT NULL,
    change_amount INT NOT NULL,
    current_points INT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    related_type VARCHAR(50),
    related_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    INDEX idx_point_subject (subject_id, subject_type),
    INDEX idx_point_related (related_type, related_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主体行为积分账本';

CREATE TABLE IF NOT EXISTS subject_credit_profile (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    subject_type VARCHAR(20) NOT NULL,
    score INT NOT NULL DEFAULT 300,
    rating VARCHAR(20),
    dispute_count INT NOT NULL DEFAULT 0,
    projects_completed INT NOT NULL DEFAULT 0,
    success_rate INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    UNIQUE KEY uk_credit_subject (subject_id, subject_type),
    INDEX idx_credit_rating (rating),
    CHECK (score BETWEEN 300 AND 850)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主体信用档案';

CREATE TABLE IF NOT EXISTS agent_wallet_state (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL UNIQUE,
    wallet_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    enabled_at TIMESTAMP NULL,
    enabled_by BIGINT NULL COMMENT 'owner human id',
    disabled_at TIMESTAMP NULL,
    disabled_by BIGINT NULL,
    status_reason VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',
    INDEX idx_agent_wallet_enabled (wallet_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 钱包启用状态';

INSERT INTO subject_point_ledger (
    subject_id, subject_type, change_amount, current_points, reason, related_type, related_id, del_token, create_time, update_time
)
SELECT s.subject_id, s.subject_type, s.change_amount, s.current_points, 'SPRINT_ACTOR_SEED', 'SPRINT_1A', s.subject_id, '0', NOW(), NOW()
FROM (
    SELECT 2 AS subject_id, 'HUMAN' AS subject_type, 185 AS change_amount, 185 AS current_points
    UNION ALL SELECT 11, 'HUMAN', 260, 260
    UNION ALL SELECT 12, 'HUMAN', 340, 340
    UNION ALL SELECT 13, 'HUMAN', 520, 520
    UNION ALL SELECT 14, 'HUMAN', 245, 245
    UNION ALL SELECT 15, 'HUMAN', 410, 410
    UNION ALL SELECT 16, 'HUMAN', 170, 170
    UNION ALL SELECT 17, 'HUMAN', 210, 210
    UNION ALL SELECT 101, 'AGENT', 650, 650
    UNION ALL SELECT 102, 'AGENT', 260, 260
    UNION ALL SELECT 103, 'AGENT', 720, 720
    UNION ALL SELECT 104, 'AGENT', 430, 430
    UNION ALL SELECT 105, 'AGENT', 390, 390
) s
WHERE NOT EXISTS (
    SELECT 1
    FROM subject_point_ledger existing
    WHERE existing.subject_id = s.subject_id
      AND existing.subject_type = s.subject_type
      AND existing.reason = 'SPRINT_ACTOR_SEED'
      AND existing.del_token = '0'
);

INSERT INTO subject_credit_profile (
    subject_id, subject_type, score, rating, dispute_count, projects_completed, success_rate, del_token, create_time, update_time
)
VALUES
    (2, 'HUMAN', 696, 'GOOD', 0, 3, 94, '0', NOW(), NOW()),
    (11, 'HUMAN', 718, 'GOOD', 0, 5, 96, '0', NOW(), NOW()),
    (12, 'HUMAN', 735, 'GOOD', 0, 7, 97, '0', NOW(), NOW()),
    (13, 'HUMAN', 740, 'GOOD', 0, 6, 95, '0', NOW(), NOW()),
    (14, 'HUMAN', 702, 'GOOD', 0, 4, 93, '0', NOW(), NOW()),
    (15, 'HUMAN', 707, 'GOOD', 0, 4, 92, '0', NOW(), NOW()),
    (16, 'HUMAN', 685, 'GOOD', 0, 2, 90, '0', NOW(), NOW()),
    (17, 'HUMAN', 691, 'GOOD', 0, 3, 91, '0', NOW(), NOW()),
    (101, 'AGENT', 608, 'FAIR', 0, 8, 93, '0', NOW(), NOW()),
    (102, 'AGENT', 471, 'BASE', 1, 2, 82, '0', NOW(), NOW()),
    (103, 'AGENT', 680, 'GOOD', 0, 9, 95, '0', NOW(), NOW()),
    (104, 'AGENT', 559, 'BASE', 0, 3, 88, '0', NOW(), NOW()),
    (105, 'AGENT', 548, 'BASE', 0, 3, 86, '0', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    score = VALUES(score),
    rating = VALUES(rating),
    dispute_count = VALUES(dispute_count),
    projects_completed = VALUES(projects_completed),
    success_rate = VALUES(success_rate),
    del_token = '0',
    update_time = NOW();

INSERT INTO agent_wallet_state (
    agent_id, wallet_enabled, enabled_at, enabled_by, disabled_at, disabled_by, status_reason, del_token, create_time, update_time
)
VALUES
    (101, TRUE, NOW(), 2, NULL, NULL, 'owner enabled after reaching 500 behavior points', '0', NOW(), NOW()),
    (102, FALSE, NULL, NULL, NOW(), 2, 'agent points below 500', '0', NOW(), NOW()),
    (103, TRUE, NOW(), 11, NULL, NULL, 'owner enabled after reaching 500 behavior points', '0', NOW(), NOW()),
    (104, FALSE, NULL, NULL, NOW(), 12, 'agent points below 500', '0', NOW(), NOW()),
    (105, FALSE, NULL, NULL, NOW(), 15, 'agent points below 500', '0', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    wallet_enabled = VALUES(wallet_enabled),
    enabled_at = VALUES(enabled_at),
    enabled_by = VALUES(enabled_by),
    disabled_at = VALUES(disabled_at),
    disabled_by = VALUES(disabled_by),
    status_reason = VALUES(status_reason),
    del_token = '0',
    update_time = NOW();

UPDATE world_post p
LEFT JOIN agent_profile ap
  ON ap.id = p.author_id
 AND ap.del_token = '0'
SET p.author_type = CASE WHEN ap.id IS NULL THEN 'HUMAN' ELSE 'AGENT' END
WHERE p.del_token = '0';

SET FOREIGN_KEY_CHECKS = 1;
