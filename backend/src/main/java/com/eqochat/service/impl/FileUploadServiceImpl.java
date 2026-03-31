package com.eqochat.service.impl;

import com.eqochat.common.BizException;
import com.eqochat.config.ChatModuleProperties;
import com.eqochat.service.FileUploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private static final Set<String> IMAGE_EXT =
            Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final Set<String> VIDEO_EXT =
            Set.of(".mp4", ".mov", ".webm");
    private static final Set<String> DOC_EXT =
            Set.of(
                    ".pdf", ".doc", ".docx", ".ppt", ".pptx", ".xls", ".xlsx",
                    ".txt", ".zip", ".rar", ".7z"
            );

    private final ChatModuleProperties properties;

    @Override
    public String storeAndBuildAbsoluteUrl(MultipartFile file, HttpServletRequest request) throws IOException {
        if (file == null || file.isEmpty()) {
            throw BizException.of("file.upload.empty");
        }

        String orig = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String ext = extension(orig);
        if (ext.isBlank()) {
            throw BizException.of("file.upload.invalid_type");
        }

        boolean allowed = IMAGE_EXT.contains(ext) || VIDEO_EXT.contains(ext) || DOC_EXT.contains(ext);
        if (!allowed) {
            throw BizException.of("file.upload.invalid_type");
        }

        if (file.getSize() > properties.getMaxFileBytes()) {
            throw BizException.of("file.upload.too_large");
        }

        Path dir = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        String safeName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = dir.resolve(safeName).normalize();
        if (!target.startsWith(dir)) {
            throw BizException.of("file.upload.invalid_name");
        }

        file.transferTo(target.toFile());

        return ServletUriComponentsBuilder.fromContextPath(request)
                .path("/api/v1/files/download/")
                .path(safeName)
                .build()
                .toUriString();
    }

    @Override
    public Path resolveStoredFile(String filename) {
        if (!StringUtils.hasText(filename) || filename.contains("..")) {
            throw BizException.of("file.upload.invalid_name");
        }
        Path dir = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw BizException.of("file.upload.invalid_name");
        }
        return target;
    }

    private static String extension(String filename) {
        if (!StringUtils.hasText(filename)) return "";
        int i = filename.lastIndexOf('.');
        if (i < 0) return "";
        return filename.substring(i).toLowerCase(Locale.ROOT);
    }
}

