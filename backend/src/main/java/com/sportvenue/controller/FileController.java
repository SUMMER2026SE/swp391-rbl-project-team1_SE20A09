package com.sportvenue.controller;

import com.sportvenue.dto.FileUploadResponse;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Upload và truy xuất file")
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải ảnh đại diện", description = "Upload ảnh từ máy tính hoặc Google Drive (tải về rồi gửi lên)")
    public ResponseEntity<FileUploadResponse> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file) {
        if (userPrincipal == null) {
            throw new BadRequestException(
                    "Bạn cần đăng nhập để tải ảnh. Vui lòng đăng xuất và đăng nhập lại.");
        }

        String url = fileStorageService.storeAvatar(file, userPrincipal.getUser().getUserId());
        String fileName = url.substring(url.lastIndexOf('/') + 1);

        return ResponseEntity.ok(FileUploadResponse.builder()
                .url(url)
                .fileName(fileName)
                .build());
    }

    @PostMapping(value = "/stadium", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải ảnh sân vận động", description = "Upload ảnh sân vận động. Yêu cầu ROLE_OWNER.")
    public ResponseEntity<FileUploadResponse> uploadStadiumImage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file) {
        if (userPrincipal == null) {
            throw new BadRequestException("Bạn cần đăng nhập để tải ảnh.");
        }

        String url = fileStorageService.storeStadiumImage(file, userPrincipal.getUser().getUserId());
        String fileName = url.substring(url.lastIndexOf('/') + 1);

        return ResponseEntity.ok(FileUploadResponse.builder()
                .url(url)
                .fileName(fileName)
                .build());
    }
}
