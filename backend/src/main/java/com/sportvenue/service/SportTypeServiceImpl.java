package com.sportvenue.service;

import com.sportvenue.dto.request.CreateSportTypeRequest;
import com.sportvenue.dto.response.SportTypeResponse;
import com.sportvenue.entity.SportType;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.mapper.SportTypeMapper;
import com.sportvenue.repository.SportTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SportTypeServiceImpl implements SportTypeService {

    private final SportTypeRepository sportTypeRepository;
    private final SportTypeMapper sportTypeMapper;

    @Override
    public List<SportTypeResponse> getAllSportTypes() {
        return sportTypeMapper.toResponseList(sportTypeRepository.findAll());
    }

    @Override
    @Transactional
    public SportTypeResponse createSportType(CreateSportTypeRequest request) {
        log.info("Creating new sport type: {} with code: {}", request.getSportName(), request.getSportCode());

        if (sportTypeRepository.existsBySportName(request.getSportName())) {
            throw new BadRequestException("Tên loại môn thể thao đã tồn tại: " + request.getSportName());
        }

        if (sportTypeRepository.existsBySportCode(request.getSportCode())) {
            throw new BadRequestException("Mã môn thể thao đã tồn tại: " + request.getSportCode());
        }

        SportType sportType = SportType.builder()
                .sportName(request.getSportName())
                .nameEn(request.getNameEn())
                .icon(request.getIcon())
                .sportCode(request.getSportCode())
                .description(request.getDescription())
                .isActive(request.getIsActive())
                .build();

        SportType saved = sportTypeRepository.save(sportType);
        return sportTypeMapper.toResponse(saved);
    }
}
