package com.eqochat.controller;

import com.eqochat.common.ApiResponse;
import com.eqochat.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件控制器。
 * 提供文件上传和预签名 URL 接口。
 * 使用 S3 存储，文件通过返回的 URL 直接访问。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileUploadService fileUploadService;

    /**
     * 上传文件
     */
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

    /**
     * 删除文件
     */
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

    /**
     * 获取预签名 URL（用于前端直传）
     */
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