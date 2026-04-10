package com.eqochat.framework.file.client;

import com.eqochat.framework.file.client.s3.FilePresignedUrlRespDTO;

public interface FileClient {

    Long getId();

    String upload(byte[] content, String path, String contentType) throws Exception;

    void delete(String path) throws Exception;

    byte[] getContent(String path) throws Exception;

    default FilePresignedUrlRespDTO getPresignedObjectUrl(String path) throws Exception {
        throw new UnsupportedOperationException("不支持的操作");
    }
}
