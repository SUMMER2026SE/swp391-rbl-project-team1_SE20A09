package com.sportvenue.service;

import com.sportvenue.dto.request.CreateSportTypeRequest;
import com.sportvenue.dto.response.SportTypeResponse;
import java.util.List;

public interface SportTypeService {
    List<SportTypeResponse> getAllSportTypes();

    SportTypeResponse createSportType(CreateSportTypeRequest request);

    void deleteSportType(Integer id);
}
