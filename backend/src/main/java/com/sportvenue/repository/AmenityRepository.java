package com.sportvenue.repository;

import com.sportvenue.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Integer> {
    Optional<Amenity> findByName(String name);
}
