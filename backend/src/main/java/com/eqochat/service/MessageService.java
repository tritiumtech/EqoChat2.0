package com.eqochat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eqochat.domain.entity.Message;

import java.util.List;

public interface MessageService extends IService<Message> {
    
    /**
     * 发送消息
     */
    Message sendMessage(Message message);
    
    /**
     * 获取会话消息列表
     */
    List<Message> getConversationMessages(Long conversationId, Long lastMessageId, Integer limit);
    
    /**
     * 标记消息已读
     */
    void markAsRead(Long messageId, Long userId);
    
    /**
     * 撤回消息
     */
    void recallMessage(Long messageId, Long userId);
}
