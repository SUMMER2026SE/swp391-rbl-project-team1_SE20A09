package com.sportvenue.repository;

import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.enums.StadiumStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repository cho Stadium entity.
 * Stub — các thành viên (đặc biệt Huy, An, Hào) mở rộng thêm query khi cần.
 */
@Repository
public interface StadiumRepository extends JpaRepository<Stadium, Integer>, JpaSpecificationExecutor<Stadium> {

    /** Lấy danh sách sân theo chủ sân — dùng cho trang quản lý của Owner. */
    @EntityGraph(attributePaths = {"sportType", "images"})
    List<Stadium> findByOwnerOwnerIdAndStadiumStatusNot(Integer ownerId, StadiumStatus status);

    /** Lấy tất cả sân đang hoạt động — dùng cho trang chủ Guest. */
    @EntityGraph(attributePaths = {"sportType", "images", "owner"})
    Page<Stadium> findByStadiumStatus(StadiumStatus status, Pageable pageable);

    /** Tìm kiếm sân theo tên hoặc địa chỉ — dùng cho Search Venue. */
    @Query("""
            SELECT s FROM Stadium s
            WHERE s.stadiumStatus = 'AVAILABLE'
            AND (LOWER(s.stadiumName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(s.address) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Stadium> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /** Lấy sân theo sport type — dùng cho Filter Venue. */
    Page<Stadium> findBySportTypeSportTypeIdAndStadiumStatus(
            Integer sportTypeId, StadiumStatus status, Pageable pageable);

    /** Đếm sân theo owner — dùng cho Dashboard. */
    long countByOwnerOwnerIdAndStadiumStatus(Integer ownerId, StadiumStatus status);

    @EntityGraph(attributePaths = {"sportType", "images", "owner", "accessories"})
    Optional<Stadium> findWithDetailsByStadiumId(Integer stadiumId);

    @EntityGraph(attributePaths = {"sportType", "images"})
    @Query("""
            SELECT s FROM Stadium s
            WHERE s.stadiumStatus = com.sportvenue.entity.enums.StadiumStatus.AVAILABLE
            AND s.stadiumId NOT IN :excludeIds
            ORDER BY s.averageRating DESC, s.stadiumName ASC
            """)
    List<Stadium> findRecommendedExcluding(
            @Param("excludeIds") List<Integer> excludeIds,
            Pageable pageable);
}
