package com.eqochat.business.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
    public List<ConversationParticipant> listByParticipantId(Long participantId) {
        return baseMapper.findByParticipantId(participantId);
    }

    @Override
    public List<ConversationParticipant> listByConversationId(Long conversationId) {
        return baseMapper.findByConversationId(conversationId);
    }

    @Override
    public Optional<ConversationParticipant> findByConversationAndParticipant(Long conversationId, Long participantId) {
        return baseMapper.findByConversationAndParticipant(conversationId, participantId);
    }

    @Override
    public void updateLastRead(Long conversationId, Long participantId, Long lastReadMessageId, LocalDateTime readAt) {
        if (conversationId == null || participantId == null || lastReadMessageId == null) {
            return;
        }
        Optional<ConversationParticipant> participantOpt = findByConversationAndParticipant(conversationId, participantId);
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
