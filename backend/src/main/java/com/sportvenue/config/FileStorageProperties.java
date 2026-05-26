package com.sportvenue.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.files")
public class FileStorageProperties {

    private String uploadDir = "uploads";
    private String baseUrl = "http://localhost:8080";
    private long maxSizeBytes = 5 * 1024 * 1024;
    private List<String> allowedExtensions = List.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".heic", ".heif");
    private List<String> allowedContentTypes = List.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/bmp",
            "image/heic",
            "image/heif");
}
