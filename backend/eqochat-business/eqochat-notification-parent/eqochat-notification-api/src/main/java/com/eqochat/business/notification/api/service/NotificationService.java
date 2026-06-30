package com.eqochat.business.notification.api.service;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.notification.api.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> listNotifications(SubjectRef recipient, Integer limit);

    List<NotificationResponse> listUnreadNotifications(SubjectRef recipient, Integer limit);

    long countUnread(SubjectRef recipient);

    void markRead(SubjectRef recipient, Long notificationId);

    void markAllRead(SubjectRef recipient);

    void sendNotification(
            SubjectRef recipient,
            String type,
            String title,
            String content,
            String data,
            SubjectRef sender
    );
}
