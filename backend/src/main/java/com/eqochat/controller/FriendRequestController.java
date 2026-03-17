package com.eqochat.controller;

import com.eqochat.common.ApiResponse;
import com.eqochat.common.BizException;
import com.eqochat.dto.request.SendFriendRequestRequest;
import com.eqochat.dto.response.FriendRequestResponse;
import com.eqochat.service.FriendRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Long userId = resolveUserId();
        return ApiResponse.success(friendRequestService.sendRequest(userId, request));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<Void> accept(@PathVariable Long id) {
        Long userId = resolveUserId();
        friendRequestService.accept(userId, id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id) {
        Long userId = resolveUserId();
        friendRequestService.reject(userId, id);
        return ApiResponse.success(null);
    }

    @GetMapping("/received")
    public ApiResponse<List<FriendRequestResponse>> listReceived() {
        Long userId = resolveUserId();
        return ApiResponse.success(friendRequestService.listReceived(userId));
    }

    @GetMapping("/sent")
    public ApiResponse<List<FriendRequestResponse>> listSent() {
        Long userId = resolveUserId();
        return ApiResponse.success(friendRequestService.listSent(userId));
    }

    private Long resolveUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw BizException.of(401, "auth.token.invalid");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        throw BizException.of(401, "auth.token.invalid");
    }
}
