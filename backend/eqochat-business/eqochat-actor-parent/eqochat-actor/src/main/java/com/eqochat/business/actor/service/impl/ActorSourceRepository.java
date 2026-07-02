package com.eqochat.business.actor.service.impl;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
class ActorSourceRepository {

    private final JdbcTemplate jdbcTemplate;

    ActorSourceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<Human> findHuman(Long humanId) {
        if (humanId == null) {
            return Optional.empty();
        }
        try {
            List<Human> rows = jdbcTemplate.query(
                    """
                    SELECT id, did, phone, email, nickname, avatar_url, bio, status, credit_score
                    FROM user_profile
                    WHERE id = ?
                      AND del_token = '0'
                    LIMIT 1
                    """,
                    this::mapHuman,
                    humanId
            );
            return rows.stream().findFirst();
        } catch (DataAccessException ignored) {
            return Optional.empty();
        }
    }

    Optional<Agent> findAgent(Long agentId) {
        if (agentId == null) {
            return Optional.empty();
        }
        try {
            List<Agent> rows = jdbcTemplate.query(
                    """
                    SELECT id, did, owner_id, name, avatar_url, description, status,
                           credit_score, capability_tags, source_config
                    FROM agent_profile
                    WHERE id = ?
                      AND del_token = '0'
                    LIMIT 1
                    """,
                    this::mapAgent,
                    agentId
            );
            return rows.stream().findFirst();
        } catch (DataAccessException ignored) {
            return Optional.empty();
        }
    }

    Optional<Binding> findOwnerBinding(Long agentId, Long ownerId) {
        if (agentId == null || ownerId == null) {
            return Optional.empty();
        }
        try {
            List<Binding> rows = jdbcTemplate.query(
                    """
                    SELECT agent_id, owner_id, binding_type, binding_status, liability_accepted
                    FROM agent_binding
                    WHERE agent_id = ?
                      AND owner_id = ?
                      AND del_token = '0'
                    LIMIT 1
                    """,
                    this::mapBinding,
                    agentId,
                    ownerId
            );
            return rows.stream().findFirst();
        } catch (DataAccessException ignored) {
            return Optional.empty();
        }
    }

    private Human mapHuman(ResultSet rs, int rowNum) throws SQLException {
        return new Human(
                rs.getObject("id", Long.class),
                rs.getString("did"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("nickname"),
                rs.getString("avatar_url"),
                rs.getString("bio"),
                humanStatus(rs.getString("status")),
                rs.getObject("credit_score", Integer.class)
        );
    }

    private Agent mapAgent(ResultSet rs, int rowNum) throws SQLException {
        return new Agent(
                rs.getObject("id", Long.class),
                rs.getString("did"),
                rs.getObject("owner_id", Long.class),
                rs.getString("name"),
                rs.getString("avatar_url"),
                rs.getString("description"),
                agentStatus(rs.getString("status")),
                rs.getObject("credit_score", Integer.class),
                rs.getString("capability_tags"),
                rs.getString("source_config")
        );
    }

    private Binding mapBinding(ResultSet rs, int rowNum) throws SQLException {
        return new Binding(
                rs.getObject("agent_id", Long.class),
                rs.getObject("owner_id", Long.class),
                bindingType(rs.getString("binding_type")),
                bindingStatus(rs.getString("binding_status")),
                rs.getObject("liability_accepted", Boolean.class)
        );
    }

    private static HumanStatus humanStatus(String raw) {
        return enumValue(raw, HumanStatus.UNKNOWN, HumanStatus.class);
    }

    private static AgentStatus agentStatus(String raw) {
        return enumValue(raw, AgentStatus.UNKNOWN, AgentStatus.class);
    }

    private static BindingType bindingType(String raw) {
        return enumValue(raw, BindingType.UNKNOWN, BindingType.class);
    }

    private static BindingStatus bindingStatus(String raw) {
        return enumValue(raw, BindingStatus.UNKNOWN, BindingStatus.class);
    }

    private static <T extends Enum<T>> T enumValue(String raw, T fallback, Class<T> enumType) {
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumType, raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    record Human(
            Long id,
            String did,
            String phone,
            String email,
            String nickname,
            String avatarUrl,
            String bio,
            HumanStatus status,
            Integer creditScore
    ) {
        Long getId() {
            return id;
        }

        Integer getCreditScore() {
            return creditScore;
        }

        HumanStatus getStatus() {
            return status;
        }

        String getNickname() {
            return nickname;
        }

        String getEmail() {
            return email;
        }

        String getPhone() {
            return phone;
        }
    }

    enum HumanStatus {
        ACTIVE, INACTIVE, BANNED, UNKNOWN
    }

    record Agent(
            Long id,
            String did,
            Long ownerId,
            String name,
            String avatarUrl,
            String description,
            AgentStatus status,
            Integer creditScore,
            String capabilityTags,
            String sourceConfig
    ) {
        Long getId() {
            return id;
        }

        Long getOwnerId() {
            return ownerId;
        }

        Integer getCreditScore() {
            return creditScore;
        }

        String getCapabilityTags() {
            return capabilityTags;
        }

        String getSourceConfig() {
            return sourceConfig;
        }

        AgentStatus getStatus() {
            return status;
        }

        String getName() {
            return name;
        }
    }

    enum AgentStatus {
        ACTIVE, INACTIVE, SUSPENDED, UNKNOWN
    }

    record Binding(
            Long agentId,
            Long ownerId,
            BindingType bindingType,
            BindingStatus bindingStatus,
            Boolean liabilityAccepted
    ) {
    }

    enum BindingType {
        OWNER, OPERATOR, VIEWER, UNKNOWN
    }

    enum BindingStatus {
        ACTIVE, INACTIVE, REVOKED, UNKNOWN
    }
}
