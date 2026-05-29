package com.sportvenue.repository;

import com.sportvenue.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho Review entity.
 * Stub — Hoàng mở rộng thêm khi implement UC-OWN-08.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    /** Kiểm tra booking đã được review chưa — ngăn review 2 lần. */
    boolean existsByBookingBookingId(Integer bookingId);

    /** Lấy danh sách review của một sân — dùng cho trang chi tiết sân. */
    @EntityGraph(attributePaths = {"user"})
    Page<Review> findByStadiumStadiumIdOrderByCreatedAtDesc(Integer stadiumId, Pageable pageable);

    /** Tính điểm trung bình của sân — gọi sau khi có review mới để cập nhật Stadium. */
    @Query("SELECT AVG(r.ratingScore) FROM Review r WHERE r.stadium.stadiumId = :stadiumId")
    Optional<Double> calculateAverageRating(@Param("stadiumId") Integer stadiumId);
}
