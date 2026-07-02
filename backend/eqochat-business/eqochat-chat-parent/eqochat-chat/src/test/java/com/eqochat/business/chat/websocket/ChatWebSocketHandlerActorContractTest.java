package com.eqochat.business.chat.websocket;

import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.chat.api.service.ConversationParticipantService;
import com.eqochat.business.chat.entity.ConversationParticipant;
import com.eqochat.framework.common.UserContext;
import com.eqochat.framework.websocket.WebSocketMessage;
import com.eqochat.framework.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerActorContractTest {

    @Mock
    private WebSocketMessageHandler messageHandler;
    @Mock
    private ConversationParticipantService participantService;
    @Mock
    private SubjectDirectoryApi subjectDirectoryApi;
    @Mock
    private WebSocketSession session;
    @Mock
    private WebSocketSession newSession;
    @Mock
    private WebSocketSession observerSession;
    @Mock
    private WebSocketSession unrelatedSession;

    private WebSocketSessionManager sessionManager;
    private ChatWebSocketHandler handler;
    private ObjectMapper objectMapper;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        sessionManager = new WebSocketSessionManager();
        handler = new ChatWebSocketHandler(
                objectMapper,
                sessionManager,
                messageHandler,
                participantService,
                subjectDirectoryApi
        );
        attributes = new HashMap<>();
        attributes.put("principalHumanId", "2");
        lenient().when(session.getId()).thenReturn("session-1");
        lenient().when(session.getAttributes()).thenReturn(attributes);
        lenient().when(newSession.getId()).thenReturn("session-2");
        lenient().when(newSession.getAttributes()).thenReturn(attributes);
        lenient().when(newSession.isOpen()).thenReturn(true);
        lenient().when(observerSession.getId()).thenReturn("observer-session");
        lenient().when(observerSession.isOpen()).thenReturn(true);
        lenient().when(unrelatedSession.getId()).thenReturn("unrelated-session");
        lenient().when(unrelatedSession.isOpen()).thenReturn(true);
    }

    @Test
    void connectionRegistersAllAssociatedSubjectsWithoutChangingActiveSubject() throws Exception {
        when(subjectDirectoryApi.listAssociatedSubjects(2L)).thenReturn(List.of(
                SubjectRef.human(2L),
                SubjectRef.agent(101L)
        ));
        when(participantService.listByParticipant(SubjectRef.human(2L))).thenReturn(List.of(
                ConversationParticipant.builder().conversationId(10002L).build()
        ));
        when(participantService.listByParticipant(SubjectRef.agent(101L))).thenReturn(List.of(
                ConversationParticipant.builder().conversationId(10003L).build()
        ));

        handler.afterConnectionEstablished(session);

        assertThat(sessionManager.getActiveSubjectKeyBySessionId("session-1")).isNull();
        assertThat(sessionManager.getSubjectKeysBySessionId("session-1"))
                .containsExactlyInAnyOrder("HUMAN:2", "AGENT:101");
        assertThat(sessionManager.getSubjectSessions("2", "HUMAN")).contains(session);
        assertThat(sessionManager.getSubjectSessions("101", "AGENT")).contains(session);
        assertThat(sessionManager.getConversationSubjectKeys("10002")).contains("HUMAN:2");
        assertThat(sessionManager.getConversationSubjectKeys("10003")).contains("AGENT:101");
        assertThat(sessionManager.getConversationPrincipalHumans("10002")).contains("2");
        assertThat(sessionManager.getConversationPrincipalHumans("10003")).doesNotContain("2");
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void connectionDoesNotFallbackToHumanSubjectWhenDirectoryHasNoAssociatedSubjects() throws Exception {
        when(subjectDirectoryApi.listAssociatedSubjects(2L)).thenReturn(List.of());

        handler.afterConnectionEstablished(session);

        assertThat(sessionManager.getActiveSubjectKeyBySessionId("session-1")).isNull();
        assertThat(sessionManager.getSubjectKeysBySessionId("session-1")).isEmpty();
    }

    @Test
    void closeOfReplacedSessionDoesNotUnregisterCurrentSession() throws Exception {
        when(subjectDirectoryApi.listAssociatedSubjects(2L)).thenReturn(List.of());

        handler.afterConnectionEstablished(session);
        handler.afterConnectionEstablished(newSession);
        handler.afterConnectionClosed(session, org.springframework.web.socket.CloseStatus.NORMAL);

        assertThat(handler.isPrincipalHumanOnline("2")).isTrue();
        assertThat(sessionManager.getPrincipalHumanSession("2")).isSameAs(newSession);
        assertThat(sessionManager.getSubjectKeysBySessionId("session-1")).isEmpty();
        assertThat(sessionManager.getSubjectKeysBySessionId("session-2")).isEmpty();
    }

    @Test
    void connectionBroadcastsPresenceForAssociatedAgentSubject() throws Exception {
        Map<String, Object> observerAttributes = new HashMap<>();
        observerAttributes.put("principalHumanId", "3");
        when(observerSession.getAttributes()).thenReturn(observerAttributes);
        when(subjectDirectoryApi.listAssociatedSubjects(3L)).thenReturn(List.of(SubjectRef.human(3L)));
        when(participantService.listByParticipant(SubjectRef.human(3L))).thenReturn(List.of(
                ConversationParticipant.builder().conversationId(10003L).build()
        ));
        handler.afterConnectionEstablished(observerSession);

        Map<String, Object> unrelatedAttributes = new HashMap<>();
        unrelatedAttributes.put("principalHumanId", "4");
        when(unrelatedSession.getAttributes()).thenReturn(unrelatedAttributes);
        when(subjectDirectoryApi.listAssociatedSubjects(4L)).thenReturn(List.of(SubjectRef.human(4L)));
        when(participantService.listByParticipant(SubjectRef.human(4L))).thenReturn(List.of());
        handler.afterConnectionEstablished(unrelatedSession);

        when(subjectDirectoryApi.listAssociatedSubjects(2L)).thenReturn(List.of(
                SubjectRef.human(2L),
                SubjectRef.agent(101L)
        ));
        when(participantService.listByParticipant(SubjectRef.human(2L))).thenReturn(List.of());
        when(participantService.listByParticipant(SubjectRef.agent(101L))).thenReturn(List.of(
                ConversationParticipant.builder().conversationId(10003L).build()
        ));
        handler.afterConnectionEstablished(session);

        ArgumentCaptor<TextMessage> observerMessages = ArgumentCaptor.forClass(TextMessage.class);
        verify(observerSession, atLeastOnce()).sendMessage(observerMessages.capture());
        ArgumentCaptor<TextMessage> unrelatedMessages = ArgumentCaptor.forClass(TextMessage.class);
        verify(unrelatedSession, atLeastOnce()).sendMessage(unrelatedMessages.capture());

        assertThat(hasPresence(observerMessages.getAllValues(), "PRESENCE_ONLINE", "101", "AGENT")).isTrue();
        assertThat(hasPresence(unrelatedMessages.getAllValues(), "PRESENCE_ONLINE", "101", "AGENT")).isFalse();
    }

    @Test
    void closeBroadcastsOfflinePresenceForAssociatedAgentSubject() throws Exception {
        Map<String, Object> observerAttributes = new HashMap<>();
        observerAttributes.put("principalHumanId", "3");
        when(observerSession.getAttributes()).thenReturn(observerAttributes);
        when(subjectDirectoryApi.listAssociatedSubjects(3L)).thenReturn(List.of(SubjectRef.human(3L)));
        when(participantService.listByParticipant(SubjectRef.human(3L))).thenReturn(List.of(
                ConversationParticipant.builder().conversationId(10003L).build()
        ));
        handler.afterConnectionEstablished(observerSession);

        when(subjectDirectoryApi.listAssociatedSubjects(2L)).thenReturn(List.of(
                SubjectRef.human(2L),
                SubjectRef.agent(101L)
        ));
        when(participantService.listByParticipant(SubjectRef.human(2L))).thenReturn(List.of());
        when(participantService.listByParticipant(SubjectRef.agent(101L))).thenReturn(List.of(
                ConversationParticipant.builder().conversationId(10003L).build()
        ));
        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, org.springframework.web.socket.CloseStatus.NORMAL);

        ArgumentCaptor<TextMessage> messages = ArgumentCaptor.forClass(TextMessage.class);
        verify(observerSession, atLeastOnce()).sendMessage(messages.capture());

        assertThat(hasPresence(messages.getAllValues(), "PRESENCE_OFFLINE", "101", "AGENT")).isTrue();
    }

    @Test
    void handleTextMessageClearsUserContextAfterEachMessage() throws Exception {
        UserContext.setCurrentUser(99L);
        WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                .id("ping")
                .type(WebSocketMessage.MessageType.PING)
                .build();

        handler.handleTextMessage(session, new TextMessage(objectMapper.writeValueAsString(message)));

        assertThat(UserContext.getCurrentUser()).isNull();
    }

    private boolean hasPresence(List<TextMessage> messages, String type, String subjectId, String subjectType) throws Exception {
        for (TextMessage message : messages) {
            if (matchesPresence(message, type, subjectId, subjectType)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPresence(TextMessage message, String type, String subjectId, String subjectType) throws Exception {
        JsonNode json = objectMapper.readTree(message.getPayload());
        JsonNode payload = json.path("payload");
        return type.equals(json.path("type").asText())
                && subjectId.equals(json.path("senderSubjectId").asText())
                && subjectType.equals(json.path("senderSubjectType").asText())
                && subjectId.equals(payload.path("subjectId").asText())
                && subjectType.equals(payload.path("subjectType").asText());
    }
}
