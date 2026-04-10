package com.eqochat.business.contact.controller.friend;

import com.eqochat.framework.common.ApiResponse;
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
    public ApiResponse<List<FriendRequestResponse>> listReceived() {
        return ApiResponse.success(friendRequestService.listReceived(UserContext.requireCurrentUser()));
    }

    @GetMapping("/sent")
    public ApiResponse<List<FriendRequestResponse>> listSent() {
        return ApiResponse.success(friendRequestService.listSent(UserContext.requireCurrentUser()));
    }
}
