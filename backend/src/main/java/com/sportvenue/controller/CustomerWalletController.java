package com.sportvenue.controller;

import com.sportvenue.dto.request.WalletTopupRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.WalletBalanceResponse;
import com.sportvenue.dto.response.WalletTopupResponse;
import com.sportvenue.dto.response.WalletTransactionResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.WalletService;
import com.sportvenue.service.WalletTopupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller quản lý Ví nội bộ dành cho Customer — xem số dư, lịch sử giao dịch, nạp tiền qua VNPay.
 */
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer — Wallet Management", description = "Quản lý ví nội bộ và nạp tiền của Customer")
@PreAuthorize("hasRole('Customer')")
public class CustomerWalletController {

    private final WalletService walletService;
    private final WalletTopupService walletTopupService;

    @GetMapping
    @Operation(summary = "Xem số dư ví", description = "Lấy số dư hiện tại trong ví nội bộ của Customer.")
    public ResponseEntity<WalletBalanceResponse> getBalance(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("Customer ID {} requested wallet balance", principal.getUserId());
        return ResponseEntity.ok(walletService.getCustomerWalletBalance(principal.getUserId()));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Xem danh sách giao dịch ví", description = "Lấy lịch sử biến động số dư ví nội bộ của Customer (phân trang).")
    public ResponseEntity<PageResponse<WalletTransactionResponse>> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Customer ID {} requested wallet transaction history", principal.getUserId());
        return ResponseEntity.ok(walletService.getCustomerWalletTransactions(principal.getUserId(), pageable));
    }

    @PostMapping("/topup")
    @Operation(summary = "Nạp tiền vào ví", description = "Sinh URL thanh toán VNPay để nạp tiền vào ví nội bộ, không gắn với booking nào.")
    public ResponseEntity<WalletTopupResponse> topup(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody WalletTopupRequest request,
            HttpServletRequest httpRequest) {
        log.info("Customer ID {} requested wallet topup, amount={}", principal.getUserId(), request.getAmount());
        return ResponseEntity.ok(walletTopupService.initiateTopup(principal, request, httpRequest));
    }
}
