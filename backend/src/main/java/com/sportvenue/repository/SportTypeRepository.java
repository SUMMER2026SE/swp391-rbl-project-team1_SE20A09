package com.sportvenue.repository;

import com.sportvenue.entity.SportType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho SportType entity.
 * Stub — các thành viên mở rộng thêm query khi cần.
 */
@Repository
public interface SportTypeRepository extends JpaRepository<SportType, Integer> {

    /** Bảng nhỏ, gần như không đổi — cache để tránh SELECT * lặp lại mỗi lần AI tool resolve sportName. */
    @Cacheable(value = "sportTypes")
    @Override
    List<SportType> findAll();

    Optional<SportType> findBySportName(String sportName);

    boolean existsBySportName(String sportName);

    boolean existsBySportCode(String sportCode);
}
