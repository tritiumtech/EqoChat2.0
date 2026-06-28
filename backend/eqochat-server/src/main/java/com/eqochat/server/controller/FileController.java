package com.eqochat.server.controller;

import com.eqochat.framework.common.ApiResponse;
import com.eqochat.framework.file.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 全局文件上传（S3/MinIO），依赖 {@link FileUploadService} 与存储配置。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "eqochat.file.s3", name = "endpoint")
public class FileController {

    private final FileUploadService fileUploadService;

    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        try {
            String url = fileUploadService.upload(file);
            return ApiResponse.success(Map.of("url", url));
        } catch (Exception e) {
            log.error("[upload][文件上传失败]", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/{path}")
    public ApiResponse<Void> delete(@PathVariable String path) {
        try {
            fileUploadService.delete(path);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("[delete][文件删除失败: {}]", path, e);
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }

    @GetMapping("/presigned-url")
    public ApiResponse<Map<String, String>> getPresignedUrl(@RequestParam String filename) {
        try {
            FileUploadService.FilePresignedUrlResult result = fileUploadService.getPresignedUrl(filename);
            return ApiResponse.success(Map.of(
                    "uploadUrl", result.uploadUrl(),
                    "url", result.url()
            ));
        } catch (Exception e) {
            log.error("[getPresignedUrl][获取预签名 URL 失败: {}]", filename, e);
            throw new RuntimeException("获取预签名 URL 失败: " + e.getMessage(), e);
        }
    }
}
