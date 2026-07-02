package com.eqochat.business.actor.service.impl;

import com.eqochat.business.actor.api.model.CreditProfileSummary;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
class ActorDataAccess {

    private static final String DEMO_USER_POINTS_PREFIX = "demo.user.points.";
    private static final String DEMO_AGENT_POINTS_PREFIX = "demo.agent.points.";
    private static final int STATUS_REASON_MAX_LENGTH = 200;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final boolean demoFallbackEnabled;
    private final Map<String, Boolean> tableExistsCache = new ConcurrentHashMap<>();

    ActorDataAccess(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            @Value("${eqochat.actor.demo-fallback.enabled:false}") boolean demoFallbackEnabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.demoFallbackEnabled = demoFallbackEnabled;
    }

    int currentPoints(SubjectRef ref, Integer fallback) {
        int resolvedFallback = fallback != null ? Math.max(0, fallback) : 0;
        if (ref == null || ref.id() == null) {
            return resolvedFallback;
        }
        if (tableExists("subject_point_ledger")) {
            try {
                Integer points = jdbcTemplate.queryForObject(
                        """
                        SELECT current_points
                        FROM subject_point_ledger
                        WHERE subject_id = ?
                          AND subject_type = ?
                          AND del_token = '0'
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                        Integer.class,
                        ref.id(),
                        ref.type().name()
                );
                if (points != null) {
                    return Math.max(0, points);
                }
            } catch (DataAccessException ignored) {
                // Optional demo/local seed fallback is gated below.
            }
        }

        if (!demoFallbackEnabled) {
            return resolvedFallback;
        }

        String keyPrefix = ref.isAgent() ? DEMO_AGENT_POINTS_PREFIX : DEMO_USER_POINTS_PREFIX;
        try {
            Integer configured = jdbcTemplate.queryForObject(
                    "SELECT CAST(config_value AS SIGNED) FROM system_config WHERE config_key = ? AND del_token = '0' LIMIT 1",
                    Integer.class,
                    keyPrefix + ref.id()
            );
            return configured != null ? Math.max(0, configured) : resolvedFallback;
        } catch (DataAccessException ignored) {
            return resolvedFallback;
        }
    }

    CreditProfileSummary creditProfile(SubjectRef ref, Integer legacyScore) {
        if (ref != null && ref.id() != null && tableExists("subject_credit_profile")) {
            try {
                List<CreditProfileSummary> rows = jdbcTemplate.query(
                        """
                        SELECT score, rating, dispute_count, projects_completed, success_rate
                        FROM subject_credit_profile
                        WHERE subject_id = ?
                          AND subject_type = ?
                          AND del_token = '0'
                        LIMIT 1
                        """,
                        (rs, rowNum) -> new CreditProfileSummary(
                                rs.getInt("score"),
                                rs.getString("rating"),
                                rs.getInt("dispute_count"),
                                rs.getInt("projects_completed"),
                                rs.getInt("success_rate")
                        ),
                        ref.id(),
                        ref.type().name()
                );
                if (!rows.isEmpty()) {
                    return rows.get(0);
                }
            } catch (DataAccessException ignored) {
                // Use adapter below when the local table is not ready.
            }
        }
        int adapted = adaptCreditScore(legacyScore);
        return new CreditProfileSummary(adapted, rating(adapted), 0, 0, 0);
    }

    AgentWalletState agentWalletState(Long agentId) {
        if (agentId == null || !tableExists("agent_wallet_state")) {
            return null;
        }
        try {
            List<AgentWalletState> rows = jdbcTemplate.query(
                    """
                    SELECT wallet_enabled, enabled_by, status_reason
                    FROM agent_wallet_state
                    WHERE agent_id = ?
                      AND del_token = '0'
                    LIMIT 1
                    """,
                    (rs, rowNum) -> new AgentWalletState(
                            rs.getBoolean("wallet_enabled"),
                            rs.getObject("enabled_by", Long.class),
                            rs.getString("status_reason")
                    ),
                    agentId
            );
            return rows.isEmpty() ? null : rows.get(0);
        } catch (DataAccessException ignored) {
            return null;
        }
    }

    boolean upsertAgentWalletState(Long agentId, boolean enabled, Long operatorHumanId, String reason) {
        if (agentId == null || operatorHumanId == null || !tableExists("agent_wallet_state")) {
            return false;
        }
        String statusReason = boundedStatusReason(reason);
        if (enabled) {
            jdbcTemplate.update(
                    """
                    INSERT INTO agent_wallet_state (
                        agent_id, wallet_enabled, enabled_at, enabled_by, disabled_at, disabled_by,
                        status_reason, del_token, create_by, update_by, create_time, update_time
                    )
                    VALUES (?, TRUE, NOW(), ?, NULL, NULL, ?, '0', ?, ?, NOW(), NOW())
                    ON DUPLICATE KEY UPDATE
                        wallet_enabled = TRUE,
                        enabled_at = NOW(),
                        enabled_by = VALUES(enabled_by),
                        disabled_at = NULL,
                        disabled_by = NULL,
                        status_reason = VALUES(status_reason),
                        del_token = '0',
                        update_by = VALUES(update_by),
                        update_time = NOW()
                    """,
                    agentId,
                    operatorHumanId,
                    statusReason,
                    operatorHumanId,
                    operatorHumanId
            );
            return true;
        }

        jdbcTemplate.update(
                """
                INSERT INTO agent_wallet_state (
                    agent_id, wallet_enabled, enabled_at, enabled_by, disabled_at, disabled_by,
                    status_reason, del_token, create_by, update_by, create_time, update_time
                )
                VALUES (?, FALSE, NULL, NULL, NOW(), ?, ?, '0', ?, ?, NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                    wallet_enabled = FALSE,
                    disabled_at = NOW(),
                    disabled_by = VALUES(disabled_by),
                    status_reason = VALUES(status_reason),
                    del_token = '0',
                    update_by = VALUES(update_by),
                    update_time = NOW()
                """,
                agentId,
                operatorHumanId,
                statusReason,
                operatorHumanId,
                operatorHumanId
        );
        return true;
    }

    boolean sourceConfigWalletEnabled(String raw) {
        if (!demoFallbackEnabled) {
            return false;
        }
        if (!StringUtils.hasText(raw)) {
            return false;
        }
        // Legacy/local compatibility only; agent_wallet_state remains authoritative.
        try {
            JsonNode node = objectMapper.readTree(raw);
            JsonNode wallet = node.get("wallet");
            if (wallet != null && wallet.isTextual()) {
                return "enabled".equalsIgnoreCase(wallet.asText());
            }
            JsonNode walletEnabled = node.get("walletEnabled");
            return walletEnabled != null && walletEnabled.asBoolean(false);
        } catch (Exception ignored) {
            return raw.toLowerCase(Locale.ROOT).contains("wallet")
                    && raw.toLowerCase(Locale.ROOT).contains("enabled");
        }
    }

    List<String> parseCapabilityTags(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            JsonNode array = node.isArray() ? node : node.get("capabilities");
            if (array != null && array.isArray()) {
                List<String> out = new ArrayList<>();
                for (JsonNode item : array) {
                    if (item != null && item.isTextual() && StringUtils.hasText(item.asText())) {
                        out.add(item.asText().trim());
                    }
                }
                return out;
            }
        } catch (Exception ignored) {
            // Try delimiter parsing below.
        }
        return List.of(raw.split("[,，;；]")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    SubjectStatus userStatus(ActorSourceRepository.Human profile) {
        if (profile == null || profile.getStatus() == null) {
            return SubjectStatus.UNKNOWN;
        }
        return switch (profile.getStatus()) {
            case ACTIVE -> SubjectStatus.ACTIVE;
            case INACTIVE -> SubjectStatus.INACTIVE;
            case BANNED -> SubjectStatus.BANNED;
            case UNKNOWN -> SubjectStatus.UNKNOWN;
        };
    }

    SubjectStatus agentStatus(ActorSourceRepository.Agent profile) {
        if (profile == null || profile.getStatus() == null) {
            return SubjectStatus.UNKNOWN;
        }
        return switch (profile.getStatus()) {
            case ACTIVE -> SubjectStatus.ACTIVE;
            case INACTIVE -> SubjectStatus.INACTIVE;
            case SUSPENDED -> SubjectStatus.SUSPENDED;
            case UNKNOWN -> SubjectStatus.UNKNOWN;
        };
    }

    int adaptCreditScore(Integer score) {
        if (score == null) {
            return 300;
        }
        if (score >= 300 && score <= 850) {
            return score;
        }
        if (score >= 0 && score <= 100) {
            return clamp(300 + Math.round(score * 5.5f), 300, 850);
        }
        return clamp(score, 300, 850);
    }

    String rating(Integer score) {
        int s = score != null ? score : 300;
        if (s >= 750) return "EXCELLENT";
        if (s >= 680) return "GOOD";
        if (s >= 600) return "FAIR";
        return "BASE";
    }

    String displayName(ActorSourceRepository.Human profile) {
        if (profile == null) {
            return null;
        }
        if (StringUtils.hasText(profile.getNickname())) {
            return profile.getNickname();
        }
        if (StringUtils.hasText(profile.getEmail())) {
            return profile.getEmail();
        }
        if (StringUtils.hasText(profile.getPhone())) {
            return profile.getPhone();
        }
        return "User";
    }

    String displayName(ActorSourceRepository.Agent profile) {
        if (profile == null) {
            return null;
        }
        return StringUtils.hasText(profile.getName()) ? profile.getName() : "Agent";
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String boundedStatusReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.length() <= STATUS_REASON_MAX_LENGTH
                ? trimmed
                : trimmed.substring(0, STATUS_REASON_MAX_LENGTH);
    }

    record AgentWalletState(boolean walletEnabled, Long enabledBy, String statusReason) {
    }
}
