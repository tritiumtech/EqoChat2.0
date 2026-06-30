package com.eqochat.business.contact.api.dto.response;

import com.eqochat.business.actor.api.model.SubjectType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友申请响应
 */
@Data
@Builder
public class FriendRequestResponse {

    private Long id;
    private Long requesterSubjectId;
    private SubjectType requesterSubjectType;
    private Long recipientSubjectId;
    private SubjectType recipientSubjectType;
    private String requestMessage;
    private String status;
    private LocalDateTime createTime;

    /** 申请者昵称（用于收到的申请列表展示） */
    private String requesterNickname;
    private String requesterAvatarUrl;

    /** 接收者昵称（用于发出的申请列表展示） */
    private String recipientNickname;
    private String recipientAvatarUrl;
}
