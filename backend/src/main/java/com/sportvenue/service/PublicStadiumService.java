package com.sportvenue.service;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.StadiumResponse;

public interface PublicStadiumService {
    PageResponse<StadiumResponse> searchStadiums(StadiumSearchRequest request);
}
