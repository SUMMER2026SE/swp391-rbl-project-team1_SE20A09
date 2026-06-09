package com.sportvenue.service;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.StadiumDetailResponse;
import com.sportvenue.dto.response.StadiumResponse;

public interface PublicStadiumService {
    PageResponse<StadiumResponse> searchStadiums(StadiumSearchRequest request);
    StadiumDetailResponse getStadiumDetail(Integer stadiumId);
    PageResponse<StadiumDetailResponse.ReviewDto> getStadiumReviews(Integer stadiumId, int page, int size);
}
