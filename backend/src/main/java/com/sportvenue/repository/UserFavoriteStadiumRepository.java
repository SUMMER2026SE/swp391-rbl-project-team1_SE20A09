package com.sportvenue.repository;

import com.sportvenue.entity.UserFavoriteStadium;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFavoriteStadiumRepository extends JpaRepository<UserFavoriteStadium, Long> {

    @EntityGraph(attributePaths = {"stadium", "stadium.sportType", "stadium.images", "user"})
    List<UserFavoriteStadium> findByUserUserIdOrderByCreatedAtDesc(Integer userId);

    long countByUserUserId(Integer userId);

    boolean existsByUserUserIdAndStadiumStadiumId(Integer userId, Integer stadiumId);
}
