package com.eqochat.dto.response;

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
    private Long requesterId;
    private Long recipientId;
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
