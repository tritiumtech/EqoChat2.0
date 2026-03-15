package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 会话实体 - 含审计字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "conversation", autoResultMap = true)
public class Conversation {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("conversation_type")
    private String conversationType;
    
    @TableField("title")
    private String title;
    
    @TableField("avatar_url")
    private String avatarUrl;
    
    @TableField("creator_id")
    private Long creatorId;
    
    @TableField("last_message_id")
    private Long lastMessageId;
    
    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;
    
    @TableField("unread_count")
    private Integer unreadCount;
    
    @TableField("status")
    private String status;
    
    @TableField("settings")
    private String settings;
    
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
    
    @TableField(value = "del_token", fill = FieldFill.INSERT)
    private Long delToken;
    
    public boolean isDeleted() {
        return delToken != null && delToken != 0L;
    }
}
