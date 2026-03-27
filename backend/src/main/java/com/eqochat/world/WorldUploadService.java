package com.eqochat.world;

import com.eqochat.common.BizException;
import com.eqochat.config.WorldModuleProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
public class WorldUploadService {

    private static final Set<String> IMAGE_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final Set<String> VIDEO_EXT = Set.of(".mp4", ".mov", ".webm");

    private final WorldModuleProperties properties;

    public String storeAndBuildAbsoluteUrl(MultipartFile file, HttpServletRequest request) throws IOException {
        if (file == null || file.isEmpty()) {
            throw BizException.of("world.upload.empty");
        }
        String orig = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String ext = extension(orig);
        long maxBytes;
        if (IMAGE_EXT.contains(ext)) {
            maxBytes = properties.getMaxImageBytes();
        } else if (VIDEO_EXT.contains(ext)) {
            maxBytes = properties.getMaxVideoBytes();
        } else {
            throw BizException.of("world.upload.invalid_type");
        }
        if (file.getSize() > maxBytes) {
            throw BizException.of("world.upload.too_large");
        }

        Path dir = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String safeName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = dir.resolve(safeName).normalize();
        if (!target.startsWith(dir)) {
            throw BizException.of("world.upload.invalid_name");
        }
        file.transferTo(target.toFile());

        return ServletUriComponentsBuilder.fromContextPath(request)
                .path("/api/v1/world/uploads/")
                .path(safeName)
                .build()
                .toUriString();
    }

    public Path resolveStoredFile(String filename) {
        if (filename == null || filename.isBlank() || filename.contains("..")) {
            throw BizException.of("world.upload.invalid_name");
        }
        Path dir = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw BizException.of("world.upload.invalid_name");
        }
        return target;
    }

    private static String extension(String filename) {
        int i = filename.lastIndexOf('.');
        if (i < 0) return "";
        return filename.substring(i).toLowerCase(Locale.ROOT);
    }
}
