package com.sportvenue.repository;

import com.sportvenue.entity.StadiumImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho StadiumImage entity.
 * Stub — mở rộng thêm khi cần.
 */
@Repository
public interface StadiumImageRepository extends JpaRepository<StadiumImage, Integer> {

    /** Lấy tất cả ảnh của một sân theo thứ tự upload. */
    List<StadiumImage> findByStadiumStadiumIdOrderByUploadedAtAsc(Integer stadiumId);

    /** Xóa tất cả ảnh của một sân — dùng khi Owner upload lại toàn bộ ảnh. */
    void deleteByStadiumStadiumId(Integer stadiumId);
}
