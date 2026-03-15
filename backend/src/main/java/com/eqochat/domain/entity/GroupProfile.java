package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 群组资料实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "group_profile", autoResultMap = true)
public class GroupProfile {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("conversation_id")
    private Long conversationId;
    
    @TableField("group_name")
    private String groupName;
    
    @TableField("group_avatar")
    private String groupAvatar;
    
    @TableField("description")
    private String description;
    
    @TableField("owner_id")
    private Long ownerId;
    
    @TableField("max_members")
    private Integer maxMembers;
    
    @TableField("member_count")
    private Integer memberCount;
    
    @TableField("group_type")
    private GroupType groupType;
    
    @TableField("join_type")
    private JoinType joinType;
    
    @TableField("status")
    private GroupStatus status;
    
    @TableField("settings")
    private String settings;
    
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
    private String delToken;
    
    public enum GroupType {
        GENERAL, WORK, INTEREST
    }
    
    public enum JoinType {
        FREE, APPROVAL, INVITE_ONLY
    }
    
    public enum GroupStatus {
        ACTIVE, DISBANDED
    }
    
    public boolean isDeleted() {
        return delToken != null && !"0".equals(delToken);
    }
}
