package com.sportvenue.service;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.StadiumSearchResponse;
import com.sportvenue.entity.Stadium;
import com.sportvenue.mapper.StadiumMapper;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.specification.StadiumSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StadiumServiceImpl implements StadiumService {

    private final StadiumRepository stadiumRepository;
    private final StadiumMapper stadiumMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<StadiumSearchResponse> searchStadiums(StadiumSearchRequest request, Pageable pageable) {
        Specification<Stadium> spec = StadiumSpecification.filterStadiums(request);
        Page<Stadium> stadiumPage = stadiumRepository.findAll(spec, pageable);
        return stadiumPage.map(stadiumMapper::toStadiumSearchResponse);
    }
}
