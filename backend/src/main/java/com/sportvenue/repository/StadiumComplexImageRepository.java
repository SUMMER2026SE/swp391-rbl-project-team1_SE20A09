package com.sportvenue.repository;

import com.sportvenue.entity.StadiumComplexImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StadiumComplexImageRepository extends JpaRepository<StadiumComplexImage, Integer> {
    List<StadiumComplexImage> findByComplexComplexId(Integer complexId);
}
