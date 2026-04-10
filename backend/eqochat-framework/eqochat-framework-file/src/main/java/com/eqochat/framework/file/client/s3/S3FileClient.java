package com.eqochat.framework.file.client.s3;

import com.eqochat.framework.file.client.AbstractFileClient;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

import static com.eqochat.framework.file.client.s3.S3FileClientConfig.ENDPOINT_ALIYUN;
import static com.eqochat.framework.file.client.s3.S3FileClientConfig.ENDPOINT_TENCENT;

@Slf4j
public class S3FileClient extends AbstractFileClient<S3FileClientConfig> {

    private MinioClient client;

    public S3FileClient(Long id, S3FileClientConfig config) {
        super(id, config);
    }

    @Override
    protected void doInit() {
        if (config.getDomain() == null || config.getDomain().isBlank()) {
            config.setDomain(buildDomain());
        }
        client = MinioClient.builder()
                .endpoint(buildEndpointURL())
                .region(buildRegion())
                .credentials(config.getAccessKey(), config.getAccessSecret())
                .build();
    }

    private String buildEndpointURL() {
        String endpoint = config.getEndpoint();
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;
        }
        return "https://" + endpoint;
    }

    private String buildDomain() {
        String endpoint = config.getEndpoint();
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint + "/" + config.getBucket();
        }
        return "https://" + config.getBucket() + "." + endpoint;
    }

    private String buildRegion() {
        String endpoint = config.getEndpoint();
        if (endpoint.contains(ENDPOINT_ALIYUN)) {
            return endpoint.substring(0, endpoint.indexOf('.'))
                    .replace("-internal", "")
                    .replace("https://", "");
        }
        if (endpoint.contains(ENDPOINT_TENCENT)) {
            String region = endpoint.substring(endpoint.indexOf("cos.") + 4);
            return region.replace("." + ENDPOINT_TENCENT, "");
        }
        return null;
    }

    @Override
    public String upload(byte[] content, String path, String contentType) throws Exception {
        client.putObject(PutObjectArgs.builder()
                .bucket(config.getBucket())
                .contentType(contentType)
                .object(path)
                .stream(new ByteArrayInputStream(content), content.length, -1)
                .build());
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
