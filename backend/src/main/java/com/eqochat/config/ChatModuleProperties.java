package com.eqochat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Chat 模块运行时配置：附件存储与下载。
 */
@Data
@ConfigurationProperties(prefix = "eqochat.chat")
public class ChatModuleProperties {

    /**
     * 附件落盘目录（绝对或相对运行目录）。
     */
    private String uploadDir = "./data/chat-uploads";

    /**
     * 单个文件最大字节数，默认 20MB。
     */
    private long maxFileBytes = 20L * 1024 * 1024;
}

