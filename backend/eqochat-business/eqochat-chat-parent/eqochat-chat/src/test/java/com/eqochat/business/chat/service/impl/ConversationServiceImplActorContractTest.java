package com.eqochat.business.chat.service.impl;

import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.chat.api.dto.request.CreateConversationRequest;
import com.eqochat.business.chat.api.dto.request.MarkConversationReadRequest;
import com.eqochat.business.chat.api.dto.request.SendMessageRequest;
import com.eqochat.business.chat.api.dto.response.ConversationSummaryResponse;
import com.eqochat.business.chat.api.dto.response.MessageResponse;
import com.eqochat.business.chat.api.service.ConversationParticipantService;
import com.eqochat.business.chat.api.service.MessageService;
import com.eqochat.business.chat.entity.Conversation;
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
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
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
    void agentCreatorCreatesConversationAsAgentParticipant() {
        SubjectRef agent = SubjectRef.agent(11L);
        SubjectRef target = SubjectRef.human(12L);
        when(liabilityPolicyApi.resolveLiability(agent)).thenReturn(LiabilityChain.agentToHuman(11L, 2L));
        when(subjectDirectoryApi.getSubject(target)).thenReturn(subject(target, "Target Human"));
        when(participantService.listByParticipant(agent)).thenReturn(List.of());

        CreateConversationRequest request = new CreateConversationRequest();
        request.setCreatorSubjectId(11L);
        request.setCreatorSubjectType(SubjectType.AGENT);
        request.setTargetSubjectId(12L);
        request.setTargetSubjectType(SubjectType.HUMAN);

        service.createConversation(2L, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ConversationParticipant>> participants = ArgumentCaptor.forClass(List.class);
        verify(participantService).saveBatch(participants.capture());
        assertThat(participants.getValue())
                .extracting(ConversationParticipant::getParticipantId, ConversationParticipant::getParticipantType)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(11L, SubjectType.AGENT),
                        org.assertj.core.groups.Tuple.tuple(12L, SubjectType.HUMAN)
                );
    }

    @Test
    void createConversationRequiresExplicitCreatorSubject() {
        CreateConversationRequest request = new CreateConversationRequest();
        request.setTargetSubjectId(12L);
        request.setTargetSubjectType(SubjectType.HUMAN);

        assertThatThrownBy(() -> service.createConversation(2L, request))
                .isInstanceOf(BizException.class)
                .hasMessage("actor.subject.invalid");

        verifyNoInteractions(liabilityPolicyApi, participantService, subjectDirectoryApi);
    }

    @Test
    void listConversationsRequiresExplicitViewerSubject() {
        assertThatThrownBy(() -> service.listConversations(2L, null, null))
                .isInstanceOf(BizException.class)
                .hasMessage("conv.viewer.invalid");

        verifyNoInteractions(liabilityPolicyApi, participantService);
    }

    @Test
    void agentViewerReadsMessagesAsAgentParticipant() {
        SubjectRef agent = SubjectRef.agent(11L);
        Message latest = Message.builder()
                .id(901L)
                .conversationId(10002L)
                .senderId(12L)
                .senderType(SubjectType.HUMAN)
                .messageType("TEXT")
                .content("hello agent")
                .createTime(LocalDateTime.now())
                .build();
        when(liabilityPolicyApi.resolveLiability(agent)).thenReturn(LiabilityChain.agentToHuman(11L, 2L));
        when(participantService.findByConversationAndParticipant(10002L, agent))
                .thenReturn(Optional.of(participant(10002L, agent)));
        when(messageService.getConversationMessages(10002L, null, 20)).thenReturn(List.of(latest));
        when(messageMapper.countOlderMessages(10002L, 901L)).thenReturn(0L);

        service.getMessages(2L, 10002L, null, null, 11L, SubjectType.AGENT);

        verify(participantService).updateLastRead(eq(10002L), eq(agent), eq(901L), any(LocalDateTime.class));
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
    void singleConversationSummaryUsesSubjectOnlineForAgentTarget() {
        SubjectRef viewer = SubjectRef.human(2L);
        SubjectRef agent = SubjectRef.agent(11L);
        WebSocketSession agentSession = mock(WebSocketSession.class);
        when(agentSession.getId()).thenReturn("agent-session");
        when(agentSession.isOpen()).thenReturn(true);
        webSocketSessionManager.registerSubjectSession("2", "11", "AGENT", agentSession);

        Conversation conversation = Conversation.builder()
                .conversationType("SINGLE")
                .title("stale title")
                .unreadCount(0)
                .delToken(0L)
                .build();
        service.save(conversation);
        when(liabilityPolicyApi.resolveLiability(viewer)).thenReturn(LiabilityChain.selfResponsible(2L));
        when(participantService.findByConversationAndParticipant(1000L, viewer))
                .thenReturn(Optional.of(participant(1000L, viewer)));
        when(participantService.listByConversationId(1000L)).thenReturn(List.of(
                participant(1000L, viewer),
                participant(1000L, agent)
        ));
        when(messageMapper.selectByConversationId(1000L, 1)).thenReturn(List.of());
        when(subjectDirectoryApi.getSubject(agent)).thenReturn(subject(agent, "Agent 11"));

        ConversationSummaryResponse response = service.getConversation(2L, 1000L, 2L, SubjectType.HUMAN);

        assertThat(response.getTargetSubjectId()).isEqualTo(11L);
        assertThat(response.getTargetSubjectType()).isEqualTo(SubjectType.AGENT);
        assertThat(response.getTitle()).isEqualTo("Agent 11");
        assertThat(response.getOnline()).isTrue();
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

    private SubjectSummaryResponse subject(SubjectRef ref, String displayName) {
        return SubjectSummaryResponse.builder()
                .id(ref.id())
                .type(ref.type())
                .displayName(displayName)
                .status(SubjectStatus.ACTIVE)
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

        private long nextConversationId = 1000L;
        private Conversation savedConversation;

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

        @Override
        public boolean save(Conversation entity) {
            entity.setId(nextConversationId++);
            this.savedConversation = entity;
            return true;
        }

        @Override
        public Conversation getById(java.io.Serializable id) {
            if (savedConversation != null && savedConversation.getId().equals(id)) {
                return savedConversation;
            }
            return null;
        }
    }
}
