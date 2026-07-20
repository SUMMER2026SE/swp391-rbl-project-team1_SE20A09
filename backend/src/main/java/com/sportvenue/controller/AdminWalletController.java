package com.sportvenue.controller;

import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.WalletBalanceResponse;
import com.sportvenue.dto.response.WalletTransactionResponse;
import com.sportvenue.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller quản lý Ví và Giao dịch hệ thống dành cho Admin.
 */
@RestController
@RequestMapping("/api/v1/admin/wallet")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin — Platform Wallet Management", description = "Quản lý ví và lịch sử phí dịch vụ hệ thống của Admin")
@PreAuthorize("hasRole('Admin')")
public class AdminWalletController {

    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "Xem số dư ví Platform", description = "Lấy số dư hiện tại trong ví phí dịch vụ của Platform.")
    public ResponseEntity<WalletBalanceResponse> getBalance() {
        log.info("Admin requested platform wallet balance");
        return ResponseEntity.ok(walletService.getPlatformWalletBalance());
    }

    @GetMapping("/transactions")
    @Operation(summary = "Xem danh sách giao dịch ví Platform", description = "Lấy lịch sử biến động số dư ví phí dịch vụ Platform (phân trang).")
    public ResponseEntity<PageResponse<WalletTransactionResponse>> getTransactions(
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Admin requested platform wallet transaction history");
        return ResponseEntity.ok(walletService.getPlatformWalletTransactions(pageable));
    }
}
