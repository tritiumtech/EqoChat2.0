package com.eqochat.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket消息协议
 */
public class WebSocketMessage {
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        // 聊天消息
        CHAT_MESSAGE,           // 聊天消息
        CHAT_TYPING,            // 正在输入
        CHAT_READ,              // 已读回执
        
        // 系统通知
        NOTIFICATION,           // 系统通知
        
        // 智能体相关
        AGENT_INTENT,           // 智能体意图
        AGENT_RESPONSE,         // 智能体响应
        
        // 状态相关
        PRESENCE_ONLINE,        // 上线状态
        PRESENCE_OFFLINE,       // 离线状态
        PRESENCE_TYPING,        // 正在输入状态
        
        // 连接控制
        CONNECT_ACK,            // 连接确认
        PING,                   // 心跳请求
        PONG,                   // 心跳响应
        ERROR                   // 错误消息
    }
    
    /**
     * 基础消息格式
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaseMessage {
        private String id;                      // 消息唯一ID
        private MessageType type;               // 消息类型
        private String senderId;                // 发送者ID
        private String senderType;              // USER/AGENT/SYSTEM
        private String recipientId;             // 接收者ID（用户或会话）
        private LocalDateTime timestamp;        // 时间戳
        private Object payload;                 // 消息内容
    }
    
    /**
     * 聊天消息内容
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessagePayload {
        private String conversationId;          // 会话ID
        private String messageType;             // TEXT/IMAGE/FILE/VOICE/VIDEO/CARD
        private String content;                 // 文本内容
        private Object metadata;                // 附加数据（图片URL、文件信息等）
        private String replyToMessageId;        // 回复消息ID
        private String intentData;              // 智能体意图数据
    }
    
    /**
     * 已读回执内容
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadReceiptPayload {
        private String conversationId;          // 会话ID
        private String messageId;               // 消息ID
        private String readerId;                // 阅读者ID
    }
    
    /**
     * 正在输入状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypingPayload {
        private String conversationId;          // 会话ID
        private String userId;                  // 用户ID
        private boolean isTyping;               // 是否正在输入
    }
    
    /**
     * 在线状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresencePayload {
        private String userId;                  // 用户ID
        private String status;                  // ONLINE/OFFLINE/BUSY
        private Long lastSeenAt;                // 最后在线时间
    }
    
    /**
     * 连接确认
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectAckPayload {
        private String userId;                  // 当前用户ID
        private String connectionId;            // 连接ID
        private Long serverTime;                // 服务器时间
    }
    
    /**
     * 错误消息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorPayload {
        private Integer code;                   // 错误码
        private String message;                 // 错误信息
        private String originalMessageId;       // 原消息ID
    }
}
