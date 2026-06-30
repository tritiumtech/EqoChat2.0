package com.eqochat.business.chat.websocket;

import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.chat.api.service.ConversationParticipantService;
import com.eqochat.business.chat.api.service.ConversationService;
import com.eqochat.business.chat.api.service.MessageService;
import com.eqochat.framework.websocket.WebSocketMessage;
import com.eqochat.framework.websocket.WebSocketSender;
import com.eqochat.framework.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WebSocketMessageHandlerActorContractTest {

    @Mock
    private MessageService messageService;
    @Mock
    private ConversationService conversationService;
    @Mock
    private ConversationParticipantService participantService;
    private WebSocketSessionManager sessionManager;
    private WebSocketSession session;

    private ObjectMapper objectMapper;
    private CapturingWebSocketSender webSocketSender;
    private WebSocketMessageHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sessionManager = new WebSocketSessionManager();
        webSocketSender = new CapturingWebSocketSender(objectMapper, sessionManager);
        handler = new WebSocketMessageHandler(
                objectMapper,
                messageService,
                conversationService,
                participantService,
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
        assertThat(json).doesNotContain("senderId");
        assertThat(json).doesNotContain("senderType\":\"USER");
        assertThat(json).doesNotContain("userId");
        assertThat(json).doesNotContain("recipientId");
    }

    private static class CapturingWebSocketSender extends WebSocketSender {
        private Integer lastCode;
        private String lastMessage;
        private String lastOriginalMessageId;

        CapturingWebSocketSender(ObjectMapper objectMapper, WebSocketSessionManager sessionManager) {
            super(objectMapper, sessionManager);
        }

        @Override
        public void sendError(WebSocketSession session, Integer code, String message, String originalMessageId) {
            this.lastCode = code;
            this.lastMessage = message;
            this.lastOriginalMessageId = originalMessageId;
        }
    }
}
