package com.eqochat.business.chat.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eqochat.business.chat.entity.ConversationParticipant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConversationParticipantService extends IService<ConversationParticipant> {

    List<ConversationParticipant> listByParticipantId(Long participantId);

    List<ConversationParticipant> listByConversationId(Long conversationId);

    Optional<ConversationParticipant> findByConversationAndParticipant(Long conversationId, Long participantId);

    void updateLastRead(Long conversationId, Long participantId, Long lastReadMessageId, LocalDateTime readAt);
}
