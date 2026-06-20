package com.sportvenue.repository;

import com.sportvenue.entity.VenueReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueReviewRepository extends JpaRepository<VenueReview, Integer> {
    
    boolean existsByCustomer_UserIdAndVenue_StadiumId(Integer customerId, Integer venueId);
}
