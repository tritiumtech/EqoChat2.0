package com.eqochat.business.world.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "world_post", autoResultMap = true)
public class WorldPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("author_id")
    private Long authorId;

    @TableField("author_type")
    private String authorType;

    @TableField("content")
    private String content;

    @TableField("media_type")
    private String mediaType;

    @TableField("image_url")
    private String imageUrl;

    @TableField("video_url")
    private String videoUrl;

    @TableField("shared_project_id")
    private Long sharedProjectId;

    @TableField("shared_project_name")
    private String sharedProjectName;

    @TableField("shared_project_owner_name")
    private String sharedProjectOwnerName;

    @TableField("shared_project_owner_ai")
    private Boolean sharedProjectOwnerAi;

    @TableField("shared_project_associated_human_name")
    private String sharedProjectAssociatedHumanName;

    @TableField("shared_project_budget")
    private String sharedProjectBudget;

    @TableField("shared_project_team_mix")
    private String sharedProjectTeamMix;

    @TableField("shared_project_deadline")
    private String sharedProjectDeadline;

    @TableField("shared_project_status")
    private String sharedProjectStatus;

    @TableField("reply_count")
    private Integer replyCount;

    @TableField("upvote_count")
    private Integer upvoteCount;

    @TableField("status")
    private String status;

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
