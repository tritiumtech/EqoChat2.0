package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.actor.api.model.SubjectType;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
class SubjectRegistryRepository {

    private static final String TABLE = "subject_registry";
    private static final int DISPLAY_NAME_MAX_LENGTH = 120;

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, Boolean> tableExistsCache = new ConcurrentHashMap<>();

    SubjectRegistryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<SubjectRegistryRecord> find(SubjectRef ref) {
        if (ref == null || ref.id() == null || ref.type() == null || ref.type() == SubjectType.SYSTEM || !available()) {
            return Optional.empty();
        }
        try {
            List<SubjectRegistryRecord> rows = jdbcTemplate.query(
                    """
                    SELECT subject_id, subject_type, did, display_name, avatar_url, bio, status,
                           points, credit_score, associated_human_id, associated_human_name, capability_tags
                    FROM subject_registry
                    WHERE subject_id = ?
                      AND subject_type = ?
                      AND del_token = '0'
                    LIMIT 1
                    """,
                    this::mapRecord,
                    ref.id(),
                    ref.type().name()
            );
            return rows.stream().findFirst();
        } catch (DataAccessException ignored) {
            return Optional.empty();
        }
    }

    List<SubjectRef> search(String keyword, int limit) {
        if (!StringUtils.hasText(keyword) || limit <= 0 || !available()) {
            return List.of();
        }
        String value = keyword.trim();
        Long numericId = parseLong(value);

        StringBuilder sql = new StringBuilder(
                """
                SELECT subject_id, subject_type
                FROM subject_registry
                WHERE del_token = '0'
                  AND subject_type IN ('HUMAN', 'AGENT')
                  AND (
                """
        );
        List<String> predicates = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (numericId != null) {
            predicates.add("subject_id = ?");
            params.add(numericId);
        }
        predicates.add("did = ?");
        params.add(value);
        predicates.add("contact_phone = ?");
        params.add(value);
        predicates.add("contact_email = ?");
        params.add(value);
        predicates.add("display_name = ?");
        params.add(value);
        predicates.add("search_text LIKE ?");
        params.add("%" + escapeLike(value) + "%");

        sql.append(String.join("\n       OR ", predicates));
        sql.append(
                """
                  )
                ORDER BY
                  CASE
                    WHEN subject_id = ? THEN 0
                    WHEN did = ? THEN 1
                    WHEN contact_phone = ? THEN 2
                    WHEN contact_email = ? THEN 3
                    WHEN display_name = ? THEN 4
                    ELSE 5
                  END,
                  CASE subject_type WHEN 'HUMAN' THEN 0 WHEN 'AGENT' THEN 1 ELSE 2 END,
                  subject_id ASC
                LIMIT ?
                """
        );
        params.add(numericId);
        params.add(value);
        params.add(value);
        params.add(value);
        params.add(value);
        params.add(limit);

        try {
            return jdbcTemplate.query(
                    sql.toString(),
                    (rs, rowNum) -> new SubjectRef(rs.getLong("subject_id"), SubjectType.from(rs.getString("subject_type"))),
                    params.toArray()
            );
        } catch (DataAccessException | IllegalArgumentException ignored) {
            return List.of();
        }
    }

    List<SubjectRef> findAssociatedSubjects(Long principalHumanId) {
        if (principalHumanId == null || principalHumanId <= 0 || !available()) {
            return List.of();
        }
        try {
            return jdbcTemplate.query(
                    """
                    SELECT subject_id, subject_type
                    FROM subject_registry
                    WHERE del_token = '0'
                      AND subject_type IN ('HUMAN', 'AGENT')
                      AND (
                        (subject_type = 'HUMAN' AND subject_id = ?)
                        OR associated_human_id = ?
                      )
                    ORDER BY
                      CASE subject_type WHEN 'HUMAN' THEN 0 WHEN 'AGENT' THEN 1 ELSE 2 END,
                      subject_id ASC
                    """,
                    (rs, rowNum) -> new SubjectRef(rs.getLong("subject_id"), SubjectType.from(rs.getString("subject_type"))),
                    principalHumanId,
                    principalHumanId
            );
        } catch (DataAccessException | IllegalArgumentException ignored) {
            return List.of();
        }
    }

    void upsertHuman(ActorSourceRepository.Human profile, SubjectSummaryResponse summary) {
        if (profile == null || summary == null || profile.getId() == null || !available()) {
            return;
        }
        upsert(
                summary,
                profile.phone(),
                profile.email(),
                null,
                "user_profile",
                profile.getId()
        );
    }

    void upsertAgent(ActorSourceRepository.Agent profile, SubjectSummaryResponse summary) {
        if (profile == null || summary == null || profile.getId() == null || !available()) {
            return;
        }
        upsert(
                summary,
                null,
                null,
                profile.getCapabilityTags(),
                "agent_profile",
                profile.getId()
        );
    }

    void retire(SubjectRef ref) {
        if (ref == null || ref.id() == null || ref.type() == null || ref.type() == SubjectType.SYSTEM || !available()) {
            return;
        }
        try {
            jdbcTemplate.update(
                    """
                    UPDATE subject_registry
                    SET del_token = CAST(ROUND(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(6)) * 1000000) AS CHAR),
                        status = 'INACTIVE',
                        update_time = NOW()
                    WHERE subject_id = ?
                      AND subject_type = ?
                      AND del_token = '0'
                    """,
                    ref.id(),
                    ref.type().name()
            );
        } catch (DataAccessException ignored) {
            // Registry is a read model; deletion in the source table remains authoritative.
        }
    }

    boolean available() {
        return tableExists(TABLE);
    }

    private void upsert(
            SubjectSummaryResponse summary,
            String contactPhone,
            String contactEmail,
            String capabilityTags,
            String sourceTable,
            Long sourceId
    ) {
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO subject_registry (
                        subject_id, subject_type, did, display_name, avatar_url, bio, status,
                        contact_phone, contact_email, points, credit_score, associated_human_id,
                        associated_human_name, source_table, source_id, capability_tags, search_text,
                        del_token, create_time, update_time
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0', NOW(), NOW())
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
                        associated_human_id = VALUES(associated_human_id),
                        associated_human_name = VALUES(associated_human_name),
                        source_table = VALUES(source_table),
                        source_id = VALUES(source_id),
                        capability_tags = VALUES(capability_tags),
                        search_text = VALUES(search_text),
                        del_token = '0',
                        update_time = NOW()
                    """,
                    summary.getId(),
                    summary.getType().name(),
                    summary.getDid(),
                    boundedDisplayName(summary.getDisplayName(), summary.getType()),
                    summary.getAvatarUrl(),
                    summary.getBio(),
                    summary.getStatus() != null ? summary.getStatus().name() : SubjectStatus.UNKNOWN.name(),
                    contactPhone,
                    contactEmail,
                    summary.getPoints() != null ? summary.getPoints() : 0,
                    summary.getCreditScore() != null ? summary.getCreditScore() : 300,
                    summary.getAssociatedHumanId(),
                    summary.getAssociatedHumanName(),
                    sourceTable,
                    sourceId,
                    capabilityTags,
                    searchText(summary, contactPhone, contactEmail)
            );
        } catch (DataAccessException ignored) {
            // Registry sync is best effort; runtime subject reads remain registry-only.
        }
    }

    private SubjectRegistryRecord mapRecord(ResultSet rs, int rowNum) throws SQLException {
        return new SubjectRegistryRecord(
                rs.getLong("subject_id"),
                SubjectType.from(rs.getString("subject_type")),
                rs.getString("did"),
                rs.getString("display_name"),
                rs.getString("avatar_url"),
                rs.getString("bio"),
                parseStatus(rs.getString("status")),
                rs.getObject("points", Integer.class),
                rs.getObject("credit_score", Integer.class),
                rs.getObject("associated_human_id", Long.class),
                rs.getString("associated_human_name"),
                rs.getString("capability_tags")
        );
    }

    private boolean tableExists(String tableName) {
        if (Boolean.TRUE.equals(tableExistsCache.get(tableName))) {
            return true;
        }
        boolean exists;
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = ?
                    """,
                    Integer.class,
                    tableName
            );
            exists = count != null && count > 0;
        } catch (DataAccessException ignored) {
            exists = false;
        }
        if (exists) {
            tableExistsCache.put(tableName, true);
        }
        return exists;
    }

    private static SubjectStatus parseStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            return SubjectStatus.UNKNOWN;
        }
        try {
            return SubjectStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return SubjectStatus.UNKNOWN;
        }
    }

    private static Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static String boundedDisplayName(String displayName, SubjectType type) {
        String fallback = type == SubjectType.AGENT ? "Agent" : "User";
        String value = StringUtils.hasText(displayName) ? displayName.trim() : fallback;
        return value.length() <= DISPLAY_NAME_MAX_LENGTH ? value : value.substring(0, DISPLAY_NAME_MAX_LENGTH);
    }

    private static String searchText(SubjectSummaryResponse summary, String contactPhone, String contactEmail) {
        List<String> parts = new ArrayList<>();
        add(parts, summary.getId() != null ? summary.getId().toString() : null);
        add(parts, summary.getType() != null ? summary.getType().name() : null);
        add(parts, summary.getDid());
        add(parts, contactPhone);
        add(parts, contactEmail);
        add(parts, summary.getDisplayName());
        add(parts, summary.getBio());
        add(parts, summary.getAssociatedHumanName());
        return String.join(" ", parts);
    }

    private static void add(List<String> parts, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(value.trim());
        }
    }

    record SubjectRegistryRecord(
            Long subjectId,
            SubjectType subjectType,
            String did,
            String displayName,
            String avatarUrl,
            String bio,
            SubjectStatus status,
            Integer points,
            Integer creditScore,
            Long associatedHumanId,
            String associatedHumanName,
            String capabilityTags
    ) {
        SubjectRef ref() {
            return new SubjectRef(subjectId, subjectType);
        }
    }
}
