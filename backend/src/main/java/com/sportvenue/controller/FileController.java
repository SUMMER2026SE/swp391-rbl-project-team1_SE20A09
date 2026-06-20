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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.sportvenue.entity.Owner;
import com.sportvenue.repository.OwnerRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Upload và truy xuất file")
public class FileController {

    private final FileStorageService fileStorageService;
    private final com.sportvenue.repository.OwnerRepository ownerRepository;

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
    @PreAuthorize("hasRole('Owner')")
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

    @PostMapping(value = "/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải tài liệu đăng ký chủ sân", description = "Upload ảnh Giấy phép kinh doanh hoặc CCCD. Hỗ trợ cả khách đăng ký mới và user nâng cấp.")
    public ResponseEntity<FileUploadResponse> uploadDocument(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file) {
        Integer userId = userPrincipal != null ? userPrincipal.getUser().getUserId() : null;
        String url = fileStorageService.storeDocument(file, userId);
        String fileName = url.substring(url.lastIndexOf('/') + 1);

        return ResponseEntity.ok(FileUploadResponse.builder()
                .url(url)
                .fileName(fileName)
                .build());
    }

    @GetMapping("/documents/{fileName}")
    @Operation(summary = "Xem tài liệu đăng ký", description = "Kiểm tra phân quyền động: chỉ Admin hoặc chính chủ sở hữu tài liệu mới được xem sau khi hồ sơ đã lưu.")
    public ResponseEntity<Resource> getDocument(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("fileName") String fileName) {
        
        Optional<Owner> ownerOpt = ownerRepository
                .findByBusinessLicenseUrlContainingOrIdentityCardUrlContaining(fileName, fileName);

        if (ownerOpt.isPresent()) {
            Owner owner = ownerOpt.get();
            if (userPrincipal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            boolean isAdmin = userPrincipal.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ADMIN".equals(a.getAuthority()));
            boolean isOwnerSelf = owner.getUser().getUserId().equals(userPrincipal.getUser().getUserId());

            if (!isAdmin && !isOwnerSelf) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        Resource resource = fileStorageService.loadDocumentAsResource(fileName);
        
        String contentType = "image/jpeg";
        try {
            Path filePath = fileStorageService.getDocumentDirectory().resolve(fileName).normalize();
            String probedType = Files.probeContentType(filePath);
            if (probedType != null) {
                contentType = probedType;
            }
        } catch (IOException e) {
            // Fallback
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
