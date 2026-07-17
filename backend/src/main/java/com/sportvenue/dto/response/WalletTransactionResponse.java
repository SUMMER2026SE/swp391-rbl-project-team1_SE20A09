package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransactionResponse {
    private Integer transactionId;
    private BigDecimal amount;
    private Integer bookingId;
    private String note;
    private String transactionType;
    private LocalDateTime createdAt;
}
