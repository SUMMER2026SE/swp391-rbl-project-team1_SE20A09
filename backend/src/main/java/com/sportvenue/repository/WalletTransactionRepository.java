package com.sportvenue.repository;

import com.sportvenue.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý giao dịch ví WalletTransaction.
 */
@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Integer> {

    Page<WalletTransaction> findByWalletWalletIdOrderByCreatedAtDesc(Integer walletId, Pageable pageable);

    List<WalletTransaction> findByBookingBookingIdOrderByCreatedAtAsc(Integer bookingId);
}
