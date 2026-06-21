package com.sportvenue.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

public interface FileStorageService {
    String storeAvatar(MultipartFile file, Integer userId);

    String storeStadiumImage(MultipartFile file, Integer ownerId);

    String storeDocument(MultipartFile file, Integer userId);

    Resource loadDocumentAsResource(String fileName);

    Path getDocumentDirectory();
}
