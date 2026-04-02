package com.eqochat.file.client.s3;

import com.eqochat.file.client.AbstractFileClient;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

import static com.eqochat.file.client.s3.S3FileClientConfig.ENDPOINT_ALIYUN;
import static com.eqochat.file.client.s3.S3FileClientConfig.ENDPOINT_TENCENT;

/**
 * 基于 S3 协议的文件客户端实现。
 * 支持 MinIO、阿里云 OSS、腾讯云 COS、七牛云、华为云 OBS 等。
 */
@Slf4j
public class S3FileClient extends AbstractFileClient<S3FileClientConfig> {

    private MinioClient client;

    public S3FileClient(Long id, S3FileClientConfig config) {
        super(id, config);
    }

    @Override
    protected void doInit() {
        // 补全 domain
        if (config.getDomain() == null || config.getDomain().isBlank()) {
            config.setDomain(buildDomain());
        }
        // 初始化 MinIO 客户端
        client = MinioClient.builder()
                .endpoint(buildEndpointURL())
                .region(buildRegion())
                .credentials(config.getAccessKey(), config.getAccessSecret())
                .build();
    }

    /**
     * 构建 Endpoint URL
     */
    private String buildEndpointURL() {
        String endpoint = config.getEndpoint();
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;
        }
        return "https://" + endpoint;
    }

    /**
     * 构建 Domain 地址
     */
    private String buildDomain() {
        String endpoint = config.getEndpoint();
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint + "/" + config.getBucket();
        }
        // 阿里云、腾讯云、华为云：bucket.endpoint 格式
        return "https://" + config.getBucket() + "." + endpoint;
    }

    /**
     * 构建 Region（阿里云和腾讯云必须）
     */
    private String buildRegion() {
        String endpoint = config.getEndpoint();
        // 阼里云：从 endpoint 提取 region
        if (endpoint.contains(ENDPOINT_ALIYUN)) {
            return endpoint.substring(0, endpoint.indexOf('.'))
                    .replace("-internal", "")
                    .replace("https://", "");
        }
        // 腾讯云：从 endpoint 提取 region
        if (endpoint.contains(ENDPOINT_TENCENT)) {
            String region = endpoint.substring(endpoint.indexOf("cos.") + 4);
            return region.replace("." + ENDPOINT_TENCENT, "");
        }
        return null;
    }

    @Override
    public String upload(byte[] content, String path, String contentType) throws Exception {
        // 执行上传
        client.putObject(PutObjectArgs.builder()
                .bucket(config.getBucket())
                .contentType(contentType)
                .object(path)
                .stream(new ByteArrayInputStream(content), content.length, -1)
                .build());
        // 返回完整 URL
        return config.getDomain() + "/" + path;
    }

    @Override
    public void delete(String path) throws Exception {
        client.removeObject(RemoveObjectArgs.builder()
                .bucket(config.getBucket())
                .object(path)
                .build());
    }

    @Override
    public byte[] getContent(String path) throws Exception {
        GetObjectResponse response = client.getObject(GetObjectArgs.builder()
                .bucket(config.getBucket())
                .object(path)
                .build());
        return response.readAllBytes();
    }

    @Override
    public FilePresignedUrlRespDTO getPresignedObjectUrl(String path) throws Exception {
        String uploadUrl = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(config.getBucket())
                .object(path)
                .expiry(10, TimeUnit.MINUTES)
                .build());
        return new FilePresignedUrlRespDTO(uploadUrl, config.getDomain() + "/" + path);
    }
}