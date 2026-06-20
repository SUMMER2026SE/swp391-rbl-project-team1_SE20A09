package com.sportvenue.job;

import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentCleanupJobTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private OwnerRepository ownerRepository;

    @InjectMocks
    private DocumentCleanupJob documentCleanupJob;

    @Test
    void cleanOrphanDocuments_CleansOnlyOrphansOlderThan24Hours(@TempDir Path tempDir) throws IOException {
        // Arrange
        // 1. File mới tạo (chưa quá 24h) - không được xóa dù là orphan
        Path newOrphan = tempDir.resolve("new-orphan.jpg");
        Files.writeString(newOrphan, "new-data");

        // 2. File cũ (quá 24h) nhưng có liên kết DB - không được xóa
        Path oldLinked = tempDir.resolve("old-linked.jpg");
        Files.writeString(oldLinked, "linked-data");
        Files.setLastModifiedTime(oldLinked, FileTime.from(Instant.now().minus(25, ChronoUnit.HOURS)));

        // 3. File cũ (quá 24h) và không có liên kết DB - BẮT BUỘC BỊ XÓA
        Path oldOrphan = tempDir.resolve("old-orphan.jpg");
        Files.writeString(oldOrphan, "orphan-data");
        Files.setLastModifiedTime(oldOrphan, FileTime.from(Instant.now().minus(25, ChronoUnit.HOURS)));

        // Setup mock service
        when(fileStorageService.getDocumentDirectory()).thenReturn(tempDir);

        // Setup mock repository
        // oldLinked có liên kết -> true
        when(ownerRepository.existsByBusinessLicenseUrlContainingOrIdentityCardUrlContaining(
                eq("old-linked.jpg"), eq("old-linked.jpg"))).thenReturn(true);
        // oldOrphan không liên kết -> false
        when(ownerRepository.existsByBusinessLicenseUrlContainingOrIdentityCardUrlContaining(
                eq("old-orphan.jpg"), eq("old-orphan.jpg"))).thenReturn(false);

        // Act
        documentCleanupJob.cleanOrphanDocuments();

        // Assert
        // File mới tạo dù là orphan vẫn phải giữ lại (để phòng trường hợp đang submit dở)
        assertTrue(Files.exists(newOrphan), "File mới tạo chưa quá 24h không được xóa");
        
        // File cũ nhưng đã liên kết trong DB không được xóa
        assertTrue(Files.exists(oldLinked), "File đã liên kết DB không được xóa");
        
        // File cũ và không có liên kết DB phải bị xóa
        assertFalse(Files.exists(oldOrphan), "File cũ vô chủ phải bị dọn dẹp");

        verify(ownerRepository).existsByBusinessLicenseUrlContainingOrIdentityCardUrlContaining("old-linked.jpg", "old-linked.jpg");
        verify(ownerRepository).existsByBusinessLicenseUrlContainingOrIdentityCardUrlContaining("old-orphan.jpg", "old-orphan.jpg");
    }
}
