package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 信用记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "credit_record", autoResultMap = true)
public class CreditRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("subject_id")
    private Long subjectId;
    
    @TableField("subject_type")
    private SubjectType subjectType;
    
    @TableField("change_amount")
    private Integer changeAmount;
    
    @TableField("current_score")
    private Integer currentScore;
    
    @TableField("reason")
    private String reason;
    
    @TableField("related_type")
    private String relatedType;
    
    @TableField("related_id")
    private Long relatedId;
    
    @TableField("operator_id")
    private Long operatorId;
    
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
    
    public enum SubjectType {
        USER, AGENT
    }
    
}
