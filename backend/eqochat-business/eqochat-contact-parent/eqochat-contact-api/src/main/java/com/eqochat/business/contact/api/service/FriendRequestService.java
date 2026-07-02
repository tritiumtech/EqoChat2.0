package com.eqochat.business.contact.api.service;

import com.eqochat.business.contact.api.dto.request.SendFriendRequestRequest;
import com.eqochat.business.contact.api.dto.response.FriendRequestResponse;
import com.eqochat.business.actor.api.model.SubjectRef;

import java.util.List;

/**
 * 好友申请服务
 */
public interface FriendRequestService {

    /**
     * 发送好友申请
     */
    FriendRequestResponse sendRequest(Long userId, SendFriendRequestRequest request);

    /**
     * 同意好友申请
     */
    void accept(Long userId, Long requestId);

    /**
     * 拒绝好友申请
     */
    void reject(Long userId, Long requestId);

    /**
     * 我收到的待处理申请
     */
    List<FriendRequestResponse> listReceived(Long userId, SubjectRef recipient);

    /**
     * 我发出的申请
     */
    List<FriendRequestResponse> listSent(Long userId, SubjectRef requester);
}
