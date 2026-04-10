package com.eqochat.business.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eqochat.framework.common.BizException;
import com.eqochat.business.chat.entity.Conversation;
import com.eqochat.business.chat.entity.ConversationParticipant;
import com.eqochat.business.chat.entity.Message;
import com.eqochat.business.user.entity.UserProfile;
import com.eqochat.business.chat.api.dto.request.CreateConversationRequest;
import com.eqochat.business.chat.api.dto.request.SendMessageRequest;
import com.eqochat.business.chat.api.dto.response.ConversationSummaryResponse;
import com.eqochat.business.chat.api.dto.response.MessageAttachmentResponse;
import com.eqochat.business.chat.api.dto.response.MessagePageResponse;
import com.eqochat.business.chat.api.dto.response.MessageResponse;
import com.eqochat.business.chat.mapper.ConversationMapper;
import com.eqochat.business.chat.mapper.MessageMapper;
import com.eqochat.business.chat.api.service.ConversationParticipantService;
import com.eqochat.business.chat.api.service.ConversationService;
import com.eqochat.business.chat.api.service.MessageService;
import com.eqochat.business.user.api.service.UserProfileService;
import com.eqochat.business.chat.websocket.ChatMessageRealtimeNotifier;
import com.eqochat.framework.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation>
        implements ConversationService {
    
    private static final String TYPE_SINGLE = "SINGLE";
    
    private final ConversationParticipantService participantService;
    private final MessageService messageService;
    private final MessageMapper messageMapper;
    private final UserProfileService userProfileService;
    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager webSocketSessionManager;
    private final ChatMessageRealtimeNotifier chatMessageRealtimeNotifier;
    
    @Override
    public List<ConversationSummaryResponse> listConversations(Long userId) {
        List<ConversationParticipant> participants = participantService.listByParticipantId(userId);
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
            Message lastMessage = lastMessageMap.get(conversation.getLastMessageId());
            if (lastMessage == null) {
                List<Message> fallback = messageMapper.selectByConversationId(conversation.getId(), 1);
                if (!fallback.isEmpty()) {
                    lastMessage = fallback.get(0);
                }
            }
            
            String title;
            String avatarUrl;
            Boolean online = null;
            if (TYPE_SINGLE.equals(conversation.getConversationType())) {
                // 单聊场景下强制使用对方用户的信息作为展示名称与头像
                title = null;
                avatarUrl = null;
                // 这里必须拉取当前会话下的所有参与者，不能只看“自己”的参与记录
                List<ConversationParticipant> convParticipants = participantService.listByConversationId(conversation.getId());
                ConversationParticipant other = findOtherParticipant(convParticipants, userId);
                Long otherId = other != null ? other.getParticipantId() : null;
                if (otherId != null) {
                    UserProfile otherUser = userProfileService.getById(otherId);
                    if (otherUser != null) {
                        title = resolveDisplayName(otherUser);
                        avatarUrl = otherUser.getAvatarUrl();
                    }
                    online = webSocketSessionManager.isOnline(String.valueOf(otherId));
                }
            } else {
                // 群聊或其他类型保留会话自身配置
                title = conversation.getTitle();
                avatarUrl = conversation.getAvatarUrl();
            }

            ConversationParticipant selfParticipant = selfParticipantMap.get(conversation.getId());
            Long lastReadMessageId = selfParticipant != null ? selfParticipant.getLastReadMessageId() : null;
            long unreadCount = messageMapper.countUnreadMessages(conversation.getId(), userId, lastReadMessageId);
            
            result.add(ConversationSummaryResponse.builder()
                    .id(conversation.getId())
                    .title(StringUtils.hasText(title) ? title : "会话")
                    .avatarUrl(avatarUrl)
                    .conversationType(conversation.getConversationType())
                    .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                    .lastMessageAt(lastMessage != null ? lastMessage.getCreateTime() : conversation.getLastMessageAt())
                    .unreadCount((int) unreadCount)
                    .online(online)
                    .build());
        }
        
        return result;
    }
    
    @Override
    public ConversationSummaryResponse createConversation(Long userId, CreateConversationRequest request) {
        Long targetUserId = request.getTargetUserId();
        if (targetUserId == null) {
            throw BizException.of("conv.target.required");
        }
        if (userId.equals(targetUserId)) {
            throw BizException.of("conv.target.invalid");
        }
        
        UserProfile targetUser = userProfileService.getById(targetUserId);
        if (targetUser == null) {
            throw BizException.of("conv.target.not_found");
        }
        
        Conversation existing = findExistingSingleConversation(userId, targetUserId);
        if (existing != null) {
            return ConversationSummaryResponse.builder()
                    .id(existing.getId())
                    .title(resolveDisplayName(targetUser))
                    .avatarUrl(targetUser.getAvatarUrl())
                    .conversationType(existing.getConversationType())
                    .lastMessage(null)
                    .lastMessageAt(existing.getLastMessageAt())
                    .unreadCount(existing.getUnreadCount())
                    .build();
        }
        
        Conversation conversation = Conversation.builder()
                .conversationType(TYPE_SINGLE)
                .title(request.getTitle())
                .avatarUrl(request.getAvatarUrl())
                .creatorId(userId)
                .status("ACTIVE")
                .unreadCount(0)
                .build();
        
        save(conversation);
        
        LocalDateTime now = LocalDateTime.now();
        List<ConversationParticipant> participantList = List.of(
                ConversationParticipant.builder()
                        .conversationId(conversation.getId())
                        .participantId(userId)
                        .participantType(ConversationParticipant.ParticipantType.USER)
                        .role(ConversationParticipant.Role.OWNER)
                        .joinedAt(now)
                        .build(),
                ConversationParticipant.builder()
                        .conversationId(conversation.getId())
                        .participantId(targetUserId)
                        .participantType(ConversationParticipant.ParticipantType.USER)
                        .role(ConversationParticipant.Role.MEMBER)
                        .joinedAt(now)
                        .build()
        );
        
        participantService.saveBatch(participantList);
        
        String title = request.getTitle() != null && !request.getTitle().isBlank()
                ? request.getTitle()
                : resolveDisplayName(targetUser);
        
        String avatarUrl = request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()
                ? request.getAvatarUrl()
                : targetUser.getAvatarUrl();
        
        return ConversationSummaryResponse.builder()
                .id(conversation.getId())
                .title(StringUtils.hasText(title) ? title : "会话")
                .avatarUrl(avatarUrl)
                .conversationType(conversation.getConversationType())
                .lastMessage(null)
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(conversation.getUnreadCount())
                .build();
    }

    @Override
    public ConversationSummaryResponse getConversation(Long userId, Long conversationId) {
        Optional<ConversationParticipant> participantOpt = participantService
                .findByConversationAndParticipant(conversationId, userId);
        if (participantOpt.isEmpty()) {
            throw BizException.of("conv.access.denied");
        }

        Conversation conversation = getById(conversationId);
        if (conversation == null || (conversation.getDelToken() != null && conversation.getDelToken() != 0L)) {
            throw BizException.of("conv.not_found");
        }

        List<ConversationParticipant> participants = participantService.listByConversationId(conversationId);
        String title;
        String avatarUrl;
        Boolean online = null;
        if (TYPE_SINGLE.equals(conversation.getConversationType())) {
            // 单聊详情同样强制使用对方用户信息
            title = null;
            avatarUrl = null;
            ConversationParticipant other = findOtherParticipant(participants, userId);
            Long otherId = other != null ? other.getParticipantId() : null;
            if (otherId != null) {
                UserProfile otherUser = userProfileService.getById(otherId);
                if (otherUser != null) {
                    title = resolveDisplayName(otherUser);
                    avatarUrl = otherUser.getAvatarUrl();
                }
                online = webSocketSessionManager.isOnline(String.valueOf(otherId));
            }
        } else {
            title = conversation.getTitle();
            avatarUrl = conversation.getAvatarUrl();
        }

        Message lastMessage = null;
        if (conversation.getLastMessageId() != null) {
            lastMessage = messageService.getById(conversation.getLastMessageId());
        }
        if (lastMessage == null) {
            List<Message> fallback = messageMapper.selectByConversationId(conversation.getId(), 1);
            if (!fallback.isEmpty()) {
                lastMessage = fallback.get(0);
            }
        }

        Long lastReadMessageId = participantOpt.get().getLastReadMessageId();
        long unreadCount = messageMapper.countUnreadMessages(conversation.getId(), userId, lastReadMessageId);

        return ConversationSummaryResponse.builder()
                .id(conversation.getId())
                .title(StringUtils.hasText(title) ? title : "会话")
                .avatarUrl(avatarUrl)
                .conversationType(conversation.getConversationType())
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageAt(lastMessage != null ? lastMessage.getCreateTime() : conversation.getLastMessageAt())
                .unreadCount((int) unreadCount)
                .online(online)
                .build();
    }
    
    @Override
    public MessagePageResponse getMessages(Long userId, Long conversationId, Long lastMessageId, Integer limit) {
        Optional<ConversationParticipant> participant = participantService
                .findByConversationAndParticipant(conversationId, userId);
        if (participant.isEmpty()) {
            throw BizException.of("conv.access.denied");
        }

        int pageSize = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        List<Message> messages = messageService.getConversationMessages(conversationId, lastMessageId, pageSize);
        if (lastMessageId == null && !messages.isEmpty()) {
            Message latest = messages.get(0);
            participantService.updateLastRead(conversationId, userId, latest.getId(), LocalDateTime.now());
        }

        List<MessageResponse> items = messages.stream()
                .map(message -> MessageResponse.builder()
                        .id(message.getId())
                        .conversationId(message.getConversationId())
                        .senderId(message.getSenderId())
                        .senderType(message.getSenderType())
                        .messageType(message.getMessageType())
                        .content(message.getContent())
                        .attachment(parseAttachment(message.getMessageType(), message.getContentMetadata()))
                        .createTime(message.getCreateTime())
                        .build())
                .collect(Collectors.toList());

        Long total = messageMapper.countByConversationId(conversationId);
        Long nextLastMessageId = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();
        boolean hasMore = nextLastMessageId != null && messageMapper.countOlderMessages(conversationId, nextLastMessageId) > 0;

        return MessagePageResponse.builder()
                .items(items)
                .total(total)
                .hasMore(hasMore)
                .nextLastMessageId(nextLastMessageId)
                .build();
    }

    @Override
    public MessageResponse sendMessage(Long userId, Long conversationId, SendMessageRequest request) {
        Optional<ConversationParticipant> participant = participantService
                .findByConversationAndParticipant(conversationId, userId);
        if (participant.isEmpty()) {
            throw BizException.of("conv.access.denied");
        }

        String effectiveMessageType = request.getMessageType() != null ? request.getMessageType() : "TEXT";
        boolean isText = effectiveMessageType == null || "TEXT".equalsIgnoreCase(effectiveMessageType);
        String content = request.getContent();

        if (isText) {
            if (!StringUtils.hasText(content)) {
                throw BizException.of("message.content.required");
            }
        } else {
            if (request.getMetadata() == null) {
                throw BizException.of("message.metadata.required");
            }
        }

        String contentMetadataJson = null;
        if (request.getMetadata() != null) {
            try {
                contentMetadataJson = objectMapper.writeValueAsString(request.getMetadata());
            } catch (Exception e) {
                throw BizException.of("message.metadata.required");
            }
        }

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(userId)
                .senderType("USER")
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
        participantService.updateLastRead(conversationId, userId, message.getId(), createdAt);

        chatMessageRealtimeNotifier.notifyChatMessageSaved(message);

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderType(message.getSenderType())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .attachment(parseAttachment(message.getMessageType(), message.getContentMetadata()))
                .createTime(createdAt)
                .build();
    }

    private MessageAttachmentResponse parseAttachment(String messageType, String contentMetadata) {
        if (contentMetadata == null || !StringUtils.hasText(contentMetadata)) return null;
        String mt = messageType != null ? messageType.trim().toUpperCase() : "TEXT";
        if ("TEXT".equals(mt)) return null;

        try {
            JsonNode node = objectMapper.readTree(contentMetadata);
            if (node == null || node.isNull()) return null;

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
        if (node == null || node.isNull()) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        String s = v.asText();
        return StringUtils.hasText(s) ? s : null;
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
    
    private ConversationParticipant findOtherParticipant(List<ConversationParticipant> participants, Long userId) {
        if (participants == null) {
            return null;
        }
        return participants.stream()
                .filter(p -> p.getParticipantId() != null && !p.getParticipantId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    private String resolveDisplayName(UserProfile user) {
        if (user == null) {
            return "会话";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        if (StringUtils.hasText(user.getPhone())) {
            return user.getPhone();
        }
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail();
        }
        if (StringUtils.hasText(user.getDid())) {
            return user.getDid();
        }
        // 所有可读字段都为空时，至少用用户ID作为可区分标识
        if (user.getId() != null) {
            return "用户" + user.getId();
        }
        return "会话";
    }
    
    private Conversation findExistingSingleConversation(Long userId, Long targetUserId) {
        List<Long> userConvIds = participantService.listByParticipantId(userId).stream()
                .map(ConversationParticipant::getConversationId)
                .collect(Collectors.toList());
        if (userConvIds.isEmpty()) {
            return null;
        }
        Set<Long> targetConvIds = participantService.listByParticipantId(targetUserId).stream()
                .map(ConversationParticipant::getConversationId)
                .collect(Collectors.toSet());
        if (targetConvIds.isEmpty()) {
            return null;
        }
        
        for (Long convId : userConvIds) {
            if (targetConvIds.contains(convId)) {
                Conversation conversation = getById(convId);
                if (conversation != null && TYPE_SINGLE.equals(conversation.getConversationType())) {
                    return conversation;
                }
            }
        }
        
        return null;
    }
}
