package com.eqochat.business.credit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 违规记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "violation_record", autoResultMap = true)
public class ViolationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("subject_id")
    private Long subjectId;

    @TableField("subject_type")
    private SubjectType subjectType;

    @TableField("violation_type")
    private String violationType;

    @TableField("severity")
    private Severity severity;

    @TableField("description")
    private String description;

    @TableField("evidence")
    private String evidence;

    @TableField("punishment")
    private String punishment;

    @TableField("punished_until")
    private LocalDateTime punishedUntil;

    @TableField("reporter_id")
    private Long reporterId;

    @TableField("reporter_type")
    private SubjectType reporterType;

    @TableField("reviewer_id")
    private Long reviewerId;

    @TableField("reviewer_type")
    private SubjectType reviewerType;

    @TableField("status")
    private ViolationStatus status;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("synced_to_platforms")
    private String syncedToPlatforms;

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
        HUMAN, AGENT, SYSTEM
    }

    public enum Severity {
        MINOR, MODERATE, SEVERE, CRITICAL
    }

    public enum ViolationStatus {
        PENDING, CONFIRMED, REJECTED, APPEALED
    }
}
