package com.eqochat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eqochat.domain.entity.ConversationParticipant;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface ConversationParticipantService extends IService<ConversationParticipant> {
    
    List<ConversationParticipant> listByParticipantId(Long participantId);

    List<ConversationParticipant> listByConversationId(Long conversationId);
    
    Optional<ConversationParticipant> findByConversationAndParticipant(Long conversationId, Long participantId);

    void updateLastRead(Long conversationId, Long participantId, Long lastReadMessageId, LocalDateTime readAt);
}
