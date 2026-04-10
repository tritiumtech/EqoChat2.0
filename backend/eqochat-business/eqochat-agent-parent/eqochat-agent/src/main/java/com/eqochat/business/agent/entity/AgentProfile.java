package com.eqochat.business.agent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 智能体资料实体 - 含审计字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "agent_profile", autoResultMap = true)
public class AgentProfile {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("did")
    private String did;
    
    @TableField("owner_id")
    private Long ownerId;
    
    @TableField("name")
    private String name;
    
    @TableField("avatar_url")
    private String avatarUrl;
    
    @TableField("description")
    private String description;
    
    @TableField("agent_type")
    private AgentType agentType;
    
    @TableField("status")
    private AgentStatus status;
    
    @TableField("permission_level")
    private String permissionLevel;
    
    @TableField("credit_score")
    private Integer creditScore;
    
    @TableField("capability_tags")
    private String capabilityTags;
    
    @TableField("source_platform")
    private String sourcePlatform;
    
    @TableField("source_config")
    private String sourceConfig;
    
    // ========== 审计字段 ==========
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 创建人ID
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    
    /**
     * 更新人ID
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    @TableLogic
    private Long delToken;
    
    public enum AgentType {
        GENERAL, PERSONAL, ASSISTANT, BUSINESS
    }
    
    public enum AgentStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
    
}
