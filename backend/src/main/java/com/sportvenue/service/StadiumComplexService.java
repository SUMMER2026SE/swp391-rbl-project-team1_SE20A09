package com.sportvenue.service;

import com.sportvenue.dto.request.CreateComplexRequest;
import com.sportvenue.dto.response.ComplexResponse;

import java.util.List;

public interface StadiumComplexService {

    ComplexResponse createComplex(CreateComplexRequest request, Integer userId);

    List<ComplexResponse> getMyComplexes(Integer userId);

    ComplexResponse getComplexById(Integer complexId);

    ComplexResponse getComplexByIdAndOwner(Integer complexId, Integer userId);

    List<ComplexResponse> getAllComplexes(String approvedStatus);

    ComplexResponse approveComplex(Integer complexId);

    ComplexResponse rejectComplex(Integer complexId, String reason);

    ComplexResponse updateComplex(Integer complexId, CreateComplexRequest request, Integer userId);

    /** Bảo trì vô thời hạn — set complexStatus = MAINTENANCE, cascade áp dụng qua {@code isStadiumUnderMaintenance}. */
    void suspendComplex(Integer complexId, Integer userId);

    void activateComplex(Integer complexId, Integer userId);
}
