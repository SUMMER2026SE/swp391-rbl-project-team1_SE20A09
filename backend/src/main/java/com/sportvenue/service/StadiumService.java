package com.sportvenue.service;

import com.sportvenue.dto.request.CreateStadiumRequest;
import com.sportvenue.dto.request.UpdateStadiumRequest;
import com.sportvenue.dto.response.StadiumResponse;

public interface StadiumService {
    StadiumResponse createStadium(CreateStadiumRequest request, Integer userId);

    java.util.List<StadiumResponse> getMyStadiums(Integer userId, String search, Integer sportTypeId, String status);

    StadiumResponse getStadiumByIdAndOwner(Integer stadiumId, Integer userId);

    StadiumResponse updateStadium(Integer stadiumId, UpdateStadiumRequest request, Integer userId);

    StadiumResponse approveStadium(Integer stadiumId);

    StadiumResponse rejectStadium(Integer stadiumId);

    java.util.List<StadiumResponse> getAllStadiums(String approvedStatus);
}
