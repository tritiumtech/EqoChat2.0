-- Sprint 9C: consolidate Credit read model on canonical subject identity.
SET NAMES utf8mb4;

UPDATE credit_record
SET subject_type = 'HUMAN'
WHERE subject_type = 'USER';

UPDATE violation_record
SET subject_type = 'HUMAN'
WHERE subject_type = 'USER';

INSERT INTO subject_credit_profile (
    subject_id, subject_type, score, rating, dispute_count, projects_completed, success_rate,
    del_token, create_time, update_time
)
SELECT
    base.subject_id,
    base.subject_type,
    COALESCE(latest.score, 300) AS score,
    CASE
        WHEN COALESCE(latest.score, 300) >= 750 THEN 'EXCELLENT'
        WHEN COALESCE(latest.score, 300) >= 680 THEN 'GOOD'
        WHEN COALESCE(latest.score, 300) >= 600 THEN 'FAIR'
        ELSE 'BASE'
    END AS rating,
    COALESCE(disputes.dispute_count, 0) AS dispute_count,
    COALESCE(projects.projects_completed, positives.positive_count, 0) AS projects_completed,
    CASE
        WHEN COALESCE(projects.projects_completed, positives.positive_count, 0) <= 0 THEN 0
        ELSE GREATEST(0, LEAST(100, ROUND(
            ((COALESCE(projects.projects_completed, positives.positive_count, 0) - COALESCE(disputes.dispute_count, 0)) * 100.0)
            / COALESCE(projects.projects_completed, positives.positive_count, 0)
        )))
    END AS success_rate,
    '0',
    NOW(),
    NOW()
FROM (
    SELECT subject_id, subject_type
    FROM credit_record
    WHERE del_token = '0'
    UNION
    SELECT subject_id, subject_type
    FROM violation_record
    WHERE del_token = '0'
) base
LEFT JOIN (
    SELECT cr.subject_id,
           cr.subject_type,
           CASE
               WHEN cr.current_score BETWEEN 300 AND 850 THEN cr.current_score
               WHEN cr.current_score BETWEEN 0 AND 100 THEN LEAST(850, GREATEST(300, ROUND(300 + (cr.current_score * 5.5))))
               ELSE LEAST(850, GREATEST(300, cr.current_score))
           END AS score
    FROM credit_record cr
    JOIN (
        SELECT subject_id, subject_type, MAX(id) AS max_id
        FROM credit_record
        WHERE del_token = '0'
        GROUP BY subject_id, subject_type
    ) latest_id
      ON latest_id.max_id = cr.id
    WHERE cr.del_token = '0'
) latest
  ON latest.subject_id = base.subject_id
 AND latest.subject_type = base.subject_type
LEFT JOIN (
    SELECT subject_id, subject_type, COUNT(*) AS positive_count
    FROM credit_record
    WHERE del_token = '0'
      AND change_amount > 0
    GROUP BY subject_id, subject_type
) positives
  ON positives.subject_id = base.subject_id
 AND positives.subject_type = base.subject_type
LEFT JOIN (
    SELECT subject_id, subject_type, COUNT(*) AS projects_completed
    FROM credit_record
    WHERE del_token = '0'
      AND change_amount > 0
      AND (
        UPPER(COALESCE(related_type, '')) LIKE '%PROJECT%'
        OR UPPER(COALESCE(reason, '')) LIKE '%PROJECT%'
      )
    GROUP BY subject_id, subject_type
) projects
  ON projects.subject_id = base.subject_id
 AND projects.subject_type = base.subject_type
LEFT JOIN (
    SELECT subject_id, subject_type, COUNT(*) AS dispute_count
    FROM violation_record
    WHERE del_token = '0'
      AND status = 'CONFIRMED'
    GROUP BY subject_id, subject_type
) disputes
  ON disputes.subject_id = base.subject_id
 AND disputes.subject_type = base.subject_type
WHERE base.subject_type IN ('HUMAN', 'AGENT')
ON DUPLICATE KEY UPDATE
    score = VALUES(score),
    rating = VALUES(rating),
    dispute_count = VALUES(dispute_count),
    projects_completed = VALUES(projects_completed),
    success_rate = VALUES(success_rate),
    del_token = '0',
    update_time = NOW();

UPDATE subject_registry sr
JOIN subject_credit_profile credit
  ON credit.subject_id = sr.subject_id
 AND credit.subject_type = sr.subject_type
 AND credit.del_token = '0'
SET sr.credit_score = credit.score,
    sr.credit_rating = credit.rating,
    sr.update_time = NOW()
WHERE sr.del_token = '0';
