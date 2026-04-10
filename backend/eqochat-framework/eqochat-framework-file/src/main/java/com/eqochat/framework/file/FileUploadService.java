package com.eqochat.framework.file;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传门面：基于 {@link com.eqochat.framework.file.client.FileClient} 与存储配置。
 */
public interface FileUploadService {

    String upload(MultipartFile file) throws Exception;

    void delete(String path) throws Exception;

    byte[] getContent(String path) throws Exception;

    FilePresignedUrlResult getPresignedUrl(String path) throws Exception;

    record FilePresignedUrlResult(String uploadUrl, String url) {}
}
