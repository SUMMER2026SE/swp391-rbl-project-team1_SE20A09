package com.sportvenue.service;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.StadiumSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StadiumService {
    Page<StadiumSearchResponse> searchStadiums(StadiumSearchRequest request, Pageable pageable);
}
