package com.eqochat.framework.file.client;

import com.eqochat.framework.file.client.s3.S3FileClientConfig;

public interface FileClientFactory {

    FileClient getFileClient();

    void createOrUpdateFileClient(S3FileClientConfig config);
}
