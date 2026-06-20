package com.sportvenue.job;

import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Tác vụ nền dọn dẹp các ảnh tài liệu đăng ký chủ sân bị spam hoặc "vô chủ"
 * (tải lên nhưng không bấm submit form đăng ký/nâng cấp) quá 24 giờ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentCleanupJob {

    private final FileStorageService fileStorageService;
    private final OwnerRepository ownerRepository;

    /**
     * Chạy định kỳ lúc 2:00 sáng hàng ngày để dọn dẹp tài liệu dư thừa.
     * Cron: "giây phút giờ ngày tháng thứ"
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanOrphanDocuments() {
        log.info("DocumentCleanupJob: Bắt đầu quét dọn dẹp tài liệu vô chủ...");
        Path docDir = fileStorageService.getDocumentDirectory();

        if (docDir == null || !Files.exists(docDir)) {
            log.warn("DocumentCleanupJob: Thư mục chứa tài liệu không tồn tại: {}", docDir);
            return;
        }

        Instant cutoffTime = Instant.now().minus(24, ChronoUnit.HOURS);
        int deletedCount = 0;
        int checkedCount = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(docDir)) {
            for (Path file : stream) {
                if (Files.isDirectory(file)) {
                    continue;
                }
                checkedCount++;

                try {
                    Instant lastModified = Files.getLastModifiedTime(file).toInstant();

                    // Chỉ kiểm tra và dọn dẹp các file đã được sửa đổi quá 24 giờ trước
                    if (lastModified.isBefore(cutoffTime)) {
                        String fileName = file.getFileName().toString();

                        // Kiểm tra xem file có được liên kết với hồ sơ chủ sân nào trong DB hay không
                        boolean isReferenced = ownerRepository
                                .existsByBusinessLicenseUrlContainingOrIdentityCardUrlContaining(fileName, fileName);

                        if (!isReferenced) {
                            Files.delete(file);
                            deletedCount++;
                            log.info("DocumentCleanupJob: Đã xóa file vô chủ: {}", fileName);
                        }
                    }
                } catch (IOException e) {
                    log.error("DocumentCleanupJob: Lỗi khi xử lý file: {}", file.getFileName(), e);
                }
            }
        } catch (IOException e) {
            log.error("DocumentCleanupJob: Lỗi khi mở thư mục tài liệu: {}", docDir, e);
        }

        log.info("DocumentCleanupJob: Hoàn tất dọn dẹp. Đã kiểm tra {} file, xóa {} file tài liệu vô chủ.", 
                checkedCount, deletedCount);
    }
}
