package com.sportvenue.repository;

import com.sportvenue.entity.WalletTopup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository quản lý yêu cầu nạp tiền vào ví Customer (WalletTopup).
 */
@Repository
public interface WalletTopupRepository extends JpaRepository<WalletTopup, Integer> {

    Optional<WalletTopup> findByTransactionCode(String transactionCode);

    /**
     * Lock row để xử lý idempotent callback VNPay — Return và IPN có thể tới song song.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM WalletTopup t WHERE t.transactionCode = :txnRef")
    Optional<WalletTopup> findByTransactionCodeForUpdate(@Param("txnRef") String txnRef);
}
