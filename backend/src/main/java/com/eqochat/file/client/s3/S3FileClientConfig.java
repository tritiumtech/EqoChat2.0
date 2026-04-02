package com.eqochat.file.client.s3;

import com.eqochat.file.client.FileClientConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * S3 文件客户端配置类。
 * 支持基于 S3 协议的存储服务：MinIO、阿里云 OSS、腾讯云 COS、七牛云、华为云 OBS 等。
 */
@Data
public class S3FileClientConfig implements FileClientConfig {

    public static final String ENDPOINT_QINIU = "qiniucs.com";
    public static final String ENDPOINT_ALIYUN = "aliyuncs.com";
    public static final String ENDPOINT_TENCENT = "myqcloud.com";

    /**
     * 节点地址
     * MinIO: http://127.0.0.1:9000
     * 阿里云: oss-cn-hangzhou.aliyuncs.com
     * 腾讯云: cos.ap-guangzhou.myqcloud.com
     */
    @NotNull(message = "endpoint 不能为空")
    private String endpoint;

    /**
     * 自定义域名（可选）
     * 若不设置，将自动根据 bucket + endpoint 生成
     */
    private String domain;

    /**
     * 存储 Bucket
     */
    @NotNull(message = "bucket 不能为空")
    private String bucket;

    /**
     * 访问 Key
     */
    @NotNull(message = "accessKey 不能为空")
    private String accessKey;

    /**
     * 访问 Secret
     */
    @NotNull(message = "accessSecret 不能为空")
    private String accessSecret;

    /**
     * 七牛云必须配置自定义域名
     */
    @AssertTrue(message = "七牛云必须配置自定义域名")
    @JsonIgnore
    public boolean isDomainValid() {
        if (endpoint.contains(ENDPOINT_QINIU) && (domain == null || domain.isBlank())) {
            return false;
        }
        return true;
    }
}