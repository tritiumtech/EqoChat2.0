package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 会话参与者实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "conversation_participant", autoResultMap = true)
public class ConversationParticipant {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("conversation_id")
    private Long conversationId;
    
    @TableField("participant_id")
    private Long participantId;
    
    @TableField("participant_type")
    private ParticipantType participantType;
    
    @TableField("role")
    private Role role;
    
    @TableField("nickname_in_conv")
    private String nicknameInConv;
    
    @TableField("joined_at")
    private LocalDateTime joinedAt;
    
    @TableField("last_read_message_id")
    private Long lastReadMessageId;
    
    @TableField("last_read_at")
    private LocalDateTime lastReadAt;
    
    @TableField("is_muted")
    private Boolean isMuted;
    
    @TableField("mute_until")
    private LocalDateTime muteUntil;
    
    // ========== 审计字段 ==========
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    @TableField(value = "del_token", fill = FieldFill.INSERT)
    private Long delToken;
    
    public enum ParticipantType {
        USER, AGENT
    }
    
    public enum Role {
        OWNER, ADMIN, MEMBER
    }
    
    public boolean isDeleted() {
        return delToken != null && delToken != 0L;
    }
}
