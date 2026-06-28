package com.sportvenue.service;

import com.sportvenue.dto.response.CourtResponse;
import com.sportvenue.dto.response.FacilityResponse;
import com.sportvenue.dto.response.PublicComplexDetailResponse;

import java.util.List;

public interface PublicComplexService {
    PublicComplexDetailResponse getPublicComplexById(Integer complexId);
    List<FacilityResponse> getFacilitiesByComplexId(Integer complexId);
    List<CourtResponse> getCourtsByFacilityId(Integer facilityId);
}
