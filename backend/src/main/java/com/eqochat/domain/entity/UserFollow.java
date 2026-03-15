package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 关注关系实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "user_follow", autoResultMap = true)
public class UserFollow {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("follower_id")
    private Long followerId;
    
    @TableField("following_id")
    private Long followingId;
    
    @TableField("follow_type")
    private FollowType followType;
    
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
    
    public enum FollowType {
        NORMAL, MUTE, BLOCK
    }
    
}
