package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 消息反应实体（表情回复）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "message_reaction", autoResultMap = true)
public class MessageReaction {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("message_id")
    private Long messageId;
    
    @TableField("reactor_id")
    private Long reactorId;
    
    @TableField("reactor_type")
    private ReactorType reactorType;
    
    @TableField("reaction_type")
    private String reactionType;
    
    @TableField("reaction_content")
    private String reactionContent;
    
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
    
    public enum ReactorType {
        USER, AGENT
    }
    
}
