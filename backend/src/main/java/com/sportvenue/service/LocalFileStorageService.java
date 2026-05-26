package com.sportvenue.service;

import com.sportvenue.config.FileStorageProperties;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.util.ImageContentValidator;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final Path avatarDir;
    private final String publicBaseUrl;
    private final long maxBytes;
    private final Set<String> allowedContentTypes;
    private final Set<String> allowedExtensions;

    public LocalFileStorageService(FileStorageProperties properties) {
        this.avatarDir = Paths.get(properties.getUploadDir(), "avatars").toAbsolutePath().normalize();
        String baseUrl = properties.getBaseUrl();
        this.publicBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.maxBytes = properties.getMaxSizeBytes();
        this.allowedContentTypes = properties.getAllowedContentTypes().stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.allowedExtensions = properties.getAllowedExtensions().stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        initDirectory();
    }

    private void initDirectory() {
        try {
            Files.createDirectories(avatarDir);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo thư mục lưu ảnh: " + avatarDir, e);
        }
    }

    @Override
    public String storeAvatar(MultipartFile file, Integer userId) {
        validateFile(file);

        String extension = resolveExtension(file);
        String storedName = UUID.randomUUID() + extension;
        Path target = avatarDir.resolve(storedName).normalize();

        if (!target.startsWith(avatarDir)) {
            throw new BadRequestException("Tên file không hợp lệ.");
        }

        try (InputStream raw = file.getInputStream();
                BufferedInputStream in = new BufferedInputStream(raw)) {
            ImageContentValidator.validateImageContent(in, extension);
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Avatar saved for user {}: {}", userId, target);
        } catch (IOException e) {
            throw new BadRequestException("Không thể lưu ảnh. Vui lòng thử lại.");
        }

        return publicBaseUrl + "/api/v1/files/avatars/" + storedName;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ảnh để tải lên.");
        }
        if (file.getSize() > maxBytes) {
            long maxMb = maxBytes / (1024 * 1024);
            throw new BadRequestException("Ảnh không được vượt quá " + maxMb + "MB.");
        }

        String extension = extractExtension(file);
        if (!allowedExtensions.contains(extension)) {
            throw new BadRequestException(
                    "Chỉ chấp nhận ảnh JPG, PNG, WEBP, GIF hoặc BMP. "
                            + "Ảnh HEIC từ iPhone nên chuyển sang JPG trước khi tải.");
        }

        String contentType = file.getContentType() != null
                ? file.getContentType().toLowerCase(Locale.ROOT)
                : "";
        if (!contentType.isBlank()
                && !"application/octet-stream".equals(contentType)
                && !allowedContentTypes.contains(contentType)) {
            throw new BadRequestException(
                    "Chỉ chấp nhận ảnh JPG, PNG, WEBP, GIF hoặc BMP. "
                            + "Ảnh HEIC từ iPhone nên chuyển sang JPG trước khi tải.");
        }
    }

    private String extractExtension(MultipartFile file) {
        String original = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            return original.substring(dot).toLowerCase(Locale.ROOT);
        }
        String type = file.getContentType();
        if ("image/png".equals(type)) {
            return ".png";
        }
        if ("image/webp".equals(type)) {
            return ".webp";
        }
        if ("image/gif".equals(type)) {
            return ".gif";
        }
        if (type != null && type.startsWith("image/")) {
            return ".jpg";
        }
        return "";
    }

    private String resolveExtension(MultipartFile file) {
        String ext = extractExtension(file);
        if (ext.isBlank() || !allowedExtensions.contains(ext)) {
            return ".jpg";
        }
        return ext;
    }

    public Path getAvatarDirectory() {
        return avatarDir;
    }
}
