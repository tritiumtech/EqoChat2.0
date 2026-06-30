package com.eqochat.business.chat.service.impl;

import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.chat.api.dto.request.MarkConversationReadRequest;
import com.eqochat.business.chat.api.dto.request.SendMessageRequest;
import com.eqochat.business.chat.api.dto.response.MessageResponse;
import com.eqochat.business.chat.api.service.ConversationParticipantService;
import com.eqochat.business.chat.api.service.MessageService;
import com.eqochat.business.chat.entity.ConversationParticipant;
import com.eqochat.business.chat.entity.Message;
import com.eqochat.business.chat.entity.MessageReadReceipt;
import com.eqochat.business.chat.mapper.MessageMapper;
import com.eqochat.business.chat.mapper.MessageReadReceiptMapper;
import com.eqochat.business.chat.websocket.ChatMessageRealtimeNotifier;
import com.eqochat.framework.common.BizException;
import com.eqochat.framework.websocket.WebSocketSender;
import com.eqochat.framework.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplActorContractTest {

    @Mock
    private ConversationParticipantService participantService;
    @Mock
    private MessageService messageService;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private MessageReadReceiptMapper messageReadReceiptMapper;
    @Mock
    private SubjectDirectoryApi subjectDirectoryApi;
    @Mock
    private LiabilityPolicyApi liabilityPolicyApi;
    private WebSocketSessionManager webSocketSessionManager;
    private ChatMessageRealtimeNotifier chatMessageRealtimeNotifier;

    private ConversationServiceImpl service;

    @BeforeEach
    void setUp() {
        webSocketSessionManager = new WebSocketSessionManager();
        chatMessageRealtimeNotifier = new NoopChatMessageRealtimeNotifier();
        service = new TestConversationService(
                participantService,
                messageService,
                messageMapper,
                messageReadReceiptMapper,
                subjectDirectoryApi,
                liabilityPolicyApi,
                new ObjectMapper(),
                webSocketSessionManager,
                chatMessageRealtimeNotifier
        );
    }

    @Test
    void sendMessageRequiresExplicitActorSubject() {
        SendMessageRequest request = new SendMessageRequest();
        request.setMessageType("TEXT");
        request.setContent("hello");

        assertThatThrownBy(() -> service.sendMessage(2L, 10002L, request))
                .isInstanceOf(BizException.class);

        verifyNoInteractions(liabilityPolicyApi, participantService, messageService);
    }

    @Test
    void humanSendPersistsCanonicalSubjectAndLiability() {
        SubjectRef human = SubjectRef.human(2L);
        when(liabilityPolicyApi.resolveLiability(human)).thenReturn(LiabilityChain.selfResponsible(2L));
        when(participantService.findByConversationAndParticipant(10002L, human))
                .thenReturn(Optional.of(participant(10002L, human)));
        stubMessageSave(501L);

        SendMessageRequest request = new SendMessageRequest();
        request.setActorSubjectId(2L);
        request.setActorSubjectType(SubjectType.HUMAN);
        request.setMessageType("TEXT");
        request.setContent("human says hi");

        MessageResponse response = service.sendMessage(2L, 10002L, request);

        ArgumentCaptor<Message> saved = ArgumentCaptor.forClass(Message.class);
        verify(messageService).save(saved.capture());
        assertThat(saved.getValue().getSenderId()).isEqualTo(2L);
        assertThat(saved.getValue().getSenderType()).isEqualTo(SubjectType.HUMAN);
        assertThat(saved.getValue().getLiableHumanId()).isEqualTo(2L);
        assertThat(response.getSenderSubjectId()).isEqualTo(2L);
        assertThat(response.getSenderSubjectType()).isEqualTo(SubjectType.HUMAN);
        assertThat(response.getLiableHumanId()).isEqualTo(2L);
        verify(participantService).updateLastRead(anyLong(), any(SubjectRef.class), anyLong(), any(LocalDateTime.class));
    }

    @Test
    void agentSendPersistsAgentSubjectAndLiableHuman() {
        SubjectRef agent = SubjectRef.agent(11L);
        when(liabilityPolicyApi.resolveLiability(agent)).thenReturn(LiabilityChain.agentToHuman(11L, 2L));
        when(participantService.findByConversationAndParticipant(10002L, agent))
                .thenReturn(Optional.of(participant(10002L, agent)));
        stubMessageSave(502L);

        SendMessageRequest request = new SendMessageRequest();
        request.setActorSubjectId(11L);
        request.setActorSubjectType(SubjectType.AGENT);
        request.setMessageType("TEXT");
        request.setContent("agent says hi");

        MessageResponse response = service.sendMessage(2L, 10002L, request);

        ArgumentCaptor<Message> saved = ArgumentCaptor.forClass(Message.class);
        verify(messageService).save(saved.capture());
        assertThat(saved.getValue().getSenderId()).isEqualTo(11L);
        assertThat(saved.getValue().getSenderType()).isEqualTo(SubjectType.AGENT);
        assertThat(saved.getValue().getLiableHumanId()).isEqualTo(2L);
        assertThat(response.getSenderSubjectId()).isEqualTo(11L);
        assertThat(response.getSenderSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getLiableHumanId()).isEqualTo(2L);
    }

    @Test
    void readReceiptUsesSubjectTypeWhenHumanAndAgentShareNumericId() {
        SubjectRef reader = SubjectRef.agent(7L);
        Message messageFromHumanSeven = Message.builder()
                .id(900L)
                .conversationId(10002L)
                .senderId(7L)
                .senderType(SubjectType.HUMAN)
                .messageType("TEXT")
                .content("same numeric id")
                .build();
        when(liabilityPolicyApi.resolveLiability(reader)).thenReturn(LiabilityChain.agentToHuman(7L, 2L));
        when(participantService.findByConversationAndParticipant(10002L, reader))
                .thenReturn(Optional.of(participant(10002L, reader)));
        when(messageService.getById(900L)).thenReturn(messageFromHumanSeven);

        MarkConversationReadRequest request = new MarkConversationReadRequest();
        request.setMessageId(900L);
        request.setReaderSubjectId(7L);
        request.setReaderSubjectType(SubjectType.AGENT);

        service.markRead(2L, 10002L, request);

        verify(messageService).markAsRead(900L, reader);
        verify(participantService).updateLastRead(anyLong(), any(SubjectRef.class), anyLong(), any(LocalDateTime.class));
        ArgumentCaptor<MessageReadReceipt> receipt = ArgumentCaptor.forClass(MessageReadReceipt.class);
        verify(messageReadReceiptMapper).insert(receipt.capture());
        assertThat(receipt.getValue().getReaderId()).isEqualTo(7L);
        assertThat(receipt.getValue().getReaderType()).isEqualTo(SubjectType.AGENT);
    }

    @Test
    void unreadMapperContractIncludesReaderSubjectType() throws NoSuchMethodException {
        Select select = MessageMapper.class
                .getMethod("countUnreadMessages", Long.class, Long.class, SubjectType.class, Long.class)
                .getAnnotation(Select.class);
        String sql = String.join("\n", Arrays.asList(select.value()));

        assertThat(sql).contains("sender_id = #{readerId}");
        assertThat(sql).contains("sender_type = #{readerType}");
    }

    private ConversationParticipant participant(Long conversationId, SubjectRef subject) {
        return ConversationParticipant.builder()
                .conversationId(conversationId)
                .participantId(subject.id())
                .participantType(subject.type())
                .build();
    }

    private void stubMessageSave(Long id) {
        doAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(id);
            message.setCreateTime(LocalDateTime.now());
            return true;
        }).when(messageService).save(any(Message.class));
    }

    private static class NoopChatMessageRealtimeNotifier extends ChatMessageRealtimeNotifier {

        NoopChatMessageRealtimeNotifier() {
            super(
                    new WebSocketSender(new ObjectMapper(), new WebSocketSessionManager()),
                    new WebSocketSessionManager(),
                    null,
                    new ObjectMapper()
            );
        }

        @Override
        public void notifyChatMessageSaved(Message msg) {
            // Unit tests in this class assert persistence payloads, not realtime fanout.
        }
    }

    private static class TestConversationService extends ConversationServiceImpl {

        TestConversationService(
                ConversationParticipantService participantService,
                MessageService messageService,
                MessageMapper messageMapper,
                MessageReadReceiptMapper messageReadReceiptMapper,
                SubjectDirectoryApi subjectDirectoryApi,
                LiabilityPolicyApi liabilityPolicyApi,
                ObjectMapper objectMapper,
                WebSocketSessionManager webSocketSessionManager,
                ChatMessageRealtimeNotifier chatMessageRealtimeNotifier
        ) {
            super(
                    participantService,
                    messageService,
                    messageMapper,
                    messageReadReceiptMapper,
                    subjectDirectoryApi,
                    liabilityPolicyApi,
                    objectMapper,
                    webSocketSessionManager,
                    chatMessageRealtimeNotifier
            );
        }

        @Override
        public void updateLastMessage(Long conversationId, Long messageId, LocalDateTime messageTime) {
            // Unit tests in this class exercise actor write-path contracts, not MyBatis persistence.
        }
    }
}
