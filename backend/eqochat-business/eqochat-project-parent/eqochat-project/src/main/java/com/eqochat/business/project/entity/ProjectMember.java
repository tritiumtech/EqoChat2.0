package com.eqochat.business.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 项目成员表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "project_member", autoResultMap = true)
public class ProjectMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("member_id")
    private Long memberId;

    @TableField("member_type")
    private MemberType memberType;

    @TableField("name")
    private String name;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField("is_online")
    private Boolean isOnline;

    @TableField("master_id")
    private Long masterId;

    @TableField("credit_score")
    private Integer creditScore;

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

    public enum MemberType {
        HUMAN, AGENT
    }
}

