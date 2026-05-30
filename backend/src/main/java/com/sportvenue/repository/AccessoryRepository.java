package com.sportvenue.repository;

import com.sportvenue.entity.Accessory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho Accessory entity.
 * Stub — Lượng mở rộng thêm khi implement UC-OWN-11.
 */
@Repository
public interface AccessoryRepository extends JpaRepository<Accessory, Integer> {

    /** Lấy tất cả phụ kiện đang cho thuê của một sân. */
    List<Accessory> findByStadiumStadiumIdAndIsAvailableTrue(Integer stadiumId);

    /** Lấy tất cả phụ kiện của một sân (kể cả ngưng cho thuê) — dùng cho Owner quản lý. */
    List<Accessory> findByStadiumStadiumId(Integer stadiumId);
}
