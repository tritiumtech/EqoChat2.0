package com.eqochat.business.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.eqochat.business.actor.api.model.SubjectType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 消息实体 - 含审计字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "message", autoResultMap = true)
public class Message {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("conversation_id")
    private Long conversationId;
    
    @TableField("sender_id")
    private Long senderId;
    
    @TableField("sender_type")
    private SubjectType senderType;

    @TableField("liable_human_id")
    private Long liableHumanId;
    
    @TableField("message_type")
    private String messageType;
    
    @TableField("content")
    private String content;
    
    @TableField("content_metadata")
    private String contentMetadata;
    
    @TableField("intent_data")
    private String intentData;
    
    @TableField("reply_to_message_id")
    private Long replyToMessageId;
    
    @TableField("forward_from_message_id")
    private Long forwardFromMessageId;
    
    @TableField("status")
    private String status;
    
    @TableField("edited_at")
    private LocalDateTime editedAt;
    
    @TableField("deleted_at")
    private LocalDateTime deletedAt;
    
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
    
}
