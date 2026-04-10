package com.eqochat.business.notification.api.service;

import com.eqochat.business.notification.api.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> listMyNotifications(Long userId, Integer limit);

    void markRead(Long userId, Long notificationId);

    /**
     * 发送通知给指定用户
     *
     * @param recipientId 接收者ID
     * @param type 通知类型
     * @param title 标题
     * @param content 内容
     * @param data 附加数据（JSON格式）
     * @param senderId 发送者ID（可选）
     */
    void sendNotification(Long recipientId, String type, String title, String content, String data, Long senderId);
}

