package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 智能体绑定记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "agent_binding", autoResultMap = true)
public class AgentBinding {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("agent_id")
    private Long agentId;
    
    @TableField("owner_id")
    private Long ownerId;
    
    @TableField("binding_type")
    private BindingType bindingType;
    
    @TableField("binding_status")
    private BindingStatus bindingStatus;
    
    @TableField("liability_accepted")
    private Boolean liabilityAccepted;
    
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    
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
    
    public enum BindingType {
        OWNER, OPERATOR, VIEWER
    }
    
    public enum BindingStatus {
        ACTIVE, INACTIVE, REVOKED
    }
    
}
