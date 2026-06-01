package com.sportvenue.service;

import com.sportvenue.dto.request.CreateStadiumRequest;
import com.sportvenue.dto.response.StadiumResponse;

public interface StadiumService {
    StadiumResponse createStadium(CreateStadiumRequest request, Integer userId);

    java.util.List<StadiumResponse> getMyStadiums(Integer userId);
}
