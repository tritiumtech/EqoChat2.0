package com.eqochat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * World 模块运行时配置：发帖附件存储与分享链接前缀。
 */
@Data
@ConfigurationProperties(prefix = "eqochat.world")
public class WorldModuleProperties {

    /**
     * 附件落盘目录（绝对或相对运行目录）。
     */
    private String uploadDir = "./data/world-uploads";

    /**
     * 单张图片最大字节数，默认 5MB。
     */
    private long maxImageBytes = 5L * 1024 * 1024;

    /**
     * 单个视频最大字节数，默认 50MB。
     */
    private long maxVideoBytes = 50L * 1024 * 1024;

    /**
     * 对外分享落地页 URL 前缀，须包含占位符 {postId}，例如 https://example.com/world/post/{postId}
     */
    private String shareUrlTemplate = "https://example.com/world/post/{postId}";
}
