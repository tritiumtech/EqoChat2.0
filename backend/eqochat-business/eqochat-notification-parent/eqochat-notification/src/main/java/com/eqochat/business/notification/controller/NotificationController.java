package com.eqochat.business.notification.controller;

import com.eqochat.framework.common.ApiResponse;
import com.eqochat.framework.common.UserContext;
import com.eqochat.business.notification.api.dto.request.MarkNotificationReadRequest;
import com.eqochat.business.notification.api.dto.response.NotificationResponse;
import com.eqochat.business.notification.api.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> listMine(@RequestParam(required = false) Integer limit) {
        return ApiResponse.success(notificationService.listMyNotifications(UserContext.requireCurrentUser(), limit));
    }

    @PostMapping("/read")
    public ApiResponse<Void> markRead(@RequestBody @Valid MarkNotificationReadRequest request) {
        notificationService.markRead(UserContext.requireCurrentUser(), request.getNotificationId());
        return ApiResponse.success(null);
    }
}

