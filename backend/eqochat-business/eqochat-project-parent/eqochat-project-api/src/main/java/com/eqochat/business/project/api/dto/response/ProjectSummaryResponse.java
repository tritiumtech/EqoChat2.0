package com.eqochat.business.project.api.dto.response;

import com.eqochat.business.actor.api.model.SubjectType;
import lombok.*;

/**
 * 项目列表返回对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSummaryResponse {

    private Long id;
    private String name;
    private String status;
    private String color;

    private int humans;
    private int agents;

    /**
     * 收益展示口径（前端展示用）。
     */
    private String revenue;

    private Long bid;
    private boolean depositPaid;
    private String deadline;
    private int progress;

    private Long ownerSubjectId;
    private SubjectType ownerSubjectType;
    private String ownerDisplayName;
    private Long associatedHumanId;
    private String associatedHumanName;
    private Long liableHumanId;
    private boolean agentFullyAuthorized;
    private String walletRouting;
    private String responsibilityChain;

    /**
     * 是否存在待处理 bid 更新（用于列表角标展示）。
     */
    private boolean pendingBidUpdate;
}
