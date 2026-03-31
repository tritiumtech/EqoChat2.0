package com.eqochat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 项目文件记录表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "project_file", autoResultMap = true)
public class ProjectFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_type")
    private String fileType;

    @TableField("download_url")
    private String downloadUrl;

    @TableField("uploaded_by_id")
    private Long uploadedById;

    @TableField("uploaded_by_type")
    private UploaderType uploadedByType;

    @TableField("uploaded_by_name")
    private String uploadedByName;

    @TableField("size")
    private String size;

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

    public enum UploaderType {
        HUMAN, AGENT
    }
}

