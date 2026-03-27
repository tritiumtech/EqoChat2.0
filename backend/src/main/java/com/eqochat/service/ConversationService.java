package com.eqochat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eqochat.domain.entity.Conversation;
import com.eqochat.dto.request.CreateConversationRequest;
import com.eqochat.dto.request.SendMessageRequest;
import com.eqochat.dto.response.ConversationSummaryResponse;
import com.eqochat.dto.response.MessagePageResponse;
import com.eqochat.dto.response.MessageResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversationService extends IService<Conversation> {
    
    List<ConversationSummaryResponse> listConversations(Long userId);

    ConversationSummaryResponse getConversation(Long userId, Long conversationId);
    
    ConversationSummaryResponse createConversation(Long userId, CreateConversationRequest request);

    MessagePageResponse getMessages(Long userId, Long conversationId, Long lastMessageId, Integer limit);

    MessageResponse sendMessage(Long userId, Long conversationId, SendMessageRequest request);

    void updateLastMessage(Long conversationId, Long messageId, LocalDateTime messageTime);
}
