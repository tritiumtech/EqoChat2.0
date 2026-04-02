package com.eqochat.file.client;

import com.eqochat.file.client.s3.S3FileClientConfig;

/**
 * 文件客户端工厂接口。
 */
public interface FileClientFactory {

    /**
     * 获得文件客户端
     *
     * @return 文件客户端
     */
    FileClient getFileClient();

    /**
     * 创建或更新文件客户端
     *
     * @param config 文件配置
     */
    void createOrUpdateFileClient(S3FileClientConfig config);
}