package com.sportvenue.service;

import com.sportvenue.dto.request.VenueReviewRequest;
import com.sportvenue.dto.response.VenueReviewResponse;

public interface VenueReviewService {
    VenueReviewResponse createReview(Integer venueId, VenueReviewRequest request, String username);
}
