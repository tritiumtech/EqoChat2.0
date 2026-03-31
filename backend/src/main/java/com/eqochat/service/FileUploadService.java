package com.eqochat.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileUploadService {

    /**
     * 存储上传文件，并返回可直接访问的下载 URL。
     */
    String storeAndBuildAbsoluteUrl(MultipartFile file, HttpServletRequest request) throws IOException;

    /**
     * 根据存储文件名解析落盘路径。
     */
    Path resolveStoredFile(String filename);
}

