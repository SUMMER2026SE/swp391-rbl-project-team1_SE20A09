package com.sportvenue.controller;

import com.sportvenue.dto.request.AdminSuspendStadiumRequest;
import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.service.StadiumService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminStadiumControllerTest {

    @Mock
    private StadiumService stadiumService;

    @InjectMocks
    private AdminStadiumController adminStadiumController;

    @Test
    void suspendStadiumDelegatesToService() {
        AdminSuspendStadiumRequest request = AdminSuspendStadiumRequest.builder()
                .reason("Fake listing report")
                .build();

        ResponseEntity<ApiResponse<Void>> result = adminStadiumController.suspendStadium(10, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(200, result.getBody().getCode());
        assertEquals("Stadium suspended successfully", result.getBody().getMessage());
        verify(stadiumService).suspendStadiumByAdmin(10, "Fake listing report");
    }

    @Test
    void suspendStadiumAcceptsEmptyBody() {
        ResponseEntity<ApiResponse<Void>> result = adminStadiumController.suspendStadium(10, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(stadiumService).suspendStadiumByAdmin(10, null);
    }

    @Test
    void unsuspendStadiumDelegatesToService() {
        ResponseEntity<ApiResponse<Void>> result = adminStadiumController.unsuspendStadium(10);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(200, result.getBody().getCode());
        assertEquals("Stadium unsuspended successfully", result.getBody().getMessage());
        verify(stadiumService).unsuspendStadiumByAdmin(10);
    }
}
