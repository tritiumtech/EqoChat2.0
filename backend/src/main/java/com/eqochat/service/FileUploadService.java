package com.eqochat.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口。
 */
public interface FileUploadService {

    /**
     * 上传文件并返回可访问的 URL。
     *
     * @param file 上传的文件
     * @return 文件访问 URL
     * @throws Exception 上传异常
     */
    String upload(MultipartFile file) throws Exception;

    /**
     * 删除文件。
     *
     * @param path 文件路径
     * @throws Exception 删除异常
     */
    void delete(String path) throws Exception;

    /**
     * 获取文件内容。
     *
     * @param path 文件路径
     * @return 文件内容
     * @throws Exception 获取异常
     */
    byte[] getContent(String path) throws Exception;

    /**
     * 获取文件预签名 URL（用于前端直传）。
     *
     * @param path 文件路径
     * @return 预签名 URL 信息
     * @throws Exception 获取异常
     */
    FilePresignedUrlResult getPresignedUrl(String path) throws Exception;

    /**
     * 预签名 URL 结果
     */
    record FilePresignedUrlResult(String uploadUrl, String url) {}
}