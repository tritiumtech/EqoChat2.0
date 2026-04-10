package com.eqochat.business.chat.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eqochat.business.chat.entity.Conversation;
import com.eqochat.business.chat.api.dto.request.CreateConversationRequest;
import com.eqochat.business.chat.api.dto.request.SendMessageRequest;
import com.eqochat.business.chat.api.dto.response.ConversationSummaryResponse;
import com.eqochat.business.chat.api.dto.response.MessagePageResponse;
import com.eqochat.business.chat.api.dto.response.MessageResponse;

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
