package com.eqochat.business.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DID文档实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "did_document", autoResultMap = true)
public class DidDocument {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("did")
    private String did;
    
    @TableField("did_method")
    private String didMethod;
    
    @TableField("document")
    private String document;
    
    @TableField("controller_id")
    private Long controllerId;
    
    @TableField("is_active")
    private Boolean isActive;
    
    @TableField("deactivated_at")
    private LocalDateTime deactivatedAt;
    
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
