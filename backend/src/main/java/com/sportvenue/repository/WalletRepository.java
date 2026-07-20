package com.sportvenue.repository;

import com.sportvenue.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository quản lý ví nội bộ Wallet.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {

    Optional<Wallet> findByOwnerOwnerId(Integer ownerId);

    Optional<Wallet> findByUserUserId(Integer userId);

    Optional<Wallet> findByIsPlatformTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.owner.ownerId = :ownerId")
    Optional<Wallet> findByOwnerIdForUpdate(@Param("ownerId") Integer ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.user.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Integer userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.isPlatform = true")
    Optional<Wallet> findPlatformWalletForUpdate();
}
