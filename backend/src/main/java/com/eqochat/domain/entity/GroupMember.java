package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 群组成员实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "group_member", autoResultMap = true)
public class GroupMember {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("group_id")
    private Long groupId;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("role")
    private MemberRole role;
    
    @TableField("group_nickname")
    private String groupNickname;
    
    @TableField("join_time")
    private LocalDateTime joinTime;
    
    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;
    
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
    
    public enum MemberRole {
        OWNER, ADMIN, MEMBER
    }
    
}
