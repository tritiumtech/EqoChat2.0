package com.eqochat.business.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eqochat.framework.common.BizException;
import com.eqochat.business.chat.entity.Message;
import com.eqochat.business.chat.mapper.MessageMapper;
import com.eqochat.business.chat.api.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> 
        implements MessageService {
    
    @Override
    public Message sendMessage(Message message) {
        message.setStatus("SENT");
        save(message);
        log.info("消息已保存: messageId={}", message.getId());
        return message;
    }
    
    @Override
    public List<Message> getConversationMessages(Long conversationId, Long lastMessageId, Integer limit) {
        int pageSize = limit != null ? limit : 20;
        if (lastMessageId != null) {
            return baseMapper.selectByConversationIdWithCursor(conversationId, lastMessageId, pageSize);
        }
        return baseMapper.selectByConversationId(conversationId, pageSize);
    }
    
    @Override
    public void markAsRead(Long messageId, Long userId) {
        Message message = getById(messageId);
        if (message != null) {
            message.setStatus("READ");
            updateById(message);
            log.info("消息已标记为已读: messageId={}, userId={}", messageId, userId);
        }
    }
    
    @Override
    public void recallMessage(Long messageId, Long userId) {
        Message message = getById(messageId);
        if (message != null && message.getSenderId().equals(userId)) {
            // 2分钟内可撤回
            if (message.getCreateTime().plusMinutes(2).isAfter(LocalDateTime.now())) {
                message.setStatus("RECALLED");
                updateById(message);
                log.info("消息已撤回: messageId={}", messageId);
            } else {
                throw BizException.of("message.recall.timeout");
            }
        }
    }
}
