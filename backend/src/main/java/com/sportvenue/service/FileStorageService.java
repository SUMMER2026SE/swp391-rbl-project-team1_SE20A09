package com.sportvenue.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeAvatar(MultipartFile file, Integer userId);

    String storeStadiumImage(MultipartFile file, Integer ownerId);

    String storeDocument(MultipartFile file, Integer userId);
}
