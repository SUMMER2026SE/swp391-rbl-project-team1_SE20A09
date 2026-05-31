package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateAccessoryRequest;
import com.sportvenue.dto.response.AccessoryResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.AccessoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stadiums")
@RequiredArgsConstructor
@Tag(name = "Accessory", description = "Quản lý phụ kiện cho thuê của sân (Owner)")
public class AccessoryController {

    private final AccessoryService accessoryService;

    @PostMapping("/{stadiumId}/accessories")
    @PreAuthorize("hasRole('Owner')")
    @Operation(
            summary = "Thêm phụ kiện cho thuê kèm sân",
            description = "Yêu cầu tài khoản đăng nhập có vai trò Owner và sở hữu đúng sân này. Cần gửi Bearer JWT Token ở Header"
    )
    public ResponseEntity<AccessoryResponse> addAccessory(
            @PathVariable Integer stadiumId,
            @Valid @RequestBody CreateAccessoryRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        AccessoryResponse response = accessoryService.addAccessory(stadiumId, request, userPrincipal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @org.springframework.web.bind.annotation.GetMapping("/{stadiumId}/accessories")
    @PreAuthorize("hasRole('Owner')")
    @Operation(
            summary = "Lấy danh sách phụ kiện cho thuê của sân",
            description = "Yêu cầu tài khoản đăng nhập có vai trò Owner và sở hữu đúng sân này. Trả về tất cả phụ kiện kèm sân."
    )
    public ResponseEntity<java.util.List<AccessoryResponse>> getAccessories(
            @PathVariable Integer stadiumId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        java.util.List<AccessoryResponse> response = accessoryService.getAccessoriesByStadium(stadiumId, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }
}
