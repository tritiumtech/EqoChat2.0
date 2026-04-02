package com.eqochat.file.client;

import com.eqochat.file.client.s3.S3FileClient;
import com.eqochat.file.client.s3.S3FileClientConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件客户端工厂实现类。
 * 简化设计：仅支持 S3 协议客户端，单例模式。
 */
@Slf4j
public class FileClientFactoryImpl implements FileClientFactory {

    private static final Long DEFAULT_CLIENT_ID = 1L;

    private volatile S3FileClient client;

    @Override
    public FileClient getFileClient() {
        if (client == null) {
            log.error("[getFileClient][文件客户端未初始化]");
            throw new IllegalStateException("文件客户端未初始化，请先配置 S3 存储参数");
        }
        return client;
    }

    @Override
    public void createOrUpdateFileClient(S3FileClientConfig config) {
        if (client == null) {
            client = new S3FileClient(DEFAULT_CLIENT_ID, config);
            client.init();
            log.info("[createOrUpdateFileClient][S3 客户端创建完成]");
        } else {
            client.refresh(config);
            log.info("[createOrUpdateFileClient][S3 客户端配置更新完成]");
        }
    }
}