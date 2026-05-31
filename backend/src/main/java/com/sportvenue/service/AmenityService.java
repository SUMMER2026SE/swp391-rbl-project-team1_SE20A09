package com.sportvenue.service;

import com.sportvenue.dto.response.AmenityResponse;

import java.util.List;

public interface AmenityService {
    List<AmenityResponse> getAllAmenities();
}
