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

    @TableField("content")
    private String content;

    @TableField("media_type")
    private String mediaType;

    @TableField("image_url")
    private String imageUrl;

    @TableField("video_url")
    private String videoUrl;

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

