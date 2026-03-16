package com.eqochat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eqochat.common.BizException;
import com.eqochat.domain.entity.Conversation;
import com.eqochat.domain.entity.ConversationParticipant;
import com.eqochat.domain.entity.Message;
import com.eqochat.domain.entity.UserProfile;
import com.eqochat.dto.request.CreateConversationRequest;
import com.eqochat.dto.request.SendMessageRequest;
import com.eqochat.dto.response.ConversationSummaryResponse;
import com.eqochat.dto.response.MessageResponse;
import com.eqochat.mapper.ConversationMapper;
import com.eqochat.mapper.MessageMapper;
import com.eqochat.service.ConversationParticipantService;
import com.eqochat.service.ConversationService;
import com.eqochat.service.MessageService;
import com.eqochat.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        
        Map<Long, List<ConversationParticipant>> participantsByConv = participants.stream()
                .collect(Collectors.groupingBy(ConversationParticipant::getConversationId));
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
            
            String title = conversation.getTitle();
            String avatarUrl = conversation.getAvatarUrl();
            if (TYPE_SINGLE.equals(conversation.getConversationType())) {
                Long otherId = findOtherParticipantId(participantsByConv.get(conversation.getId()), userId);
                if (otherId != null) {
                    UserProfile otherUser = userProfileService.getById(otherId);
                    if (otherUser != null) {
                        title = resolveDisplayName(otherUser);
                        if (avatarUrl == null || avatarUrl.isBlank()) {
                            avatarUrl = otherUser.getAvatarUrl();
                        }
                    }
                }
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
        String title = conversation.getTitle();
        String avatarUrl = conversation.getAvatarUrl();
        if (TYPE_SINGLE.equals(conversation.getConversationType())) {
            Long otherId = findOtherParticipantId(participants, userId);
            if (otherId != null) {
                UserProfile otherUser = userProfileService.getById(otherId);
                if (otherUser != null) {
                    title = resolveDisplayName(otherUser);
                    if (!StringUtils.hasText(avatarUrl)) {
                        avatarUrl = otherUser.getAvatarUrl();
                    }
                }
            }
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
                .build();
    }
    
    @Override
    public List<MessageResponse> getMessages(Long userId, Long conversationId, Long lastMessageId, Integer limit) {
        Optional<ConversationParticipant> participant = participantService
                .findByConversationAndParticipant(conversationId, userId);
        if (participant.isEmpty()) {
            throw BizException.of("conv.access.denied");
        }
        
        List<Message> messages = messageService.getConversationMessages(conversationId, lastMessageId, limit);
        if (lastMessageId == null && !messages.isEmpty()) {
            Message latest = messages.get(0);
            participantService.updateLastRead(conversationId, userId, latest.getId(), LocalDateTime.now());
        }
        return messages.stream()
                .map(message -> MessageResponse.builder()
                        .id(message.getId())
                        .conversationId(message.getConversationId())
                        .senderId(message.getSenderId())
                        .senderType(message.getSenderType())
                        .messageType(message.getMessageType())
                        .content(message.getContent())
                        .createTime(message.getCreateTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public MessageResponse sendMessage(Long userId, Long conversationId, SendMessageRequest request) {
        Optional<ConversationParticipant> participant = participantService
                .findByConversationAndParticipant(conversationId, userId);
        if (participant.isEmpty()) {
            throw BizException.of("conv.access.denied");
        }

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(userId)
                .senderType("USER")
                .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
                .content(request.getContent())
                .contentMetadata(request.getMetadata() != null ? request.getMetadata().toString() : null)
                .intentData(request.getIntentData())
                .replyToMessageId(request.getReplyToMessageId() != null
                        ? Long.parseLong(request.getReplyToMessageId()) : null)
                .status("SENT")
                .build();

        messageService.save(message);

        LocalDateTime createdAt = message.getCreateTime() != null ? message.getCreateTime() : LocalDateTime.now();
        updateLastMessage(conversationId, message.getId(), createdAt);
        participantService.updateLastRead(conversationId, userId, message.getId(), createdAt);

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .senderType(message.getSenderType())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .createTime(createdAt)
                .build();
    }
    
    @Override
    public void updateLastMessage(Long conversationId, Long messageId, LocalDateTime messageTime) {
        Conversation conversation = getById(conversationId);
        if (conversation == null) {
            return;
        }
        conversation.setLastMessageId(messageId);
        conversation.setLastMessageAt(messageTime);
        updateById(conversation);
    }
    
    private Long findOtherParticipantId(List<ConversationParticipant> participants, Long userId) {
        if (participants == null) {
            return null;
        }
        return participants.stream()
                .filter(p -> p.getParticipantId() != null && !p.getParticipantId().equals(userId))
                .map(ConversationParticipant::getParticipantId)
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
