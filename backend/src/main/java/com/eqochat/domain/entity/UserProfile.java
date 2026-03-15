package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用户资料实体 - 含审计字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "user_profile", autoResultMap = true)
public class UserProfile {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("did")
    private String did;
    
    @TableField("phone")
    private String phone;
    
    @TableField("email")
    private String email;
    
    @TableField("nickname")
    private String nickname;
    
    @TableField("avatar_url")
    private String avatarUrl;
    
    @TableField("bio")
    private String bio;
    
    @TableField("password_hash")
    private String passwordHash;
    
    @TableField("status")
    private UserStatus status;
    
    @TableField("credit_score")
    private Integer creditScore;
    
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;
    
    @TableField("login_ip")
    private String loginIp;
    
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
    private String delToken;
    
    public enum UserStatus {
        ACTIVE, INACTIVE, BANNED
    }
    
    /**
     * 是否已删除
     */
    public boolean isDeleted() {
        return delToken != null && !"0".equals(delToken);
    }
}
