package com.sportvenue.controller;

import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.WalletBalanceResponse;
import com.sportvenue.dto.response.WalletTransactionResponse;
import com.sportvenue.security.RequireApprovedOwner;
import com.sportvenue.security.UserPrincipal;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller quản lý Ví và Giao dịch dành cho Owner.
 */
@RestController
@RequestMapping("/api/v1/owner/wallet")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Owner — Wallet Management", description = "Quản lý ví và lịch sử giao dịch của chủ sân")
@RequireApprovedOwner
@PreAuthorize("hasRole('Owner')")
public class OwnerWalletController {

    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "Xem số dư ví Owner", description = "Lấy số dư hiện tại trong ví nội bộ của chủ sân.")
    public ResponseEntity<WalletBalanceResponse> getBalance(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("Owner ID {} requested wallet balance", principal.getUserId());
        return ResponseEntity.ok(walletService.getOwnerWalletBalance(principal.getUserId()));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Xem danh sách giao dịch ví Owner", description = "Lấy lịch sử biến động số dư ví nội bộ của chủ sân (phân trang).")
    public ResponseEntity<PageResponse<WalletTransactionResponse>> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Owner ID {} requested wallet transaction history", principal.getUserId());
        return ResponseEntity.ok(walletService.getOwnerWalletTransactions(principal.getUserId(), pageable));
    }

    @GetMapping("/transactions/booking/{bookingId}")
    @Operation(summary = "Xem lịch sử giao dịch ví theo Booking", description = "Lấy lịch sử giao dịch ví liên quan đến một Booking cụ thể (dành cho đối soát timeline).")
    public ResponseEntity<List<WalletTransactionResponse>> getTransactionsByBooking(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Integer bookingId) {
        log.info("Owner ID {} requested transaction timeline for booking {}", principal.getUserId(), bookingId);
        return ResponseEntity.ok(walletService.getWalletTransactionsByBooking(principal.getUserId(), bookingId));
    }
}
