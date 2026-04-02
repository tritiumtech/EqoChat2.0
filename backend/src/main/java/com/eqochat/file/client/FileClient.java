package com.eqochat.file.client;

import com.eqochat.file.client.s3.FilePresignedUrlRespDTO;

/**
 * 文件客户端接口，定义文件存储的基本操作。
 */
public interface FileClient {

    /**
     * 获得客户端编号
     *
     * @return 客户端编号
     */
    Long getId();

    /**
     * 上传文件
     *
     * @param content 文件内容
     * @param path    相对路径
     * @param contentType 文件类型
     * @return 完整路径，即 HTTP 访问地址
     * @throws Exception 上传文件时抛出异常
     */
    String upload(byte[] content, String path, String contentType) throws Exception;

    /**
     * 删除文件
     *
     * @param path 相对路径
     * @throws Exception 删除文件时抛出异常
     */
    void delete(String path) throws Exception;

    /**
     * 获得文件内容
     *
     * @param path 相对路径
     * @return 文件内容
     * @throws Exception 获取文件时抛出异常
     */
    byte[] getContent(String path) throws Exception;

    /**
     * 获得文件预签名地址
     *
     * @param path 相对路径
     * @return 文件预签名地址
     * @throws Exception 获取预签名地址时抛出异常
     */
    default FilePresignedUrlRespDTO getPresignedObjectUrl(String path) throws Exception {
        throw new UnsupportedOperationException("不支持的操作");
    }
}