package com.sportvenue.service;

import com.sportvenue.dto.response.SportTypeResponse;
import com.sportvenue.mapper.SportTypeMapper;
import com.sportvenue.repository.SportTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SportTypeServiceImpl implements SportTypeService {

    private final SportTypeRepository sportTypeRepository;
    private final SportTypeMapper sportTypeMapper;

    @Override
    public List<SportTypeResponse> getAllSportTypes() {
        return sportTypeMapper.toResponseList(sportTypeRepository.findAll());
    }
}
