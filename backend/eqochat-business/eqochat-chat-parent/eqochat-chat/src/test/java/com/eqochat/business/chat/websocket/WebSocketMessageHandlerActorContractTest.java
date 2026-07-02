package com.eqochat.business.chat.websocket;

import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.chat.api.service.ConversationParticipantService;
import com.eqochat.business.chat.api.service.ConversationService;
import com.eqochat.business.chat.api.service.MessageService;
import com.eqochat.business.chat.entity.Message;
import com.eqochat.framework.websocket.WebSocketMessage;
import com.eqochat.framework.websocket.WebSocketSender;
import com.eqochat.framework.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketMessageHandlerActorContractTest {

    @Mock
    private MessageService messageService;
    @Mock
    private ConversationService conversationService;
    @Mock
    private ConversationParticipantService participantService;
    @Mock
    private LiabilityPolicyApi liabilityPolicyApi;
    private WebSocketSessionManager sessionManager;
    @Mock
    private WebSocketSession session;

    private ObjectMapper objectMapper;
    private CapturingWebSocketSender webSocketSender;
    private WebSocketMessageHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        sessionManager = new WebSocketSessionManager();
        webSocketSender = new CapturingWebSocketSender(objectMapper, sessionManager);
        lenient().when(session.getId()).thenReturn("session-1");
        handler = new WebSocketMessageHandler(
                objectMapper,
                messageService,
                conversationService,
                participantService,
                liabilityPolicyApi,
                sessionManager,
                webSocketSender
        );
    }

    @Test
    void chatMessageWithoutSenderSubjectReturnsBadRequest() throws Exception {
        WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                .id("missing-subject")
                .type(WebSocketMessage.MessageType.CHAT_MESSAGE)
                .payload(Map.of(
                        "conversationId", "10002",
                        "content", "hello"
                ))
                .build();

        handler.handleMessage("2", message, session);

        assertThat(webSocketSender.lastCode).isEqualTo(400);
        assertThat(webSocketSender.lastMessage).contains("sender subject is incomplete");
        assertThat(webSocketSender.lastOriginalMessageId).isEqualTo("missing-subject");
    }

    @Test
    void chatMessageWithUserSubjectTypeReturnsBadRequest() throws Exception {
        WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                .id("legacy-user")
                .type(WebSocketMessage.MessageType.CHAT_MESSAGE)
                .senderSubjectId("2")
                .senderSubjectType("USER")
                .payload(Map.of(
                        "conversationId", "10002",
                        "content", "hello"
                ))
                .build();

        handler.handleMessage("2", message, session);

        assertThat(webSocketSender.lastCode).isEqualTo(400);
        assertThat(webSocketSender.lastMessage).contains("USER");
        assertThat(webSocketSender.lastOriginalMessageId).isEqualTo("legacy-user");
    }

    @Test
    void baseMessageJsonUsesCanonicalSubjectFieldsOnly() throws Exception {
        WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                .id("canonical")
                .type(WebSocketMessage.MessageType.CHAT_MESSAGE)
                .senderSubjectId("2")
                .senderSubjectType(SubjectType.HUMAN.name())
                .payload(Map.of("conversationId", "10002"))
                .build();

        String json = objectMapper.writeValueAsString(message);

        assertThat(json).contains("senderSubjectId");
        assertThat(json).contains("senderSubjectType");
        assertThat(json).contains("recipientSubjectId");
        assertThat(json).contains("recipientSubjectType");
        assertThat(json).doesNotContain("senderId");
        assertThat(json).doesNotContain("senderType\":\"USER");
        assertThat(json).doesNotContain("userId");
        assertThat(json).doesNotContain("recipientId");
    }

    @Test
    void ownedAgentSubjectSubscriptionRegistersActiveSubject() throws Exception {
        sessionManager.registerPrincipalHumanSession("2", session);
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L)))
                .thenReturn(LiabilityChain.agentToHuman(101L, 2L));
        WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                .id("subscribe-agent")
                .type(WebSocketMessage.MessageType.SUBJECT_SUBSCRIBE)
                .payload(Map.of(
                        "subjectId", "101",
                        "subjectType", "AGENT"
                ))
                .build();

        handler.handleMessage("2", message, session);

        assertThat(sessionManager.getActiveSubjectKeyBySessionId(session.getId())).isEqualTo("AGENT:101");
        assertThat(sessionManager.getSubjectKeysBySessionId(session.getId()))
                .containsExactly("AGENT:101")
                .doesNotContain("HUMAN:2");
        assertThat(sessionManager.getSubjectSessions("101", "AGENT")).contains(session);
    }

    @Test
    void unauthorizedAgentSubjectSubscriptionReturnsBadRequest() throws Exception {
        when(liabilityPolicyApi.resolveLiability(SubjectRef.agent(101L)))
                .thenReturn(LiabilityChain.agentToHuman(101L, 9L));
        WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                .id("subscribe-agent")
                .type(WebSocketMessage.MessageType.SUBJECT_SUBSCRIBE)
                .payload(Map.of(
                        "subjectId", "101",
                        "subjectType", "AGENT"
                ))
                .build();

        handler.handleMessage("2", message, session);

        assertThat(webSocketSender.lastCode).isEqualTo(400);
        assertThat(webSocketSender.lastMessage).contains("not authorized");
        assertThat(sessionManager.getActiveSubjectKeyBySessionId(session.getId())).isNull();
    }

    @Test
    void readReceiptForAgentSenderTargetsAgentSubject() throws Exception {
        Message stored = Message.builder()
                .id(77L)
                .conversationId(10002L)
                .senderId(101L)
                .senderType(SubjectType.AGENT)
                .build();
        when(messageService.getById(77L)).thenReturn(stored);

        WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                .id("read-agent")
                .type(WebSocketMessage.MessageType.CHAT_READ)
                .senderSubjectId("2")
                .senderSubjectType("HUMAN")
                .payload(Map.of(
                        "conversationId", "10002",
                        "messageId", "77"
                ))
                .build();

        handler.handleMessage("2", message, session);

        assertThat(webSocketSender.lastSubjectId).isEqualTo("101");
        assertThat(webSocketSender.lastSubjectType).isEqualTo("AGENT");
        assertThat(webSocketSender.lastOutbound.getRecipientSubjectId()).isEqualTo("101");
        assertThat(webSocketSender.lastOutbound.getRecipientSubjectType()).isEqualTo("AGENT");
    }

    @Test
    void conversationBroadcastDeduplicatesOneSessionSubscribedToMultipleConversationSubjects() throws Exception {
        when(session.isOpen()).thenReturn(true);
        sessionManager.registerSubjectSession("2", "2", "HUMAN", session);
        sessionManager.registerSubjectSession("2", "101", "AGENT", session);
        sessionManager.joinConversationAsSubject("10002", "2", "HUMAN");
        sessionManager.joinConversationAsSubject("10002", "101", "AGENT");
        WebSocketSender sender = new WebSocketSender(objectMapper, sessionManager);
        WebSocketMessage.BaseMessage message = WebSocketMessage.BaseMessage.builder()
                .id("fanout")
                .type(WebSocketMessage.MessageType.CHAT_MESSAGE)
                .senderSubjectId("101")
                .senderSubjectType("AGENT")
                .timestamp(LocalDateTime.now())
                .payload(Map.of("content", "hello"))
                .build();

        sender.broadcastToConversationSubjects("10002", message);

        verify(session, times(1)).sendMessage(any(TextMessage.class));
    }

    private static class CapturingWebSocketSender extends WebSocketSender {
        private Integer lastCode;
        private String lastMessage;
        private String lastOriginalMessageId;
        private String lastSubjectId;
        private String lastSubjectType;
        private WebSocketMessage.BaseMessage lastOutbound;

        CapturingWebSocketSender(ObjectMapper objectMapper, WebSocketSessionManager sessionManager) {
            super(objectMapper, sessionManager);
        }

        @Override
        public void sendError(WebSocketSession session, Integer code, String message, String originalMessageId) {
            this.lastCode = code;
            this.lastMessage = message;
            this.lastOriginalMessageId = originalMessageId;
        }

        @Override
        public void sendToSubject(String subjectId, String subjectType, WebSocketMessage.BaseMessage message) {
            this.lastSubjectId = subjectId;
            this.lastSubjectType = subjectType;
            this.lastOutbound = message;
        }
    }
}
