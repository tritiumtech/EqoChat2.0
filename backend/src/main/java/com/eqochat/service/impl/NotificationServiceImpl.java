package com.eqochat.service.impl;

import com.eqochat.common.BizException;
import com.eqochat.domain.entity.Notification;
import com.eqochat.dto.response.NotificationResponse;
import com.eqochat.mapper.NotificationMapper;
import com.eqochat.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
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
}

