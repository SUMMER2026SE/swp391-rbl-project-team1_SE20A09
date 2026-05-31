package com.sportvenue.service;

import com.sportvenue.dto.request.CreateStadiumRequest;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.mapper.StadiumMapper;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class StadiumServiceImpl implements StadiumService {

    private final StadiumRepository stadiumRepository;
    private final OwnerRepository ownerRepository;
    private final SportTypeRepository sportTypeRepository;
    private final StadiumMapper stadiumMapper;

    @Override
    @Transactional
    public StadiumResponse createStadium(CreateStadiumRequest request, Integer userId) {
        log.info("Creating stadium: {} for user: {}", request.getStadiumName(), userId);

        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        if (owner.getApprovedStatus() != ApprovedStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Tài khoản chủ sân chưa được Admin phê duyệt. Vui lòng chờ xét duyệt.");
        }

        SportType sportType = sportTypeRepository.findById(request.getSportTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Sport type not found with ID: " + request.getSportTypeId()));

        Stadium stadium = stadiumMapper.toEntity(request);
        stadium.setOwner(owner);
        stadium.setSportType(sportType);
        stadium.setStadiumStatus(StadiumStatus.AVAILABLE);

        Stadium savedStadium = stadiumRepository.save(stadium);

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            java.util.List<com.sportvenue.entity.StadiumImage> images = new java.util.ArrayList<>(
                request.getImageUrls().stream()
                    .map(url -> com.sportvenue.entity.StadiumImage.builder()
                            .stadium(savedStadium)
                            .imageUrl(url)
                            .uploadedAt(java.time.LocalDateTime.now())
                            .build())
                    .toList()
            );
            savedStadium.getImages().clear();
            savedStadium.getImages().addAll(images);
            stadiumRepository.save(savedStadium);
        }

        log.info("Successfully created stadium with ID: {}", savedStadium.getStadiumId());

        return stadiumMapper.toResponse(savedStadium);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<StadiumResponse> getMyStadiums(Integer userId) {
        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        return stadiumRepository
                .findByOwnerOwnerIdAndStadiumStatusNot(owner.getOwnerId(), StadiumStatus.CLOSED)
                .stream()
                .map(stadiumMapper::toResponse)
                .toList();
    }
}
