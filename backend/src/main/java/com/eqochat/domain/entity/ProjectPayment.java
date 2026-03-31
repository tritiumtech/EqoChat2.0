package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 项目支付记录表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "project_payment", autoResultMap = true)
public class ProjectPayment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("amount")
    private Long amount;

    @TableField("recipient_id")
    private Long recipientId;

    @TableField("recipient_type")
    private RecipientType recipientType;

    @TableField("recipient_name")
    private String recipientName;

    @TableField("master_wallet")
    private String masterWallet;

    @TableField("status")
    private PaymentStatus status;

    @TableField("date")
    private String date;

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

    public enum RecipientType {
        HUMAN, AGENT
    }

    public enum PaymentStatus {
        PENDING, PAID, INVOICED
    }
}

