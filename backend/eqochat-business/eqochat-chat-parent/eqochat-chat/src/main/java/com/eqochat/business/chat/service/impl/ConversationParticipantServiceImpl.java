package com.eqochat.business.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eqochat.business.actor.api.model.SubjectRef;
import com.eqochat.business.actor.api.model.SubjectType;
import com.eqochat.business.chat.api.service.ConversationParticipantService;
import com.eqochat.business.chat.entity.ConversationParticipant;
import com.eqochat.business.chat.mapper.ConversationParticipantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ConversationParticipantServiceImpl
        extends ServiceImpl<ConversationParticipantMapper, ConversationParticipant>
        implements ConversationParticipantService {

    @Override
    public List<ConversationParticipant> listByParticipant(SubjectRef participant) {
        if (participant == null) {
            return List.of();
        }
        return listByParticipant(participant.id(), participant.type());
    }

    @Override
    public List<ConversationParticipant> listByParticipant(Long participantId, SubjectType participantType) {
        if (participantId == null || participantType == null) {
            return List.of();
        }
        return baseMapper.findByParticipant(participantId, participantType);
    }

    @Override
    public List<ConversationParticipant> listByConversationId(Long conversationId) {
        return baseMapper.findByConversationId(conversationId);
    }

    @Override
    public Optional<ConversationParticipant> findByConversationAndParticipant(Long conversationId, SubjectRef participant) {
        if (participant == null) {
            return Optional.empty();
        }
        return findByConversationAndParticipant(conversationId, participant.id(), participant.type());
    }

    @Override
    public Optional<ConversationParticipant> findByConversationAndParticipant(
            Long conversationId,
            Long participantId,
            SubjectType participantType
    ) {
        if (conversationId == null || participantId == null || participantType == null) {
            return Optional.empty();
        }
        return baseMapper.findByConversationAndParticipant(conversationId, participantId, participantType);
    }

    @Override
    public void updateLastRead(Long conversationId, SubjectRef participant, Long lastReadMessageId, LocalDateTime readAt) {
        if (participant == null) {
            return;
        }
        updateLastRead(conversationId, participant.id(), participant.type(), lastReadMessageId, readAt);
    }

    @Override
    public void updateLastRead(
            Long conversationId,
            Long participantId,
            SubjectType participantType,
            Long lastReadMessageId,
            LocalDateTime readAt
    ) {
        if (conversationId == null || participantId == null || participantType == null || lastReadMessageId == null) {
            return;
        }
        Optional<ConversationParticipant> participantOpt =
                findByConversationAndParticipant(conversationId, participantId, participantType);
        if (participantOpt.isEmpty()) {
            return;
        }
        ConversationParticipant participant = participantOpt.get();
        Long current = participant.getLastReadMessageId();
        boolean shouldUpdate = current == null || lastReadMessageId > current;
        if (shouldUpdate) {
            participant.setLastReadMessageId(lastReadMessageId);
            participant.setLastReadAt(readAt != null ? readAt : LocalDateTime.now());
            updateById(participant);
            return;
        }
        if (current != null && current.equals(lastReadMessageId) && readAt != null) {
            participant.setLastReadAt(readAt);
            updateById(participant);
        }
    }
}
