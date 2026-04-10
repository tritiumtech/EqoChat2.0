package com.eqochat.framework.file;

import com.eqochat.framework.file.client.s3.S3FileClientConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储配置属性。
 * 支持 S3 协议存储（MinIO、阿里云 OSS、腾讯云 COS 等）。
 */
@Data
@ConfigurationProperties(prefix = "eqochat.file")
public class FileStorageProperties {

    /**
     * S3 存储配置
     */
    private S3Config s3;

    /**
     * 单个文件最大字节数，默认 20MB
     */
    private long maxFileBytes = 20L * 1024 * 1024;

    /**
     * 文件路径前缀，默认 eqochat/
     */
    private String pathPrefix = "eqochat/";

    @Data
    public static class S3Config {

        /**
         * 节点地址
         * MinIO: http://127.0.0.1:9000
         * 阼里云: oss-cn-hangzhou.aliyuncs.com
         */
        private String endpoint;

        /**
         * 自定义域名（可选）
         */
        private String domain;

        /**
         * 存储 Bucket
         */
        private String bucket;

        /**
         * 访问 Key
         */
        private String accessKey;

        /**
         * 访问 Secret
         */
        private String accessSecret;

        /**
         * 转换为 S3FileClientConfig
         */
        public S3FileClientConfig toClientConfig() {
            S3FileClientConfig config = new S3FileClientConfig();
            config.setEndpoint(this.endpoint);
            config.setDomain(this.domain);
            config.setBucket(this.bucket);
            config.setAccessKey(this.accessKey);
            config.setAccessSecret(this.accessSecret);
            return config;
        }
    }
}