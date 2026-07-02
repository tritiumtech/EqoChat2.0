package com.eqochat.business.contact.controller.friend;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.framework.common.ApiResponse;
import com.eqochat.framework.common.BizException;
import com.eqochat.framework.common.UserContext;
import com.eqochat.business.contact.api.dto.request.SendFriendRequestRequest;
import com.eqochat.business.contact.api.dto.response.FriendRequestResponse;
import com.eqochat.business.contact.api.service.FriendRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友申请接口
 */
@RestController
@RequestMapping("/api/v1/friend-requests")
@RequiredArgsConstructor
@Slf4j
public class FriendRequestController {

    private final FriendRequestService friendRequestService;

    @PostMapping
    public ApiResponse<FriendRequestResponse> sendRequest(@RequestBody @Valid SendFriendRequestRequest request) {
        return ApiResponse.success(friendRequestService.sendRequest(UserContext.requireCurrentUser(), request));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<Void> accept(@PathVariable Long id) {
        friendRequestService.accept(UserContext.requireCurrentUser(), id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id) {
        friendRequestService.reject(UserContext.requireCurrentUser(), id);
        return ApiResponse.success(null);
    }

    @GetMapping("/received")
    public ApiResponse<List<FriendRequestResponse>> listReceived(
            @RequestParam(required = false) Long recipientSubjectId,
            @RequestParam(required = false) SubjectType recipientSubjectType) {
        SubjectRef recipient = requireSubject(recipientSubjectId, recipientSubjectType);
        return ApiResponse.success(friendRequestService.listReceived(UserContext.requireCurrentUser(), recipient));
    }

    @GetMapping("/sent")
    public ApiResponse<List<FriendRequestResponse>> listSent(
            @RequestParam(required = false) Long requesterSubjectId,
            @RequestParam(required = false) SubjectType requesterSubjectType) {
        SubjectRef requester = requireSubject(requesterSubjectId, requesterSubjectType);
        return ApiResponse.success(friendRequestService.listSent(UserContext.requireCurrentUser(), requester));
    }

    private static SubjectRef requireSubject(Long subjectId, SubjectType subjectType) {
        if (subjectId == null || subjectType == null || subjectType == SubjectType.SYSTEM) {
            throw BizException.of("friend_request.subject.invalid");
        }
        return new SubjectRef(subjectId, subjectType);
    }
}
