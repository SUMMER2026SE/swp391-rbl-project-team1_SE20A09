package com.sportvenue.config;

import com.sportvenue.service.LocalFileStorageService;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
@RequiredArgsConstructor
public class FileStorageConfig implements WebMvcConfigurer {

    private final LocalFileStorageService localFileStorageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path avatarDir = localFileStorageService.getAvatarDirectory();
        String location = "file:" + avatarDir.toString().replace("\\", "/") + "/";
        registry.addResourceHandler("/api/v1/files/avatars/**")
                .addResourceLocations(location);
    }
}
