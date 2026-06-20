package com.sportvenue.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Serve uploaded files as static resources.
 * Maps /api/v1/files/avatars/** and /api/v1/files/stadiums/** to the local upload directory.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize().toUri().toString();

        // Ensure trailing slash
        if (!uploadDir.endsWith("/")) {
            uploadDir = uploadDir + "/";
        }

        registry.addResourceHandler("/api/v1/files/avatars/**")
                .addResourceLocations(uploadDir + "avatars/");

        registry.addResourceHandler("/api/v1/files/stadiums/**")
                .addResourceLocations(uploadDir + "stadiums/");

        registry.addResourceHandler("/api/v1/files/documents/**")
                .addResourceLocations(uploadDir + "documents/");
    }
}
