package com.eqochat.business.notification.service.impl;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.notification.entity.Notification;
import com.eqochat.business.notification.mapper.NotificationMapper;
import com.eqochat.framework.websocket.WebSocketMessage;
import com.eqochat.framework.websocket.WebSocketSender;
import com.eqochat.framework.common.BizException;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplActorContractTest {

    @Mock
    NotificationMapper notificationMapper;
    @Mock
    WebSocketSender webSocketSender;

    @Test
    void mapperQueriesScopeRecipientBySubjectType() throws Exception {
        assertThat(selectSql("findByRecipient"))
                .contains("recipient_id = #{recipientId}", "recipient_type = #{recipientType}");
        assertThat(selectSql("findUnreadByRecipient"))
                .contains("recipient_id = #{recipientId}", "recipient_type = #{recipientType}", "is_read = false");
        assertThat(selectSql("countUnreadByRecipient"))
                .contains("recipient_id = #{recipientId}", "recipient_type = #{recipientType}", "is_read = false");
        assertThat(selectSql("findByIdForRecipient"))
                .contains("id = #{notificationId}", "recipient_id = #{recipientId}", "recipient_type = #{recipientType}");
        assertThat(updateSql("markReadForRecipient"))
                .contains("id = #{notificationId}", "recipient_id = #{recipientId}", "recipient_type = #{recipientType}");
        assertThat(updateSql("markAllAsRead"))
                .contains("recipient_id = #{recipientId}", "recipient_type = #{recipientType}");
    }

    @Test
    void sendNotificationPersistsExplicitRecipientAndSenderSubjects() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, webSocketSender);

        service.sendNotification(
                SubjectRef.agent(101L),
                "MESSAGE_MENTION",
                "Mention",
                "hello",
                "{\"postId\":1}",
                SubjectRef.human(2L)
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        Notification notification = captor.getValue();
        assertThat(notification.getRecipientId()).isEqualTo(101L);
        assertThat(notification.getRecipientType()).isEqualTo(Notification.RecipientType.AGENT);
        assertThat(notification.getSenderId()).isEqualTo(2L);
        assertThat(notification.getSenderType()).isEqualTo(Notification.SenderType.HUMAN);
    }

    @Test
    void sendNotificationPushesRealtimeToRecipientSubject() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, webSocketSender);

        service.sendNotification(
                SubjectRef.agent(101L),
                "MESSAGE_MENTION",
                "Mention",
                "hello",
                "{\"postId\":1}",
                SubjectRef.human(2L)
        );

        ArgumentCaptor<WebSocketMessage.BaseMessage> messageCaptor =
                ArgumentCaptor.forClass(WebSocketMessage.BaseMessage.class);
        verify(webSocketSender).sendToSubject(org.mockito.ArgumentMatchers.eq("101"), org.mockito.ArgumentMatchers.eq("AGENT"), messageCaptor.capture());
        WebSocketMessage.BaseMessage message = messageCaptor.getValue();
        assertThat(message.getType()).isEqualTo(WebSocketMessage.MessageType.NOTIFICATION);
        assertThat(message.getSenderSubjectId()).isEqualTo("2");
        assertThat(message.getSenderSubjectType()).isEqualTo("HUMAN");
        assertThat(message.getRecipientSubjectId()).isEqualTo("101");
        assertThat(message.getRecipientSubjectType()).isEqualTo("AGENT");
        NotificationResponsePayload payload = NotificationResponsePayload.from(message.getPayload());
        assertThat(payload.recipientSubjectId()).isEqualTo(101L);
        assertThat(payload.recipientSubjectType()).isEqualTo("AGENT");
    }

    @Test
    void sendNotificationRequiresExplicitSender() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, webSocketSender);

        assertThatThrownBy(() -> service.sendNotification(
                SubjectRef.human(2L),
                "SYSTEM",
                "Title",
                "body",
                "{}",
                null
        ))
                .isInstanceOf(BizException.class)
                .hasMessage("notification.sender.required");
        verifyNoInteractions(webSocketSender);
    }

    @Test
    void markReadUsesRecipientSubjectScope() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, webSocketSender);
        Notification notification = Notification.builder()
                .id(10L)
                .recipientId(2L)
                .recipientType(Notification.RecipientType.HUMAN)
                .isRead(false)
                .createTime(LocalDateTime.now())
                .delToken(0L)
                .build();
        when(notificationMapper.findByIdForRecipient(10L, 2L, "HUMAN")).thenReturn(notification);

        service.markRead(SubjectRef.human(2L), 10L);

        verify(notificationMapper).markReadForRecipient(10L, 2L, "HUMAN");
    }

    @Test
    void listNotificationsReturnsCanonicalSubjectFields() {
        NotificationServiceImpl service = new NotificationServiceImpl(notificationMapper, webSocketSender);
        when(notificationMapper.findByRecipient(2L, "HUMAN")).thenReturn(List.of(Notification.builder()
                .id(10L)
                .recipientId(2L)
                .recipientType(Notification.RecipientType.HUMAN)
                .senderId(0L)
                .senderType(Notification.SenderType.SYSTEM)
                .notificationType(Notification.NotificationType.SYSTEM)
                .title("System")
                .isRead(false)
                .createTime(LocalDateTime.now())
                .build()));

        var item = service.listNotifications(SubjectRef.human(2L), 20).get(0);

        assertThat(item.getRecipientSubjectId()).isEqualTo(2L);
        assertThat(item.getRecipientSubjectType()).isEqualTo("HUMAN");
        assertThat(item.getSenderSubjectId()).isEqualTo(0L);
        assertThat(item.getSenderSubjectType()).isEqualTo("SYSTEM");
    }

    private static String selectSql(String methodName) throws Exception {
        for (Method method : NotificationMapper.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                Select select = method.getAnnotation(Select.class);
                return String.join("\n", select.value());
            }
        }
        throw new IllegalArgumentException("method not found: " + methodName);
    }

    private static String updateSql(String methodName) throws Exception {
        for (Method method : NotificationMapper.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                Update update = method.getAnnotation(Update.class);
                return String.join("\n", update.value());
            }
        }
        throw new IllegalArgumentException("method not found: " + methodName);
    }

    private record NotificationResponsePayload(
            Long recipientSubjectId,
            String recipientSubjectType
    ) {
        static NotificationResponsePayload from(Object raw) {
            com.eqochat.business.notification.api.dto.response.NotificationResponse response =
                    (com.eqochat.business.notification.api.dto.response.NotificationResponse) raw;
            return new NotificationResponsePayload(response.getRecipientSubjectId(), response.getRecipientSubjectType());
        }
    }
}
