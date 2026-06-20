package com.sportvenue.controller;

import com.sportvenue.dto.response.AdminCustomerResponse;
import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        User adminUser = User.builder().userId(1).email("admin@test.com").build();
        adminPrincipal = new UserPrincipal(adminUser);
    }

    @Test
    void getCustomers_Success() {
        // Arrange
        int page = 0;
        int pageSize = 10;
        String search = "test";
        AccountStatus status = AccountStatus.ACTIVE;
        String sortBy = "createdAt";
        String sortDir = "desc";

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(sortBy).descending());
        
        AdminCustomerResponse customer = AdminCustomerResponse.builder()
                .userId(2)
                .email("customer@test.com")
                .build();
                
        PageResponse<AdminCustomerResponse> pageResponse = new PageResponse<>();
        pageResponse.setContent(List.of(customer));
        pageResponse.setTotalElements(1);
        
        when(adminUserService.getCustomers(eq(search), eq(status), eq(pageable))).thenReturn(pageResponse);

        // Act
        ResponseEntity<ApiResponse<PageResponse<AdminCustomerResponse>>> result = 
                adminUserController.getCustomers(page, pageSize, search, status, sortBy, sortDir);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(200, result.getBody().getCode());
        assertEquals(1, result.getBody().getResult().getTotalElements());
        verify(adminUserService).getCustomers(eq(search), eq(status), eq(pageable));
    }

    @Test
    void getCustomers_InvalidSortBy_UsesFallback() {
        // Arrange
        String invalidSortBy = "invalidField";
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10, 
                org.springframework.data.domain.Sort.by("createdAt").descending());
        when(adminUserService.getCustomers(eq(null), eq(null), eq(pageable))).thenReturn(new PageResponse<>());

        // Act
        ResponseEntity<ApiResponse<PageResponse<AdminCustomerResponse>>> result = 
                adminUserController.getCustomers(0, 10, null, null, invalidSortBy, "desc");

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(adminUserService).getCustomers(eq(null), eq(null), eq(pageable));
    }

    @Test
    void lockUnlockCustomer_ReturnsOk() {
        // Arrange
        com.sportvenue.dto.request.LockCustomerRequest request = new com.sportvenue.dto.request.LockCustomerRequest(false);
        com.sportvenue.entity.User adminUser = com.sportvenue.entity.User.builder().userId(1).build();
        com.sportvenue.security.UserPrincipal principal = new com.sportvenue.security.UserPrincipal(adminUser);

        // Act
        ResponseEntity<ApiResponse<Void>> result = adminUserController.lockUnlockCustomer(2, request, principal);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Đã khóa tài khoản thành công", result.getBody().getMessage());
        verify(adminUserService).lockUnlockCustomer(2, false, 1);
    }
}
