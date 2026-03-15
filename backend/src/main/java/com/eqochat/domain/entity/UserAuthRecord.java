package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 用户认证记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "user_auth_record", autoResultMap = true)
public class UserAuthRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("auth_type")
    private AuthType authType;
    
    @TableField("auth_provider")
    private String authProvider;
    
    @TableField("auth_identifier")
    private String authIdentifier;
    
    @TableField("verified")
    private Boolean verified;
    
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
    
    public enum AuthType {
        PASSWORD, SMS, EMAIL, OAUTH
    }
    
    public boolean isDeleted() {
        return delToken != null && delToken != 0L;
    }
}
