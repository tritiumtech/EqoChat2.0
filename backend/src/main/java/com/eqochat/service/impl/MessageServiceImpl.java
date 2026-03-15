package com.eqochat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eqochat.domain.entity.Message;
import com.eqochat.mapper.MessageMapper;
import com.eqochat.service.MessageService;
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
        // TODO: 实现分页查询
        return lambdaQuery()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getDelToken, "0")
                .orderByDesc(Message::getCreateTime)
                .last("LIMIT " + (limit != null ? limit : 20))
                .list();
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
                throw new RuntimeException("消息已超过2分钟，无法撤回");
            }
        }
    }
}
