package com.eqochat.framework.file.client.s3;

import com.eqochat.framework.file.client.FileClientConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class S3FileClientConfig implements FileClientConfig {

    public static final String ENDPOINT_QINIU = "qiniucs.com";
    public static final String ENDPOINT_ALIYUN = "aliyuncs.com";
    public static final String ENDPOINT_TENCENT = "myqcloud.com";

    @NotNull(message = "endpoint 不能为空")
    private String endpoint;

    private String domain;

    @NotNull(message = "bucket 不能为空")
    private String bucket;

    @NotNull(message = "accessKey 不能为空")
    private String accessKey;

    @NotNull(message = "accessSecret 不能为空")
    private String accessSecret;

    @AssertTrue(message = "七牛云必须配置自定义域名")
    @JsonIgnore
    public boolean isDomainValid() {
        if (endpoint.contains(ENDPOINT_QINIU) && (domain == null || domain.isBlank())) {
            return false;
        }
        return true;
    }
}
