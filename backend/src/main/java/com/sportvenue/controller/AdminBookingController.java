package com.sportvenue.controller;

import com.sportvenue.dto.request.AdminBookingSearchRequest;
import com.sportvenue.dto.response.AdminBookingListResponse;
import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.service.AdminBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin Booking Management", description = "Quản lý danh sách đặt sân của hệ thống — chỉ dành cho Admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin')")
@Slf4j
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    @GetMapping
    @Operation(
        summary = "Xem danh sách và thống kê booking toàn hệ thống",
        description = "Admin xem danh sách booking có phân trang, tìm kiếm và lọc nâng cao. Đồng thời trả về 3 thẻ thống kê ở đầu trang."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Lấy danh sách thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "Không có quyền — yêu cầu role Admin")
    })
    public ResponseEntity<ApiResponse<AdminBookingListResponse>> getBookings(
            @Parameter(description = "Số trang (0-indexed)", example = "0")
            @Min(value = 0, message = "Số trang (page) không được nhỏ hơn 0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số bản ghi mỗi trang", example = "10")
            @Min(value = 1, message = "Số bản ghi mỗi trang (pageSize) phải lớn hơn 0")
            @RequestParam(defaultValue = "10") int pageSize,

            @ParameterObject AdminBookingSearchRequest filter
    ) {
        List<String> allowedSortBy = List.of(
                "bookingId", "totalPrice", "serviceFee",
                "bookingStatus", "paymentStatus", "bookingDate", "reservationDate"
        );
        String sortBy = allowedSortBy.contains(filter.getSortBy()) ? filter.getSortBy() : "bookingDate";

        Sort sort = "asc".equalsIgnoreCase(filter.getSortDir())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        AdminBookingListResponse result = adminBookingService.getAdminBookings(
                filter.getSearch(), filter.getBookingStatus(), filter.getPaymentStatus(),
                filter.getStartDate(), filter.getEndDate(), filter.getStadiumId(), filter.getOwnerId(),
                pageable);

        return ResponseEntity.ok(ApiResponse.<AdminBookingListResponse>builder()
                .code(200)
                .message("Lấy danh sách và thống kê booking thành công")
                .result(result)
                .build());
    }
}
