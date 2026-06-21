package com.sportvenue.controller;

import com.sportvenue.dto.response.AdminOwnerResponse;
import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.service.AdminOwnerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOwnerControllerTest {

    @Mock
    private AdminOwnerService adminOwnerService;

    @InjectMocks
    private AdminOwnerController adminOwnerController;

    @Test
    void getOwners_Success() {
        // Arrange
        int page = 1;
        int size = 10;
        String search = "test";
        String accountStatus = "ACTIVE";
        String approvedStatus = "APPROVED";
        String sortBy = "createdAt";
        String sortDir = "desc";

        AdminOwnerResponse owner = new AdminOwnerResponse();
        PageResponse<AdminOwnerResponse> pageResponse = new PageResponse<>();
        pageResponse.setContent(List.of(owner));
        pageResponse.setTotalElements(1);
        
        when(adminOwnerService.getOwners(eq(search), eq(accountStatus), eq(approvedStatus), eq(page), eq(size), eq(sortBy), eq(sortDir)))
                .thenReturn(pageResponse);

        // Act
        ResponseEntity<ApiResponse<PageResponse<AdminOwnerResponse>>> result = 
                adminOwnerController.getOwners(page, size, search, accountStatus, approvedStatus, sortBy, sortDir);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(200, result.getBody().getCode());
        assertEquals(1, result.getBody().getResult().getTotalElements());
        verify(adminOwnerService).getOwners(eq(search), eq(accountStatus), eq(approvedStatus), eq(page), eq(size), eq(sortBy), eq(sortDir));
    }

    @Test
    void getOwners_InvalidSortBy_UsesFallback() {
        // Arrange
        String invalidSortBy = "invalidField";
        PageResponse<AdminOwnerResponse> pageResponse = new PageResponse<>();
        
        when(adminOwnerService.getOwners(isNull(), isNull(), isNull(), eq(1), eq(10), eq("createdAt"), eq("desc")))
                .thenReturn(pageResponse);

        // Act
        ResponseEntity<ApiResponse<PageResponse<AdminOwnerResponse>>> result = 
                adminOwnerController.getOwners(1, 10, null, null, null, invalidSortBy, "desc");

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(adminOwnerService).getOwners(isNull(), isNull(), isNull(), eq(1), eq(10), eq("createdAt"), eq("desc"));
    }
}
