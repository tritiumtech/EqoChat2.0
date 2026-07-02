-- Sprint 9A: Canonical subject read-model registry.
SET NAMES utf8mb4;
SET @db_name = DATABASE();

CREATE TABLE IF NOT EXISTS subject_registry (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NOT NULL,
    subject_type VARCHAR(20) NOT NULL COMMENT 'Canonical subject type: HUMAN/AGENT',
    did VARCHAR(255) NULL,
    display_name VARCHAR(120) NOT NULL,
    avatar_url VARCHAR(500) NULL,
    bio TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'Canonical SubjectStatus',
    contact_phone VARCHAR(20) NULL,
    contact_email VARCHAR(100) NULL,
    points INT NOT NULL DEFAULT 0,
    credit_score INT NOT NULL DEFAULT 300,
    credit_rating VARCHAR(20) NULL,
    associated_human_id BIGINT NULL,
    associated_human_name VARCHAR(120) NULL,
    source_table VARCHAR(64) NOT NULL,
    source_id BIGINT NOT NULL,
    capability_tags TEXT NULL,
    search_text TEXT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    del_token VARCHAR(64) DEFAULT '0',
    UNIQUE KEY uk_subject_registry_ref (subject_id, subject_type),
    INDEX idx_subject_registry_type_status (subject_type, status),
    INDEX idx_subject_registry_did (did),
    INDEX idx_subject_registry_display_name (display_name),
    INDEX idx_subject_registry_contact_phone (contact_phone),
    INDEX idx_subject_registry_contact_email (contact_email),
    INDEX idx_subject_registry_associated_human (associated_human_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Canonical subject directory read model';

INSERT INTO subject_registry (
    subject_id, subject_type, did, display_name, avatar_url, bio, status,
    contact_phone, contact_email, points, credit_score, credit_rating,
    associated_human_id, associated_human_name, source_table, source_id,
    capability_tags, search_text, del_token, create_time, update_time
)
SELECT
    u.id,
    'HUMAN',
    u.did,
    COALESCE(NULLIF(u.nickname, ''), NULLIF(u.email, ''), NULLIF(u.phone, ''), 'User'),
    u.avatar_url,
    u.bio,
    CASE
        WHEN u.status = 'ACTIVE' THEN 'ACTIVE'
        WHEN u.status = 'INACTIVE' THEN 'INACTIVE'
        WHEN u.status = 'BANNED' THEN 'BANNED'
        ELSE 'UNKNOWN'
    END,
    u.phone,
    u.email,
    COALESCE(points.current_points, 0),
    COALESCE(credit.score,
        CASE
            WHEN u.credit_score BETWEEN 300 AND 850 THEN u.credit_score
            WHEN u.credit_score BETWEEN 0 AND 100 THEN LEAST(850, GREATEST(300, ROUND(300 + (u.credit_score * 5.5))))
            ELSE 300
        END
    ),
    COALESCE(credit.rating, NULL),
    u.id,
    COALESCE(NULLIF(u.nickname, ''), NULLIF(u.email, ''), NULLIF(u.phone, ''), 'User'),
    'user_profile',
    u.id,
    NULL,
    CONCAT_WS(' ', u.id, 'HUMAN', u.did, u.phone, u.email, u.nickname, u.bio),
    '0',
    NOW(),
    NOW()
FROM user_profile u
LEFT JOIN (
    SELECT spl.subject_id, spl.subject_type, spl.current_points
    FROM subject_point_ledger spl
    JOIN (
        SELECT subject_id, subject_type, MAX(id) AS max_id
        FROM subject_point_ledger
        WHERE del_token = '0'
        GROUP BY subject_id, subject_type
    ) latest ON latest.max_id = spl.id
    WHERE spl.del_token = '0'
) points
  ON points.subject_id = u.id
 AND points.subject_type = 'HUMAN'
LEFT JOIN subject_credit_profile credit
  ON credit.subject_id = u.id
 AND credit.subject_type = 'HUMAN'
 AND credit.del_token = '0'
WHERE u.del_token = '0'
ON DUPLICATE KEY UPDATE
    did = VALUES(did),
    display_name = VALUES(display_name),
    avatar_url = VALUES(avatar_url),
    bio = VALUES(bio),
    status = VALUES(status),
    contact_phone = VALUES(contact_phone),
    contact_email = VALUES(contact_email),
    points = VALUES(points),
    credit_score = VALUES(credit_score),
    credit_rating = VALUES(credit_rating),
    associated_human_id = VALUES(associated_human_id),
    associated_human_name = VALUES(associated_human_name),
    source_table = VALUES(source_table),
    source_id = VALUES(source_id),
    capability_tags = VALUES(capability_tags),
    search_text = VALUES(search_text),
    del_token = '0',
    update_time = NOW();

INSERT INTO subject_registry (
    subject_id, subject_type, did, display_name, avatar_url, bio, status,
    contact_phone, contact_email, points, credit_score, credit_rating,
    associated_human_id, associated_human_name, source_table, source_id,
    capability_tags, search_text, del_token, create_time, update_time
)
SELECT
    ap.id,
    'AGENT',
    ap.did,
    COALESCE(NULLIF(ap.name, ''), 'Agent'),
    ap.avatar_url,
    ap.description,
    CASE
        WHEN ap.status = 'ACTIVE' THEN 'ACTIVE'
        WHEN ap.status = 'INACTIVE' THEN 'INACTIVE'
        WHEN ap.status = 'SUSPENDED' THEN 'SUSPENDED'
        ELSE 'UNKNOWN'
    END,
    NULL,
    NULL,
    COALESCE(points.current_points, 0),
    COALESCE(credit.score,
        CASE
            WHEN ap.credit_score BETWEEN 300 AND 850 THEN ap.credit_score
            WHEN ap.credit_score BETWEEN 0 AND 100 THEN LEAST(850, GREATEST(300, ROUND(300 + (ap.credit_score * 5.5))))
            ELSE 300
        END
    ),
    COALESCE(credit.rating, NULL),
    ap.owner_id,
    COALESCE(NULLIF(owner.nickname, ''), NULLIF(owner.email, ''), NULLIF(owner.phone, ''), 'User'),
    'agent_profile',
    ap.id,
    ap.capability_tags,
    CONCAT_WS(' ', ap.id, 'AGENT', ap.did, ap.name, ap.description,
              owner.id, owner.did, owner.nickname, owner.email, owner.phone),
    '0',
    NOW(),
    NOW()
FROM agent_profile ap
LEFT JOIN user_profile owner
  ON owner.id = ap.owner_id
 AND owner.del_token = '0'
LEFT JOIN (
    SELECT spl.subject_id, spl.subject_type, spl.current_points
    FROM subject_point_ledger spl
    JOIN (
        SELECT subject_id, subject_type, MAX(id) AS max_id
        FROM subject_point_ledger
        WHERE del_token = '0'
        GROUP BY subject_id, subject_type
    ) latest ON latest.max_id = spl.id
    WHERE spl.del_token = '0'
) points
  ON points.subject_id = ap.id
 AND points.subject_type = 'AGENT'
LEFT JOIN subject_credit_profile credit
  ON credit.subject_id = ap.id
 AND credit.subject_type = 'AGENT'
 AND credit.del_token = '0'
WHERE ap.del_token = '0'
ON DUPLICATE KEY UPDATE
    did = VALUES(did),
    display_name = VALUES(display_name),
    avatar_url = VALUES(avatar_url),
    bio = VALUES(bio),
    status = VALUES(status),
    contact_phone = VALUES(contact_phone),
    contact_email = VALUES(contact_email),
    points = VALUES(points),
    credit_score = VALUES(credit_score),
    credit_rating = VALUES(credit_rating),
    associated_human_id = VALUES(associated_human_id),
    associated_human_name = VALUES(associated_human_name),
    source_table = VALUES(source_table),
    source_id = VALUES(source_id),
    capability_tags = VALUES(capability_tags),
    search_text = VALUES(search_text),
    del_token = '0',
    update_time = NOW();
