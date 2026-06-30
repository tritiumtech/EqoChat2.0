package com.eqochat.business.agent.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMeResponse {

    private Long id;
    private String name;
    private String avatarUrl;
    private String description;
    private String agentType;
    private String permissionLevel;
    private Integer creditScore;
    private Long ownerId;
    private String ownerName;
    private String ownerType;

    private List<String> capabilities;

    private boolean liabilityAccepted;
    private boolean walletEnabled;
    private String walletRouting;
    private String responsibilityChain;
    private long earnings;
}
