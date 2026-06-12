package com.sportvenue.service;

import com.sportvenue.dto.request.CreateStadiumRequest;
import com.sportvenue.dto.response.StadiumResponse;

public interface StadiumService {
    StadiumResponse createStadium(CreateStadiumRequest request, Integer userId);

    java.util.List<StadiumResponse> getMyStadiums(Integer userId);

    void suspendStadium(Integer stadiumId, Integer userId);

    void activateStadium(Integer stadiumId, Integer userId);

    void deleteStadium(Integer stadiumId, Integer userId);
}
