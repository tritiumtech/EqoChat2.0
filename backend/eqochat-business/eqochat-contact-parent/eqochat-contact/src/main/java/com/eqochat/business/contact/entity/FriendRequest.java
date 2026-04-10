package com.eqochat.business.contact.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 好友申请实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "friend_request", autoResultMap = true)
public class FriendRequest {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("requester_id")
    private Long requesterId;
    
    @TableField("recipient_id")
    private Long recipientId;
    
    @TableField("request_type")
    private RequestType requestType;
    
    @TableField("request_message")
    private String requestMessage;
    
    @TableField("status")
    private RequestStatus status;
    
    @TableField("responded_at")
    private LocalDateTime respondedAt;
    
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
    
    public enum RequestType {
        FRIEND, AGENT_BINDING, GROUP_INVITE
    }
    
    public enum RequestStatus {
        PENDING, ACCEPTED, REJECTED, EXPIRED
    }
    
}
