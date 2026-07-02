package com.eqochat.business.chat.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.chat.entity.Conversation;
import com.eqochat.business.chat.api.dto.request.CreateConversationRequest;
import com.eqochat.business.chat.api.dto.request.MarkConversationReadRequest;
import com.eqochat.business.chat.api.dto.request.SendMessageRequest;
import com.eqochat.business.chat.api.dto.response.ConversationSummaryResponse;
import com.eqochat.business.chat.api.dto.response.MessageResponse;
import com.eqochat.framework.common.PageResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversationService extends IService<Conversation> {
    
    List<ConversationSummaryResponse> listConversations(Long principalHumanId, Long viewerSubjectId, SubjectType viewerSubjectType);

    ConversationSummaryResponse getConversation(Long principalHumanId, Long conversationId, Long viewerSubjectId, SubjectType viewerSubjectType);
    
    ConversationSummaryResponse createConversation(Long principalHumanId, CreateConversationRequest request);

    PageResponse<MessageResponse> getMessages(Long principalHumanId, Long conversationId, Long lastMessageId, Integer limit, Long viewerSubjectId, SubjectType viewerSubjectType);

    MessageResponse sendMessage(Long principalHumanId, Long conversationId, SendMessageRequest request);

    void markRead(Long principalHumanId, Long conversationId, MarkConversationReadRequest request);

    void updateLastMessage(Long conversationId, Long messageId, LocalDateTime messageTime);
}
