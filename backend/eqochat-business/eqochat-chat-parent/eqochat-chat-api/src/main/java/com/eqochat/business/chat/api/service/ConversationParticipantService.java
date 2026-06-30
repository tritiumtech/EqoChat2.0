package com.eqochat.business.chat.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.chat.entity.ConversationParticipant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConversationParticipantService extends IService<ConversationParticipant> {

    List<ConversationParticipant> listByParticipant(SubjectRef participant);

    List<ConversationParticipant> listByParticipant(Long participantId, SubjectType participantType);

    List<ConversationParticipant> listByConversationId(Long conversationId);

    Optional<ConversationParticipant> findByConversationAndParticipant(Long conversationId, SubjectRef participant);

    Optional<ConversationParticipant> findByConversationAndParticipant(
            Long conversationId,
            Long participantId,
            SubjectType participantType
    );

    void updateLastRead(Long conversationId, SubjectRef participant, Long lastReadMessageId, LocalDateTime readAt);

    void updateLastRead(
            Long conversationId,
            Long participantId,
            SubjectType participantType,
            Long lastReadMessageId,
            LocalDateTime readAt
    );
}
