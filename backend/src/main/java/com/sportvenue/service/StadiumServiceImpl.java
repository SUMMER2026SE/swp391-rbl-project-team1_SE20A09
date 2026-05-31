package com.sportvenue.service;

import com.sportvenue.dto.request.CreateStadiumRequest;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.config.FileStorageProperties;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumImage;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.exception.BadRequestException;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StadiumServiceImpl implements StadiumService {

    private final StadiumRepository stadiumRepository;
    private final OwnerRepository ownerRepository;
    private final SportTypeRepository sportTypeRepository;
    private final StadiumMapper stadiumMapper;
    private final FileStorageProperties fileStorageProperties;

    @Override
    @Transactional
    public StadiumResponse createStadium(CreateStadiumRequest request, Integer userId) {
        normalizeRequest(request);
        log.info("Creating stadium for user: {}", userId);

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

        List<StadiumImage> images = request.getImageUrls().stream()
                .map(url -> StadiumImage.builder()
                        .stadium(stadium)
                        .imageUrl(url)
                        .build())
                .toList();
        stadium.getImages().addAll(images);

        Stadium savedStadium = stadiumRepository.save(stadium);
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

    private void normalizeRequest(CreateStadiumRequest request) {
        request.setStadiumName(trimToNull(request.getStadiumName()));
        request.setAddress(trimToNull(request.getAddress()));
        request.setDescription(trimToNull(request.getDescription()));
        if (request.getOpenTime() != null && request.getCloseTime() != null
                && !request.getCloseTime().isAfter(request.getOpenTime())) {
            throw new BadRequestException("Close time must be after open time");
        }
        if (request.getImageUrls() == null || request.getImageUrls().isEmpty()) {
            throw new BadRequestException("At least one stadium image is required");
        }
        request.setImageUrls(request.getImageUrls().stream()
                .map(this::trimToNull)
                .toList());
        if (request.getImageUrls().stream().anyMatch(url -> url == null)) {
            throw new BadRequestException("Image URL cannot be blank");
        }
        validateStadiumImageUrls(request.getImageUrls());
    }

    private void validateStadiumImageUrls(List<String> imageUrls) {
        String baseUrl = fileStorageProperties.getBaseUrl();
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String allowedPrefix = normalizedBaseUrl + "/api/v1/files/stadiums/";

        boolean hasInvalidUrl = imageUrls.stream()
                .anyMatch(url -> !url.startsWith(allowedPrefix)
                        || url.contains("..")
                        || url.length() <= allowedPrefix.length());
        if (hasInvalidUrl) {
            throw new BadRequestException("Image URLs must be uploaded through the stadium image endpoint");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
