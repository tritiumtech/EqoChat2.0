package com.eqochat.business.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eqochat.business.actor.api.dto.response.SubjectSummaryResponse;
import com.eqochat.business.actor.api.model.LiabilityChain;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectStatus;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.actor.api.service.LiabilityPolicyApi;
import com.eqochat.business.actor.api.service.SubjectDirectoryApi;
import com.eqochat.business.chat.api.dto.request.CreateConversationRequest;
import com.eqochat.business.chat.api.dto.request.MarkConversationReadRequest;
import com.eqochat.business.chat.api.dto.request.SendMessageRequest;
import com.eqochat.business.chat.api.dto.response.ConversationSummaryResponse;
import com.eqochat.business.chat.api.dto.response.MessageAttachmentResponse;
import com.eqochat.business.chat.api.dto.response.MessageResponse;
import com.eqochat.business.chat.api.service.ConversationParticipantService;
import com.eqochat.business.chat.api.service.ConversationService;
import com.eqochat.business.chat.api.service.MessageService;
import com.eqochat.business.chat.entity.Conversation;
import com.eqochat.business.chat.entity.ConversationParticipant;
import com.eqochat.business.chat.entity.Message;
import com.eqochat.business.chat.entity.MessageReadReceipt;
import com.eqochat.business.chat.mapper.ConversationMapper;
import com.eqochat.business.chat.mapper.MessageMapper;
import com.eqochat.business.chat.mapper.MessageReadReceiptMapper;
import com.eqochat.business.chat.websocket.ChatMessageRealtimeNotifier;
import com.eqochat.framework.common.BizException;
import com.eqochat.framework.common.PageResponse;
import com.eqochat.framework.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation>
        implements ConversationService {

    private static final String TYPE_SINGLE = "SINGLE";

    private final ConversationParticipantService participantService;
    private final MessageService messageService;
    private final MessageMapper messageMapper;
    private final MessageReadReceiptMapper messageReadReceiptMapper;
    private final SubjectDirectoryApi subjectDirectoryApi;
    private final LiabilityPolicyApi liabilityPolicyApi;
    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager webSocketSessionManager;
    private final ChatMessageRealtimeNotifier chatMessageRealtimeNotifier;

    @Override
    public List<ConversationSummaryResponse> listConversations(
            Long principalHumanId,
            Long viewerSubjectId,
            SubjectType viewerSubjectType
    ) {
        SubjectRef viewer = resolveAuthorizedViewer(principalHumanId, viewerSubjectId, viewerSubjectType);
        List<ConversationParticipant> participants = participantService.listByParticipant(viewer);
        if (participants.isEmpty()) {
            return List.of();
        }

        Set<Long> conversationIds = participants.stream()
                .map(ConversationParticipant::getConversationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (conversationIds.isEmpty()) {
            return List.of();
        }

        List<Conversation> conversations = lambdaQuery()
                .in(Conversation::getId, conversationIds)
                .eq(Conversation::getDelToken, 0L)
                .orderByDesc(Conversation::getLastMessageAt)
                .list();

        Map<Long, ConversationParticipant> selfParticipantMap = participants.stream()
                .filter(p -> p.getConversationId() != null)
                .collect(Collectors.toMap(ConversationParticipant::getConversationId, p -> p, (a, b) -> a));

        Set<Long> lastMessageIds = conversations.stream()
                .map(Conversation::getLastMessageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Message> lastMessageMap = new HashMap<>();
        if (!lastMessageIds.isEmpty()) {
            lastMessageMap = messageService.listByIds(lastMessageIds).stream()
                    .collect(Collectors.toMap(Message::getId, m -> m));
        }

        List<ConversationSummaryResponse> result = new ArrayList<>();
        for (Conversation conversation : conversations) {
            Message lastMessage = resolveLastMessage(conversation, lastMessageMap);
            ConversationParticipant selfParticipant = selfParticipantMap.get(conversation.getId());
            Long lastReadMessageId = selfParticipant != null ? selfParticipant.getLastReadMessageId() : null;
            long unreadCount = messageMapper.countUnreadMessages(
                    conversation.getId(),
                    viewer.id(),
                    viewer.type(),
                    lastReadMessageId
            );
            result.add(buildSummary(conversation, viewer, lastMessage, (int) unreadCount));
        }

        return result;
    }

    @Override
    public ConversationSummaryResponse createConversation(Long principalHumanId, CreateConversationRequest request) {
        SubjectRef creator = resolveAuthorizedSubject(
                principalHumanId,
                request.getCreatorSubjectId(),
                request.getCreatorSubjectType()
        );
        SubjectRef target = new SubjectRef(request.getTargetSubjectId(), request.getTargetSubjectType());
        validateChatSubject(target);
        if (creator.equals(target)) {
            throw BizException.of("conv.target.invalid");
        }

        SubjectSummaryResponse targetSummary = requireActiveSubject(target);
        Conversation existing = findExistingSingleConversation(creator, target);
        if (existing != null) {
            return buildSummary(existing, creator, resolveLastMessage(existing, Map.of()), existing.getUnreadCount());
        }

        Conversation conversation = Conversation.builder()
                .conversationType(TYPE_SINGLE)
                .title(StringUtils.hasText(request.getTitle()) ? request.getTitle() : targetSummary.getDisplayName())
                .avatarUrl(StringUtils.hasText(request.getAvatarUrl()) ? request.getAvatarUrl() : targetSummary.getAvatarUrl())
                .creatorId(principalHumanId)
                .status("ACTIVE")
                .unreadCount(0)
                .build();
        save(conversation);

        LocalDateTime now = LocalDateTime.now();
        List<ConversationParticipant> participantList = List.of(
                ConversationParticipant.builder()
                        .conversationId(conversation.getId())
                        .participantId(creator.id())
                        .participantType(creator.type())
                        .role(ConversationParticipant.Role.OWNER)
                        .joinedAt(now)
                        .build(),
                ConversationParticipant.builder()
                        .conversationId(conversation.getId())
                        .participantId(target.id())
                        .participantType(target.type())
                        .role(ConversationParticipant.Role.MEMBER)
                        .joinedAt(now)
                        .build()
        );
        participantService.saveBatch(participantList);

        return buildSummary(conversation, creator, null, 0);
    }

    @Override
    public ConversationSummaryResponse getConversation(
            Long principalHumanId,
            Long conversationId,
            Long viewerSubjectId,
            SubjectType viewerSubjectType
    ) {
        SubjectRef viewer = resolveAuthorizedViewer(principalHumanId, viewerSubjectId, viewerSubjectType);
        ConversationParticipant participant = requireParticipant(conversationId, viewer);
        Conversation conversation = requireConversation(conversationId);
        Message lastMessage = resolveLastMessage(conversation, Map.of());
        long unreadCount = messageMapper.countUnreadMessages(
                conversation.getId(),
                viewer.id(),
                viewer.type(),
                participant.getLastReadMessageId()
        );
        return buildSummary(conversation, viewer, lastMessage, (int) unreadCount);
    }

    @Override
    public PageResponse<MessageResponse> getMessages(
            Long principalHumanId,
            Long conversationId,
            Long lastMessageId,
            Integer limit,
            Long viewerSubjectId,
            SubjectType viewerSubjectType
    ) {
        SubjectRef viewer = resolveAuthorizedViewer(principalHumanId, viewerSubjectId, viewerSubjectType);
        requireParticipant(conversationId, viewer);

        int pageSize = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        List<Message> messages = messageService.getConversationMessages(conversationId, lastMessageId, pageSize);
        if (lastMessageId == null && !messages.isEmpty()) {
            Message latest = messages.get(0);
            participantService.updateLastRead(conversationId, viewer, latest.getId(), LocalDateTime.now());
        }

        List<MessageResponse> items = messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());

        Long nextLastMessageId = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();
        boolean hasMore = nextLastMessageId != null && messageMapper.countOlderMessages(conversationId, nextLastMessageId) > 0;
        return PageResponse.of(items, hasMore, nextLastMessageId);
    }

    @Override
    public MessageResponse sendMessage(Long principalHumanId, Long conversationId, SendMessageRequest request) {
        SubjectRef actor = resolveActor(request.getActorSubjectId(), request.getActorSubjectType());
        Long liableHumanId = requireAuthorizedLiability(principalHumanId, actor);
        requireParticipant(conversationId, actor);

        String effectiveMessageType = request.getMessageType() != null ? request.getMessageType() : "TEXT";
        boolean isText = "TEXT".equalsIgnoreCase(effectiveMessageType);
        String content = request.getContent();
        if (isText && !StringUtils.hasText(content)) {
            throw BizException.of("message.content.required");
        }
        if (!isText && request.getMetadata() == null) {
            throw BizException.of("message.metadata.required");
        }

        String contentMetadataJson = serializeMetadata(request.getMetadata());
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(actor.id())
                .senderType(actor.type())
                .liableHumanId(liableHumanId)
                .messageType(effectiveMessageType)
                .content(StringUtils.hasText(content) ? content : null)
                .contentMetadata(contentMetadataJson)
                .intentData(request.getIntentData())
                .replyToMessageId(request.getReplyToMessageId() != null
                        ? Long.parseLong(request.getReplyToMessageId()) : null)
                .status("SENT")
                .build();

        messageService.save(message);

        LocalDateTime createdAt = message.getCreateTime() != null ? message.getCreateTime() : LocalDateTime.now();
        updateLastMessage(conversationId, message.getId(), createdAt);
        participantService.updateLastRead(conversationId, actor, message.getId(), createdAt);
        chatMessageRealtimeNotifier.notifyChatMessageSaved(message);

        return toMessageResponse(message, createdAt);
    }

    @Override
    public void markRead(Long principalHumanId, Long conversationId, MarkConversationReadRequest request) {
        SubjectRef reader = resolveActor(request.getReaderSubjectId(), request.getReaderSubjectType());
        requireAuthorizedLiability(principalHumanId, reader);
        requireParticipant(conversationId, reader);

        Message message = messageService.getById(request.getMessageId());
        if (message == null || !Objects.equals(message.getConversationId(), conversationId)) {
            throw BizException.of("message.not_found");
        }

        if (!Objects.equals(message.getSenderId(), reader.id()) || message.getSenderType() != reader.type()) {
            messageService.markAsRead(message.getId(), reader);
        }
        participantService.updateLastRead(conversationId, reader, message.getId(), LocalDateTime.now());
        insertReadReceipt(message.getId(), reader);
    }

    @Override
    public void updateLastMessage(Long conversationId, Long messageId, LocalDateTime messageTime) {
        if (conversationId == null || messageId == null || messageTime == null) {
            log.warn("updateLastMessage 参数无效: conversationId={}, messageId={}, messageTime={}",
                    conversationId, messageId, messageTime);
            return;
        }
        try {
            Conversation conversation = getById(conversationId);
            if (conversation == null) {
                log.warn("会话不存在: conversationId={}", conversationId);
                return;
            }
            conversation.setLastMessageId(messageId);
            conversation.setLastMessageAt(messageTime);
            updateById(conversation);
            log.debug("更新会话最后消息成功: conversationId={}, messageId={}", conversationId, messageId);
        } catch (Exception e) {
            log.error("更新会话最后消息失败: conversationId={}, messageId={}", conversationId, messageId, e);
        }
    }

    private ConversationSummaryResponse buildSummary(
            Conversation conversation,
            SubjectRef viewer,
            Message lastMessage,
            Integer unreadCount
    ) {
        String title = conversation.getTitle();
        String avatarUrl = conversation.getAvatarUrl();
        Boolean online = null;
        SubjectRef target = null;

        if (TYPE_SINGLE.equals(conversation.getConversationType())) {
            List<ConversationParticipant> participants = participantService.listByConversationId(conversation.getId());
            ConversationParticipant other = findOtherParticipant(participants, viewer);
            if (other != null) {
                target = new SubjectRef(other.getParticipantId(), other.getParticipantType());
                SubjectSummaryResponse subject = subjectDirectoryApi.getSubject(target);
                if (subject != null) {
                    title = subject.getDisplayName();
                    avatarUrl = subject.getAvatarUrl();
                    online = webSocketSessionManager.isSubjectOnline(
                            String.valueOf(target.id()),
                            target.type().name()
                    );
                }
            }
        }

        return ConversationSummaryResponse.builder()
                .id(conversation.getId())
                .title(StringUtils.hasText(title) ? title : "会话")
                .avatarUrl(avatarUrl)
                .conversationType(conversation.getConversationType())
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageAt(lastMessage != null ? lastMessage.getCreateTime() : conversation.getLastMessageAt())
                .unreadCount(unreadCount != null ? unreadCount : 0)
                .online(online)
                .targetSubjectId(target != null ? target.id() : null)
                .targetSubjectType(target != null ? target.type() : null)
                .build();
    }

    private MessageResponse toMessageResponse(Message message) {
        return toMessageResponse(message, message.getCreateTime());
    }

    private MessageResponse toMessageResponse(Message message, LocalDateTime createTime) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderSubjectId(message.getSenderId())
                .senderSubjectType(message.getSenderType())
                .liableHumanId(message.getLiableHumanId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .attachment(parseAttachment(message.getMessageType(), message.getContentMetadata()))
                .createTime(createTime)
                .build();
    }

    private Conversation requireConversation(Long conversationId) {
        Conversation conversation = getById(conversationId);
        if (conversation == null || (conversation.getDelToken() != null && conversation.getDelToken() != 0L)) {
            throw BizException.of("conv.not_found");
        }
        return conversation;
    }

    private ConversationParticipant requireParticipant(Long conversationId, SubjectRef subject) {
        Optional<ConversationParticipant> participant = participantService
                .findByConversationAndParticipant(conversationId, subject);
        if (participant.isEmpty()) {
            throw BizException.of("conv.access.denied");
        }
        return participant.get();
    }

    private SubjectSummaryResponse requireActiveSubject(SubjectRef ref) {
        SubjectSummaryResponse subject = subjectDirectoryApi.getSubject(ref);
        if (subject == null) {
            throw BizException.of("conv.target.not_found");
        }
        if (subject.getStatus() != SubjectStatus.ACTIVE) {
            throw BizException.of("conv.target.invalid");
        }
        return subject;
    }

    private void validateChatSubject(SubjectRef subject) {
        if (subject == null || subject.id() == null || subject.type() == null || subject.type() == SubjectType.SYSTEM) {
            throw BizException.of("conv.target.invalid");
        }
    }

    private SubjectRef resolveActor(Long requestedId, SubjectType requestedType) {
        if (requestedId == null || requestedType == null) {
            throw BizException.of("actor.subject.invalid");
        }
        SubjectRef actor = new SubjectRef(requestedId, requestedType);
        validateChatSubject(actor);
        return actor;
    }

    private SubjectRef resolveAuthorizedSubject(Long principalHumanId, Long requestedId, SubjectType requestedType) {
        SubjectRef subject = resolveActor(requestedId, requestedType);
        requireAuthorizedLiability(principalHumanId, subject);
        return subject;
    }

    private SubjectRef resolveAuthorizedViewer(Long principalHumanId, Long requestedId, SubjectType requestedType) {
        if (requestedId == null || requestedType == null || requestedType == SubjectType.SYSTEM) {
            throw BizException.of("conv.viewer.invalid");
        }
        return resolveAuthorizedSubject(principalHumanId, requestedId, requestedType);
    }

    private Long requireAuthorizedLiability(Long principalHumanId, SubjectRef actor) {
        LiabilityChain chain = liabilityPolicyApi.resolveLiability(actor);
        Long liableHumanId = chain != null ? chain.liableHumanId() : null;
        if (liableHumanId == null || !Objects.equals(liableHumanId, principalHumanId)) {
            throw BizException.of("conv.access.denied");
        }
        return liableHumanId;
    }

    private Message resolveLastMessage(Conversation conversation, Map<Long, Message> lastMessageMap) {
        Message lastMessage = conversation.getLastMessageId() != null
                ? lastMessageMap.get(conversation.getLastMessageId())
                : null;
        if (lastMessage == null && conversation.getLastMessageId() != null) {
            lastMessage = messageService.getById(conversation.getLastMessageId());
        }
        if (lastMessage == null) {
            List<Message> fallback = messageMapper.selectByConversationId(conversation.getId(), 1);
            if (!fallback.isEmpty()) {
                lastMessage = fallback.get(0);
            }
        }
        return lastMessage;
    }

    private ConversationParticipant findOtherParticipant(List<ConversationParticipant> participants, SubjectRef viewer) {
        if (participants == null) {
            return null;
        }
        return participants.stream()
                .filter(p -> p.getParticipantId() != null && p.getParticipantType() != null)
                .filter(p -> !Objects.equals(p.getParticipantId(), viewer.id()) || p.getParticipantType() != viewer.type())
                .findFirst()
                .orElse(null);
    }

    private Conversation findExistingSingleConversation(SubjectRef a, SubjectRef b) {
        List<Long> aConversationIds = participantService.listByParticipant(a).stream()
                .map(ConversationParticipant::getConversationId)
                .collect(Collectors.toList());
        if (aConversationIds.isEmpty()) {
            return null;
        }
        Set<Long> bConversationIds = participantService.listByParticipant(b).stream()
                .map(ConversationParticipant::getConversationId)
                .collect(Collectors.toSet());
        if (bConversationIds.isEmpty()) {
            return null;
        }

        for (Long conversationId : aConversationIds) {
            if (bConversationIds.contains(conversationId)) {
                Conversation conversation = getById(conversationId);
                if (conversation != null && TYPE_SINGLE.equals(conversation.getConversationType())) {
                    return conversation;
                }
            }
        }
        return null;
    }

    private String serializeMetadata(Object metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            throw BizException.of("message.metadata.required");
        }
    }

    private MessageAttachmentResponse parseAttachment(String messageType, String contentMetadata) {
        if (!StringUtils.hasText(contentMetadata)) {
            return null;
        }
        String mt = messageType != null ? messageType.trim().toUpperCase() : "TEXT";
        if ("TEXT".equals(mt)) {
            return null;
        }

        try {
            JsonNode node = objectMapper.readTree(contentMetadata);
            if (node == null || node.isNull()) {
                return null;
            }
            return MessageAttachmentResponse.builder()
                    .fileName(textOrNull(node, "fileName"))
                    .fileSize(textOrNull(node, "fileSize"))
                    .fileType(textOrNull(node, "fileType"))
                    .downloadUrl(textOrNull(node, "downloadUrl"))
                    .build();
        } catch (Exception e) {
            log.warn("解析消息附件失败: messageType={}, contentMetadata={}", messageType, contentMetadata, e);
            return null;
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node != null ? node.get(field) : null;
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text : null;
    }

    private void insertReadReceipt(Long messageId, SubjectRef reader) {
        MessageReadReceipt receipt = MessageReadReceipt.builder()
                .messageId(messageId)
                .readerId(reader.id())
                .readerType(reader.type())
                .readAt(LocalDateTime.now())
                .build();
        try {
            messageReadReceiptMapper.insert(receipt);
        } catch (DuplicateKeyException ignore) {
            // idempotent read marker
        }
    }
}
