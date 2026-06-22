package com.sportvenue.service;

import com.sportvenue.dto.request.CreateSportTypeRequest;
import com.sportvenue.dto.response.SportTypeResponse;
import com.sportvenue.entity.SportType;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.mapper.SportTypeMapper;
import com.sportvenue.repository.MatchRequestRepository;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
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
    private final StadiumRepository stadiumRepository;
    private final MatchRequestRepository matchRequestRepository;

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
                .sportCode(request.getSportCode())
                .description(request.getDescription())
                .isActive(request.getIsActive())
                .isFootballType(Boolean.TRUE.equals(request.getIsFootballType()))
                .build();

        SportType saved = sportTypeRepository.save(sportType);
        return sportTypeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SportTypeResponse updateSportType(Integer id, CreateSportTypeRequest request) {
        log.info("Updating sport type ID: {} with name: {}", id, request.getSportName());

        SportType sportType = sportTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại môn thể thao với ID: " + id));

        if (!sportType.getSportName().equals(request.getSportName())
                && sportTypeRepository.existsBySportName(request.getSportName())) {
            throw new BadRequestException("Tên loại môn thể thao đã tồn tại: " + request.getSportName());
        }

        if (!sportType.getSportCode().equals(request.getSportCode())
                && sportTypeRepository.existsBySportCode(request.getSportCode())) {
            throw new BadRequestException("Mã môn thể thao đã tồn tại: " + request.getSportCode());
        }

        sportType.setSportName(request.getSportName());
        sportType.setNameEn(request.getNameEn());
        sportType.setSportCode(request.getSportCode());
        sportType.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            sportType.setIsActive(request.getIsActive());
        }
        if (request.getIsFootballType() != null) {
            sportType.setIsFootballType(request.getIsFootballType());
        }

        return sportTypeMapper.toResponse(sportTypeRepository.save(sportType));
    }

    @Override
    @Transactional
    public void deleteSportType(Integer id) {
        log.info("Request to delete/deactivate sport type with ID: {}", id);
        SportType sportType = sportTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại môn thể thao với ID: " + id));

        boolean isUsedInStadium = stadiumRepository.existsBySportTypeSportTypeId(id);
        boolean isUsedInMatchRequest = matchRequestRepository.existsBySportTypeSportTypeId(id);

        if (isUsedInStadium || isUsedInMatchRequest) {
            log.info("Sport type ID {} is in use. Deactivating instead of hard delete.", id);
            sportType.setIsActive(false);
            sportTypeRepository.save(sportType);
        } else {
            log.info("Sport type ID {} is not in use. Performing hard delete.", id);
            sportTypeRepository.delete(sportType);
        }
    }
}
