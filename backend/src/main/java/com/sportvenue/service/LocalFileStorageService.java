package com.sportvenue.service;

import com.sportvenue.exception.BadRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/bmp",
            "image/heic",
            "image/heif",
            "application/octet-stream"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".heic", ".heif"
    );

    private static final long MAX_BYTES = 5 * 1024 * 1024;

    private final Path avatarDir;
    private final String publicBaseUrl;

    public LocalFileStorageService(
            @Value("${app.files.upload-dir:uploads}") String uploadDir,
            @Value("${app.files.base-url:http://localhost:8080}") String publicBaseUrl) {
        this.avatarDir = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
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
        String storedName = userId + "_" + UUID.randomUUID() + extension;
        Path target = avatarDir.resolve(storedName).normalize();

        if (!target.startsWith(avatarDir)) {
            throw new BadRequestException("Tên file không hợp lệ.");
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Avatar saved: {}", target);
        } catch (IOException e) {
            throw new BadRequestException("Không thể lưu ảnh. Vui lòng thử lại.");
        }

        return publicBaseUrl + "/api/v1/files/avatars/" + storedName;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ảnh để tải lên.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("Ảnh không được vượt quá 5MB.");
        }
        String contentType = file.getContentType() != null
                ? file.getContentType().toLowerCase(Locale.ROOT)
                : "";
        String extension = extractExtension(file);

        boolean typeAllowed = !contentType.isBlank()
                && ALLOWED_CONTENT_TYPES.contains(contentType);
        boolean extensionAllowed = ALLOWED_EXTENSIONS.contains(extension);

        if (!typeAllowed && !extensionAllowed) {
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
        if (ext.isBlank() || !ALLOWED_EXTENSIONS.contains(ext)) {
            return ".jpg";
        }
        return ext;
    }

    public Path getAvatarDirectory() {
        return avatarDir;
    }
}
