package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 项目主表 - 含审计字段与软删除。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "project", autoResultMap = true)
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("status")
    private ProjectStatus status;

    @TableField("color")
    private String color;

    @TableField("revenue")
    private String revenue;

    @TableField("bid")
    private Long bid;

    @TableField("deposit_paid")
    private Boolean depositPaid;

    @TableField("deadline")
    private String deadline;

    @TableField("progress")
    private Integer progress;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("owner_type")
    private ProjectOwnerType ownerType;

    // 当 owner_type=AGENT 时，记录其人类主人（用于可见性）
    @TableField("agent_owner_master_id")
    private Long agentOwnerMasterId;

    @TableField("agent_fully_authorized")
    private Boolean agentFullyAuthorized;

    /**
     * 预留：待处理智能体决策（JSON 字符串口径）
     */
    @TableField("pending_agent_decisions")
    private String pendingAgentDecisions;

    /**
     * 预留：待处理 bid 更新（JSON 字符串口径）
     */
    @TableField("pending_bid_update")
    private String pendingBidUpdate;

    /**
     * 预留：待处理所有权转让（JSON 字符串口径）
     */
    @TableField("pending_ownership_transfer")
    private String pendingOwnershipTransfer;

    // ========== 审计字段 ==========

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableLogic
    private Long delToken;

    public enum ProjectStatus {
        ACTIVE, PAUSED, COMPLETED
    }

    public enum ProjectOwnerType {
        HUMAN, AGENT
    }
}

