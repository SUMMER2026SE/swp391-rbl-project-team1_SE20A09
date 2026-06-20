package com.sportvenue.controller;

import com.sportvenue.entity.Owner;
import com.sportvenue.entity.User;
import com.sportvenue.entity.Role;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private OwnerRepository ownerRepository;

    @InjectMocks
    private FileController fileController;

    private UserPrincipal adminPrincipal;
    private UserPrincipal ownerPrincipal;
    private UserPrincipal otherPrincipal;

    @BeforeEach
    void setUp() {
        Role adminRole = Role.builder().roleId(1).roleName("ADMIN").build();
        User admin = User.builder().userId(1).email("admin@test.com").role(adminRole).build();
        adminPrincipal = new UserPrincipal(admin);

        Role ownerRole = Role.builder().roleId(2).roleName("OWNER").build();
        User owner = User.builder().userId(2).email("owner@test.com").role(ownerRole).build();
        ownerPrincipal = new UserPrincipal(owner);

        Role customerRole = Role.builder().roleId(3).roleName("CUSTOMER").build();
        User other = User.builder().userId(3).email("other@test.com").role(customerRole).build();
        otherPrincipal = new UserPrincipal(other);
    }

    @Test
    void getDocument_NoOwner_PublicAccess() {
        // Arrange
        String fileName = "temp-license.jpg";
        Resource mockResource = new ByteArrayResource("test-data".getBytes());
        Path mockPath = Paths.get("uploads/documents/" + fileName);

        when(ownerRepository.findByBusinessLicenseUrlContainingOrIdentityCardUrlContaining(fileName, fileName))
                .thenReturn(Optional.empty());
        when(fileStorageService.loadDocumentAsResource(fileName)).thenReturn(mockResource);
        when(fileStorageService.getDocumentDirectory()).thenReturn(mockPath.getParent());

        // Act
        ResponseEntity<Resource> response = fileController.getDocument(null, fileName);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResource, response.getBody());
    }

    @Test
    void getDocument_WithOwner_Admin_Allowed() {
        // Arrange
        String fileName = "owner-license.jpg";
        User ownerUser = User.builder().userId(2).build();
        Owner owner = Owner.builder().ownerId(10).user(ownerUser).build();
        Resource mockResource = new ByteArrayResource("test-data".getBytes());
        Path mockPath = Paths.get("uploads/documents/" + fileName);

        when(ownerRepository.findByBusinessLicenseUrlContainingOrIdentityCardUrlContaining(fileName, fileName))
                .thenReturn(Optional.of(owner));
        when(fileStorageService.loadDocumentAsResource(fileName)).thenReturn(mockResource);
        when(fileStorageService.getDocumentDirectory()).thenReturn(mockPath.getParent());

        // Act
        ResponseEntity<Resource> response = fileController.getDocument(adminPrincipal, fileName);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getDocument_WithOwner_OwnerSelf_Allowed() {
        // Arrange
        String fileName = "owner-license.jpg";
        User ownerUser = User.builder().userId(2).build(); // Match ownerPrincipal ID 2
        Owner owner = Owner.builder().ownerId(10).user(ownerUser).build();
        Resource mockResource = new ByteArrayResource("test-data".getBytes());
        Path mockPath = Paths.get("uploads/documents/" + fileName);

        when(ownerRepository.findByBusinessLicenseUrlContainingOrIdentityCardUrlContaining(fileName, fileName))
                .thenReturn(Optional.of(owner));
        when(fileStorageService.loadDocumentAsResource(fileName)).thenReturn(mockResource);
        when(fileStorageService.getDocumentDirectory()).thenReturn(mockPath.getParent());

        // Act
        ResponseEntity<Resource> response = fileController.getDocument(ownerPrincipal, fileName);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getDocument_WithOwner_Guest_Unauthorized() {
        // Arrange
        String fileName = "owner-license.jpg";
        User ownerUser = User.builder().userId(2).build();
        Owner owner = Owner.builder().ownerId(10).user(ownerUser).build();

        when(ownerRepository.findByBusinessLicenseUrlContainingOrIdentityCardUrlContaining(fileName, fileName))
                .thenReturn(Optional.of(owner));

        // Act
        ResponseEntity<Resource> response = fileController.getDocument(null, fileName);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getDocument_WithOwner_OtherUser_Forbidden() {
        // Arrange
        String fileName = "owner-license.jpg";
        User ownerUser = User.builder().userId(2).build();
        Owner owner = Owner.builder().ownerId(10).user(ownerUser).build();

        when(ownerRepository.findByBusinessLicenseUrlContainingOrIdentityCardUrlContaining(fileName, fileName))
                .thenReturn(Optional.of(owner));

        // Act
        ResponseEntity<Resource> response = fileController.getDocument(otherPrincipal, fileName);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
