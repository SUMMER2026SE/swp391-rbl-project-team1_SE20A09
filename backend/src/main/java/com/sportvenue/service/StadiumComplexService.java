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
}
