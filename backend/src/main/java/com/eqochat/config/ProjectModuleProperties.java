package com.eqochat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Project 模块运行时配置：分享链接前缀。
 */
@Data
@ConfigurationProperties(prefix = "eqochat.project")
public class ProjectModuleProperties {

    /**
     * 对外分享落地页 URL 前缀，须包含占位符 {projectId}。
     */
    private String shareUrlTemplate = "https://example.com/project/{projectId}";
}

