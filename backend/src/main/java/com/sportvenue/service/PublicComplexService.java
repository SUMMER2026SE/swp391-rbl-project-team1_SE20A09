package com.sportvenue.service;

import com.sportvenue.dto.request.StadiumComplexSearchRequest;
import com.sportvenue.dto.response.CourtResponse;
import com.sportvenue.dto.response.FacilityResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.PublicComplexDetailResponse;
import com.sportvenue.dto.response.StadiumDetailResponse;

import java.util.List;

public interface PublicComplexService {

    PublicComplexDetailResponse getPublicComplexById(Integer complexId);

    List<FacilityResponse> getFacilitiesByComplexId(Integer complexId);

    List<CourtResponse> getCourtsByFacilityId(Integer facilityId);

    PageResponse<PublicComplexDetailResponse> searchComplexes(StadiumComplexSearchRequest request);

    /** Gộp review từ tất cả COURT thuộc complex — dùng cho trang chi tiết tổ hợp. */
    PageResponse<StadiumDetailResponse.ReviewDto> getComplexReviews(Integer complexId, int page, int size);
}
