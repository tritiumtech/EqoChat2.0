package com.eqochat.framework.file;

import com.eqochat.framework.common.BizException;
import com.eqochat.framework.file.client.FileClient;
import com.eqochat.framework.file.client.FileClientFactory;
import com.eqochat.framework.file.client.s3.FilePresignedUrlRespDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "eqochat.file.s3", name = "endpoint")
public class FileUploadServiceImpl implements FileUploadService {

    private static final Set<String> IMAGE_EXT =
            Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final Set<String> VIDEO_EXT =
            Set.of(".mp4", ".mov", ".webm");
    private static final Set<String> DOC_EXT =
            Set.of(".pdf", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx",
                    ".txt", ".zip", ".rar", ".7z");

    private final FileClientFactory fileClientFactory;
    private final FileStorageProperties properties;

    @Override
    public String upload(MultipartFile file) throws Exception {
        validateFile(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String extension = extractExtension(originalFilename);
        String safeName = UUID.randomUUID().toString().replace("-", "") + extension;
        String path = properties.getPathPrefix() + safeName;

        FileClient client = fileClientFactory.getFileClient();
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        return client.upload(file.getBytes(), path, contentType);
    }

    @Override
    public void delete(String path) throws Exception {
        FileClient client = fileClientFactory.getFileClient();
        client.delete(path);
    }

    @Override
    public byte[] getContent(String path) throws Exception {
        FileClient client = fileClientFactory.getFileClient();
        return client.getContent(path);
    }

    @Override
    public FilePresignedUrlResult getPresignedUrl(String path) throws Exception {
        FileClient client = fileClientFactory.getFileClient();
        FilePresignedUrlRespDTO dto = client.getPresignedObjectUrl(properties.getPathPrefix() + path);
        return new FilePresignedUrlResult(dto.getUploadUrl(), dto.getUrl());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.of("file.upload.empty");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String extension = extractExtension(originalFilename);

        if (extension.isBlank()) {
            throw BizException.of("file.upload.invalid_type");
        }

        boolean allowed = IMAGE_EXT.contains(extension) || VIDEO_EXT.contains(extension) || DOC_EXT.contains(extension);
        if (!allowed) {
            throw BizException.of("file.upload.invalid_type");
        }

        if (file.getSize() > properties.getMaxFileBytes()) {
            throw BizException.of("file.upload.too_large");
        }
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }
        return filename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }
}
