package com.eqochat.business.contact.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "contact_relationship", autoResultMap = true)
public class ContactRelationship {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("user_type")
    private RelationshipSubjectType userType;

    @TableField("friend_id")
    private Long friendId;

    @TableField("friend_type")
    private RelationshipSubjectType friendType;

    @TableField("remark_name")
    private String remarkName;

    @TableField("status")
    private RelationshipStatus status;

    @TableField("add_source")
    private String addSource;

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

    public enum RelationshipSubjectType {
        HUMAN, AGENT
    }

    public enum RelationshipStatus {
        ACTIVE, DELETED, BLOCKED
    }
}
