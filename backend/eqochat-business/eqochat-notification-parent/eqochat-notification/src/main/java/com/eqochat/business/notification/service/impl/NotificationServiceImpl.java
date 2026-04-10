package com.eqochat.business.notification.service.impl;

import com.eqochat.framework.common.BizException;
import com.eqochat.business.notification.entity.Notification;
import com.eqochat.business.notification.api.dto.response.NotificationResponse;
import com.eqochat.business.notification.mapper.NotificationMapper;
import com.eqochat.business.notification.api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public List<NotificationResponse> listMyNotifications(Long userId, Integer limit) {
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, 100);
        List<Notification> list = notificationMapper.findByRecipientId(userId);
        if (list.isEmpty()) return List.of();
        return list.stream()
                .limit(size)
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .type(n.getNotificationType() != null ? n.getNotificationType().name() : null)
                        .title(n.getTitle())
                        .content(n.getContent())
                        .read(Boolean.TRUE.equals(n.getIsRead()))
                        .createTime(n.getCreateTime())
                        .build())
                .toList();
    }

    @Override
    public void markRead(Long userId, Long notificationId) {
        if (notificationId == null) throw BizException.of("notification.id.required");
        Notification n = notificationMapper.selectById(notificationId);
        if (n == null || (n.getDelToken() != null && n.getDelToken() != 0L)) {
            throw BizException.of("notification.not_found");
        }
        if (!userId.equals(n.getRecipientId())) {
            throw BizException.of("notification.access.denied");
        }
        if (Boolean.TRUE.equals(n.getIsRead())) {
            return;
        }
        n.setIsRead(true);
        n.setReadAt(LocalDateTime.now());
        notificationMapper.updateById(n);
    }

    @Override
    @Transactional
    public void sendNotification(Long recipientId, String type, String title, String content, String data, Long senderId) {
        if (recipientId == null) {
            log.warn("发送通知失败: recipientId为空");
            return;
        }
        if (!StringUtils.hasText(title)) {
            log.warn("发送通知失败: title为空");
            return;
        }

        Notification.NotificationType notificationType;
        try {
            notificationType = Notification.NotificationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            log.warn("未知通知类型: {}, 使用SYSTEM类型", type);
            notificationType = Notification.NotificationType.SYSTEM;
        }

        // 截断内容避免过长
        String shortContent = content;
        if (shortContent != null && shortContent.length() > 500) {
            shortContent = shortContent.substring(0, 497) + "...";
        }

        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .recipientType(Notification.RecipientType.USER)
                .notificationType(notificationType)
                .title(title)
                .content(shortContent)
                .data(data)
                .senderId(senderId)
                .senderType(senderId != null ? Notification.SenderType.USER : Notification.SenderType.SYSTEM)
                .isRead(false)
                .priority(Notification.Priority.NORMAL)
                .build();

        try {
            notificationMapper.insert(notification);
            log.debug("发送通知成功: recipientId={}, type={}, title={}", recipientId, type, title);
        } catch (Exception e) {
            log.error("发送通知失败: recipientId={}, type={}", recipientId, type, e);
        }
    }
}

