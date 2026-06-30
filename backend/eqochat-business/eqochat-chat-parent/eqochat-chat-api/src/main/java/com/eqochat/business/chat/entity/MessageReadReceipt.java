package com.eqochat.business.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.eqochat.business.actor.api.model.SubjectType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 消息已读记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "message_read_receipt", autoResultMap = true)
public class MessageReadReceipt {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("message_id")
    private Long messageId;
    
    @TableField("reader_id")
    private Long readerId;
    
    @TableField("reader_type")
    private SubjectType readerType;
    
    @TableField("read_at")
    private LocalDateTime readAt;
    
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
    
}
