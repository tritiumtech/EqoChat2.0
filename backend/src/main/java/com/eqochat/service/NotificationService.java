package com.eqochat.service;

import com.eqochat.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> listMyNotifications(Long userId, Integer limit);

    void markRead(Long userId, Long notificationId);
}

