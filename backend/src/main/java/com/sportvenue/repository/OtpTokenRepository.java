package com.sportvenue.repository;

import com.sportvenue.entity.OtpToken;
import com.sportvenue.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Repository cho bảng otp_tokens.
 */
@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Integer> {

    Optional<OtpToken> findByUser(User user);

    /** Xóa tất cả OTP cũ của user trước khi tạo OTP mới. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM OtpToken o WHERE o.user = :user")
    void deleteAllByUser(User user);

    /** Dọn dẹp mã OTP đã hết hạn. */
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpToken o WHERE o.expiresAt < :now")
    void deleteExpiredTokens(java.time.LocalDateTime now);
}
