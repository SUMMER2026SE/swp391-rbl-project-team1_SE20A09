package com.sportvenue.repository;

import com.sportvenue.entity.SportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SportTypeRepository extends JpaRepository<SportType, Integer> {
    Optional<SportType> findBySportName(String sportName);
}
