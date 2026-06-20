package com.sportvenue.service.impl;

import com.sportvenue.dto.response.AdminOwnerResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.repository.OwnerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOwnerServiceImplTest {

    @Mock
    private OwnerRepository ownerRepository;

    @InjectMocks
    private AdminOwnerServiceImpl adminOwnerService;

    @Test
    void getOwners_Success() {
        // Arrange
        String search = "test";
        String accountStatusStr = "ACTIVE";
        String approvedStatusStr = "APPROVED";
        int page = 1;
        int size = 10;
        String sortBy = "businessName";
        String sortDir = "asc";

        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.ASC, "businessName"));
        
        AdminOwnerResponse responseDto = new AdminOwnerResponse();
        Page<AdminOwnerResponse> ownerPage = new PageImpl<>(List.of(responseDto), pageable, 1);
        
        when(ownerRepository.findOwnersForAdmin(eq(search), eq(AccountStatus.ACTIVE), eq(ApprovedStatus.APPROVED), eq(pageable)))
                .thenReturn(ownerPage);

        // Act
        PageResponse<AdminOwnerResponse> result = adminOwnerService.getOwners(search, accountStatusStr, approvedStatusStr, page, size, sortBy, sortDir);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(ownerRepository).findOwnersForAdmin(eq(search), eq(AccountStatus.ACTIVE), eq(ApprovedStatus.APPROVED), eq(pageable));
    }

    @Test
    void getOwners_InvalidStatus_HandledGracefully() {
        // Arrange
        String search = null;
        String accountStatusStr = "INVALID_STATUS";
        String approvedStatusStr = "INVALID_STATUS";
        int page = 1;
        int size = 10;
        String sortBy = "createdAt";
        String sortDir = "desc";

        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<AdminOwnerResponse> ownerPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(ownerRepository.findOwnersForAdmin(isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(ownerPage);

        // Act
        PageResponse<AdminOwnerResponse> result = adminOwnerService.getOwners(search, accountStatusStr, approvedStatusStr, page, size, sortBy, sortDir);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(ownerRepository).findOwnersForAdmin(isNull(), isNull(), isNull(), eq(pageable));
    }
}
