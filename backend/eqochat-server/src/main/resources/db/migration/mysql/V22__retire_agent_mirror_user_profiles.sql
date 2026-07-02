-- Sprint 9B: retire transitional Agent mirror user_profile rows.
SET NAMES utf8mb4;

UPDATE user_profile u
JOIN agent_profile ap
  ON ap.id = u.id
 AND ap.del_token = '0'
SET u.del_token = CAST(CAST(ROUND(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(6)) * 1000000) AS UNSIGNED) + u.id AS CHAR),
    u.status = 'INACTIVE',
    u.update_time = NOW()
WHERE u.del_token = '0'
  AND u.did LIKE 'did:eqochat:agent:%-user'
  AND (u.password_hash IS NULL OR u.password_hash = '');

UPDATE subject_registry sr
JOIN user_profile u
  ON u.id = sr.subject_id
 AND u.did LIKE 'did:eqochat:agent:%-user'
JOIN agent_profile ap
  ON ap.id = u.id
 AND ap.del_token = '0'
SET sr.del_token = CAST(CAST(ROUND(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(6)) * 1000000) AS UNSIGNED) + sr.subject_id AS CHAR),
    sr.status = 'INACTIVE',
    sr.update_time = NOW()
WHERE sr.subject_type = 'HUMAN'
  AND sr.del_token = '0'
  AND (u.password_hash IS NULL OR u.password_hash = '');

UPDATE user_friend uf
JOIN user_profile u
  ON u.id = uf.user_id
 AND u.did LIKE 'did:eqochat:agent:%-user'
JOIN agent_profile ap
  ON ap.id = u.id
 AND ap.del_token = '0'
SET uf.del_token = CAST(CAST(ROUND(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(6)) * 1000000) AS UNSIGNED) + uf.id AS CHAR),
    uf.status = 'DELETED',
    uf.update_time = NOW()
WHERE uf.user_type = 'HUMAN'
  AND uf.del_token = '0';

UPDATE user_friend uf
JOIN user_profile u
  ON u.id = uf.friend_id
 AND u.did LIKE 'did:eqochat:agent:%-user'
JOIN agent_profile ap
  ON ap.id = u.id
 AND ap.del_token = '0'
SET uf.del_token = CAST(CAST(ROUND(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(6)) * 1000000) AS UNSIGNED) + uf.id AS CHAR),
    uf.status = 'DELETED',
    uf.update_time = NOW()
WHERE uf.friend_type = 'HUMAN'
  AND uf.del_token = '0';

INSERT INTO user_contact_tag (
    user_id, user_type, friend_id, friend_type, tag_name,
    create_time, update_time, create_by, update_by, del_token
)
SELECT
    uct.user_id,
    uct.user_type,
    uct.friend_id,
    'AGENT',
    uct.tag_name,
    uct.create_time,
    NOW(),
    uct.create_by,
    uct.update_by,
    '0'
FROM user_contact_tag uct
JOIN user_profile u
  ON u.id = uct.friend_id
 AND u.did LIKE 'did:eqochat:agent:%-user'
JOIN agent_profile ap
  ON ap.id = u.id
 AND ap.del_token = '0'
WHERE uct.friend_type = 'HUMAN'
  AND uct.del_token = '0'
ON DUPLICATE KEY UPDATE
    del_token = '0',
    update_time = NOW();

UPDATE user_contact_tag uct
JOIN user_profile owner
  ON owner.id = uct.user_id
 AND owner.did LIKE 'did:eqochat:agent:%-user'
JOIN agent_profile owner_agent
  ON owner_agent.id = owner.id
 AND owner_agent.del_token = '0'
SET uct.del_token = CAST(CAST(ROUND(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(6)) * 1000000) AS UNSIGNED) + uct.id AS CHAR),
    uct.update_time = NOW()
WHERE uct.user_type = 'HUMAN'
  AND uct.del_token = '0';

UPDATE user_contact_tag uct
JOIN user_profile friend
  ON friend.id = uct.friend_id
 AND friend.did LIKE 'did:eqochat:agent:%-user'
JOIN agent_profile friend_agent
  ON friend_agent.id = friend.id
 AND friend_agent.del_token = '0'
SET uct.del_token = CAST(CAST(ROUND(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(6)) * 1000000) AS UNSIGNED) + uct.id AS CHAR),
    uct.update_time = NOW()
WHERE uct.friend_type = 'HUMAN'
  AND uct.del_token = '0';

UPDATE world_post_mention wpm
JOIN user_profile u
  ON u.id = wpm.mentioned_subject_id
 AND u.did LIKE 'did:eqochat:agent:%-user'
JOIN agent_profile ap
  ON ap.id = u.id
 AND ap.del_token = '0'
SET wpm.mentioned_subject_type = 'AGENT',
    wpm.update_time = NOW()
WHERE wpm.mentioned_subject_type = 'HUMAN'
  AND wpm.del_token = '0';
