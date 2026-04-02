package com.eqochat.config;

import com.eqochat.file.client.FileClientFactory;
import com.eqochat.file.client.FileClientFactoryImpl;
import com.eqochat.file.client.s3.S3FileClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件客户端配置类。
 * 当配置了 eqochat.file.s3.endpoint 时自动初始化 S3 客户端。
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileClientConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "eqochat.file.s3", name = "endpoint")
    public FileClientFactory fileClientFactory(FileStorageProperties properties) {
        FileClientFactoryImpl factory = new FileClientFactoryImpl();
        FileStorageProperties.S3Config s3Config = properties.getS3();
        if (s3Config != null && s3Config.getEndpoint() != null) {
            S3FileClientConfig clientConfig = s3Config.toClientConfig();
            factory.createOrUpdateFileClient(clientConfig);
            log.info("[fileClientFactory][S3 文件客户端初始化完成，endpoint={}, bucket={}]",
                    s3Config.getEndpoint(), s3Config.getBucket());
        }
        return factory;
    }
}