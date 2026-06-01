package com.sportvenue.service;

import com.sportvenue.dto.response.SportTypeResponse;
import java.util.List;

public interface SportTypeService {
    List<SportTypeResponse> getAllSportTypes();
}
