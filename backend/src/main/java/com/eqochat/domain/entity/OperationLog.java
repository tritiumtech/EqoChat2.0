package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "operation_log", autoResultMap = true)
public class OperationLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("operator_id")
    private Long operatorId;
    
    @TableField("operator_type")
    private OperatorType operatorType;
    
    @TableField("operation_type")
    private String operationType;
    
    @TableField("target_type")
    private String targetType;
    
    @TableField("target_id")
    private Long targetId;
    
    @TableField("operation_data")
    private String operationData;
    
    @TableField("ip_address")
    private String ipAddress;
    
    @TableField("user_agent")
    private String userAgent;
    
    @TableField("log_date")
    private java.sql.Date logDate;
    
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
    
    public enum OperatorType {
        USER, AGENT
    }
    
    public boolean isDeleted() {
        return delToken != null && delToken != 0L;
    }
}
