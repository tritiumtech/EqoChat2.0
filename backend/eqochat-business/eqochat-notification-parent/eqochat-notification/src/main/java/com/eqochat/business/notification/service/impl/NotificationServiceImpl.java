package com.eqochat.business.notification.service.impl;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.notification.api.dto.response.NotificationResponse;
import com.eqochat.business.notification.api.service.NotificationService;
import com.eqochat.business.notification.entity.Notification;
import com.eqochat.business.notification.mapper.NotificationMapper;
import com.eqochat.framework.common.BizException;
import com.eqochat.framework.websocket.WebSocketMessage;
import com.eqochat.framework.websocket.WebSocketSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final WebSocketSender webSocketSender;

    @Override
    public List<NotificationResponse> listNotifications(SubjectRef recipient, Integer limit) {
        SubjectRef ref = requireRecipient(recipient);
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, 100);
        List<Notification> list = notificationMapper.findByRecipient(ref.id(), ref.type().name());
        if (list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .limit(size)
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<NotificationResponse> listUnreadNotifications(SubjectRef recipient, Integer limit) {
        SubjectRef ref = requireRecipient(recipient);
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, 100);
        List<Notification> list = notificationMapper.findUnreadByRecipient(ref.id(), ref.type().name());
        if (list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .limit(size)
                .map(this::toResponse)
                .toList();
    }

    @Override
    public long countUnread(SubjectRef recipient) {
        SubjectRef ref = requireRecipient(recipient);
        return notificationMapper.countUnreadByRecipient(ref.id(), ref.type().name());
    }

    @Override
    public void markRead(SubjectRef recipient, Long notificationId) {
        SubjectRef ref = requireRecipient(recipient);
        if (notificationId == null) {
            throw BizException.of("notification.id.required");
        }
        Notification n = notificationMapper.findByIdForRecipient(notificationId, ref.id(), ref.type().name());
        if (n == null) {
            throw BizException.of("notification.not_found");
        }
        if (Boolean.TRUE.equals(n.getIsRead())) {
            return;
        }
        notificationMapper.markReadForRecipient(notificationId, ref.id(), ref.type().name());
    }

    @Override
    public void markAllRead(SubjectRef recipient) {
        SubjectRef ref = requireRecipient(recipient);
        notificationMapper.markAllAsRead(ref.id(), ref.type().name());
    }

    @Override
    @Transactional
    public void sendNotification(
            SubjectRef recipient,
            String type,
            String title,
            String content,
            String data,
            SubjectRef sender
    ) {
        SubjectRef recipientRef = requireRecipient(recipient);
        SubjectRef senderRef = requireSender(sender);
        if (!StringUtils.hasText(title)) {
            log.warn("send notification skipped: title is blank");
            return;
        }

        Notification.NotificationType notificationType;
        try {
            notificationType = StringUtils.hasText(type)
                    ? Notification.NotificationType.valueOf(type)
                    : Notification.NotificationType.SYSTEM;
        } catch (IllegalArgumentException e) {
            log.warn("unknown notification type: {}, falling back to SYSTEM", type);
            notificationType = Notification.NotificationType.SYSTEM;
        }

        String shortContent = content;
        if (shortContent != null && shortContent.length() > 500) {
            shortContent = shortContent.substring(0, 497) + "...";
        }

        Notification notification = Notification.builder()
                .recipientId(recipientRef.id())
                .recipientType(toRecipientType(recipientRef.type()))
                .notificationType(notificationType)
                .title(title)
                .content(shortContent)
                .data(data)
                .senderId(senderRef.id())
                .senderType(toSenderType(senderRef.type()))
                .isRead(false)
                .priority(Notification.Priority.NORMAL)
                .build();

        try {
            notificationMapper.insert(notification);
            log.debug("send notification succeeded: recipient={}, type={}, title={}", recipientRef, type, title);
        } catch (Exception e) {
            log.error("send notification failed: recipient={}, type={}", recipientRef, type, e);
            return;
        }

        try {
            pushRealtime(notification);
        } catch (Exception e) {
            log.warn("push realtime notification failed: recipient={}, type={}", recipientRef, type, e);
        }
    }

    private void pushRealtime(Notification notification) {
        if (notification.getRecipientId() == null || notification.getRecipientType() == null) {
            return;
        }
        String recipientId = String.valueOf(notification.getRecipientId());
        String recipientType = notification.getRecipientType().name();
        WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                .id(notification.getId() != null ? String.valueOf(notification.getId()) : UUID.randomUUID().toString())
                .type(WebSocketMessage.MessageType.NOTIFICATION)
                .senderSubjectId(notification.getSenderId() != null ? String.valueOf(notification.getSenderId()) : "0")
                .senderSubjectType(notification.getSenderType() != null
                        ? notification.getSenderType().name()
                        : SubjectType.SYSTEM.name())
                .recipientSubjectId(recipientId)
                .recipientSubjectType(recipientType)
                .timestamp(notification.getCreateTime() != null ? notification.getCreateTime() : LocalDateTime.now())
                .payload(toResponse(notification))
                .build();
        webSocketSender.sendToSubject(recipientId, recipientType, message);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .recipientSubjectId(n.getRecipientId())
                .recipientSubjectType(n.getRecipientType() != null ? n.getRecipientType().name() : null)
                .senderSubjectId(n.getSenderId())
                .senderSubjectType(n.getSenderType() != null ? n.getSenderType().name() : null)
                .type(n.getNotificationType() != null ? n.getNotificationType().name() : null)
                .title(n.getTitle())
                .content(n.getContent())
                .read(Boolean.TRUE.equals(n.getIsRead()))
                .createTime(n.getCreateTime())
                .build();
    }

    private static SubjectRef requireRecipient(SubjectRef recipient) {
        if (recipient == null || recipient.id() == null || recipient.type() == null) {
            throw BizException.of("notification.recipient.required");
        }
        if (recipient.type() == SubjectType.SYSTEM) {
            throw BizException.of("notification.recipient.type.invalid");
        }
        return recipient;
    }

    private static SubjectRef requireSender(SubjectRef sender) {
        if (sender == null || sender.type() == null || sender.id() == null) {
            throw BizException.of("notification.sender.required");
        }
        return sender;
    }

    private static Notification.RecipientType toRecipientType(SubjectType type) {
        if (type == SubjectType.HUMAN) {
            return Notification.RecipientType.HUMAN;
        }
        if (type == SubjectType.AGENT) {
            return Notification.RecipientType.AGENT;
        }
        throw BizException.of("notification.recipient.type.invalid");
    }

    private static Notification.SenderType toSenderType(SubjectType type) {
        if (type == SubjectType.HUMAN) {
            return Notification.SenderType.HUMAN;
        }
        if (type == SubjectType.AGENT) {
            return Notification.SenderType.AGENT;
        }
        if (type == SubjectType.SYSTEM) {
            return Notification.SenderType.SYSTEM;
        }
        throw BizException.of("notification.sender.type.invalid");
    }
}
