package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 通知实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "notification", autoResultMap = true)
public class Notification {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("recipient_id")
    private Long recipientId;
    
    @TableField("recipient_type")
    private RecipientType recipientType;
    
    @TableField("notification_type")
    private NotificationType notificationType;
    
    @TableField("title")
    private String title;
    
    @TableField("content")
    private String content;
    
    @TableField("data")
    private String data;
    
    @TableField("sender_id")
    private Long senderId;
    
    @TableField("sender_type")
    private SenderType senderType;
    
    @TableField("is_read")
    private Boolean isRead;
    
    @TableField("read_at")
    private LocalDateTime readAt;
    
    @TableField("priority")
    private Priority priority;
    
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
    
    public enum RecipientType {
        USER, AGENT
    }
    
    public enum SenderType {
        USER, AGENT, SYSTEM
    }
    
    public enum NotificationType {
        SYSTEM, FRIEND_REQUEST, AGENT_BINDING, GROUP_INVITE, 
        MESSAGE_MENTION, CREDIT_CHANGE
    }
    
    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }
    
}
