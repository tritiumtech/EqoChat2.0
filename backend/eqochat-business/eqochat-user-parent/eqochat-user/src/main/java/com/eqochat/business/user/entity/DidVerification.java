package com.eqochat.business.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DID验证记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "did_verification", autoResultMap = true)
public class DidVerification {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("did")
    private String did;
    
    @TableField("verification_type")
    private String verificationType;
    
    @TableField("verification_data")
    private String verificationData;
    
    @TableField("verified_at")
    private LocalDateTime verifiedAt;
    
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    
    @TableField("verifier_id")
    private Long verifierId;
    
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
