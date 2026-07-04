package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateComplexRequest;
import com.sportvenue.dto.response.ComplexResponse;
import com.sportvenue.entity.Amenity;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.StadiumComplexImage;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.ComplexStatus;
import com.sportvenue.exception.AppException;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ErrorCode;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.AmenityRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumComplexImageRepository;
import com.sportvenue.repository.StadiumComplexRepository;
import com.sportvenue.service.StadiumComplexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StadiumComplexServiceImpl implements StadiumComplexService {

    private final StadiumComplexRepository stadiumComplexRepository;
    private final OwnerRepository ownerRepository;
    private final SportTypeRepository sportTypeRepository;
    private final AmenityRepository amenityRepository;
    private final StadiumComplexImageRepository stadiumComplexImageRepository;

    @Override
    @Transactional
    public ComplexResponse createComplex(CreateComplexRequest request, Integer userId) {
        log.info("Creating complex: '{}' for user ID: {}", request.getName(), userId);

        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        if (owner.getApprovedStatus() != ApprovedStatus.APPROVED) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Set<SportType> sportTypes = new HashSet<>();
        if (request.getSportTypeIds() != null && !request.getSportTypeIds().isEmpty()) {
            sportTypes = new HashSet<>(sportTypeRepository.findAllById(request.getSportTypeIds()));
        }

        Set<Amenity> amenities = new HashSet<>();
        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
        }

        StadiumComplex complex = StadiumComplex.builder()
                .owner(owner)
                .name(request.getName().trim())
                .description(request.getDescription())
                .address(request.getAddress().trim())
                .phone(request.getPhone())
                .latitude(request.getLatitude() != null ? request.getLatitude().doubleValue() : null)
                .longitude(request.getLongitude() != null ? request.getLongitude().doubleValue() : null)
                .coverImageUrl(request.getCoverImageUrl())
                .complexStatus(ComplexStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.PENDING)
                .averageRating(BigDecimal.valueOf(5.0))
                .reviewCount(0)
                .createdAt(LocalDateTime.now())
                .sportTypes(sportTypes)
                .amenities(amenities)
                .build();

        StadiumComplex savedComplex = stadiumComplexRepository.save(complex);

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            Set<StadiumComplexImage> images = request.getImageUrls().stream()
                    .map(url -> StadiumComplexImage.builder()
                            .complex(savedComplex)
                            .imageUrl(url)
                            .uploadedAt(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toSet());
            stadiumComplexImageRepository.saveAll(images);
            savedComplex.setImages(images);
        }

        log.info("Successfully created complex with ID: {}", savedComplex.getComplexId());
        return mapToResponse(savedComplex);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplexResponse> getMyComplexes(Integer userId) {
        log.info("Retrieving complexes for user ID: {}", userId);
        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        List<StadiumComplex> complexes = stadiumComplexRepository.findByOwnerOwnerId(owner.getOwnerId());
        return complexes.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ComplexResponse getComplexById(Integer complexId) {
        log.info("Retrieving complex by ID: {}", complexId);
        StadiumComplex complex = stadiumComplexRepository.findById(complexId)
                .orElseThrow(() -> new ResourceNotFoundException("StadiumComplex not found with ID: " + complexId));
        return mapToResponse(complex);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplexResponse getComplexByIdAndOwner(Integer complexId, Integer userId) {
        log.info("Retrieving complex by ID: {} and owner user ID: {}", complexId, userId);
        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        StadiumComplex complex = stadiumComplexRepository.findById(complexId)
                .orElseThrow(() -> new ResourceNotFoundException("StadiumComplex not found with ID: " + complexId));

        if (!complex.getOwner().getOwnerId().equals(owner.getOwnerId())) {
            throw new BadRequestException("You do not own this complex");
        }

        return mapToResponse(complex);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplexResponse> getAllComplexes(String approvedStatus) {
        log.info("Admin retrieving all complexes with status: {}", approvedStatus);
        List<StadiumComplex> complexes;
        if (approvedStatus != null) {
            ApprovedStatus status = ApprovedStatus.valueOf(approvedStatus.toUpperCase());
            complexes = stadiumComplexRepository.findAll().stream()
                    .filter(c -> c.getApprovedStatus() == status)
                    .toList();
        } else {
            complexes = stadiumComplexRepository.findAll();
        }
        return complexes.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public ComplexResponse approveComplex(Integer complexId) {
        log.info("Admin approving complex ID: {}", complexId);
        StadiumComplex complex = stadiumComplexRepository.findById(complexId)
                .orElseThrow(() -> new ResourceNotFoundException("StadiumComplex not found with ID: " + complexId));

        complex.setApprovedStatus(ApprovedStatus.APPROVED);
        StadiumComplex saved = stadiumComplexRepository.save(complex);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ComplexResponse rejectComplex(Integer complexId, String reason) {
        log.info("Admin rejecting complex ID: {} with reason: {}", complexId, reason);
        StadiumComplex complex = stadiumComplexRepository.findById(complexId)
                .orElseThrow(() -> new ResourceNotFoundException("StadiumComplex not found with ID: " + complexId));

        complex.setApprovedStatus(ApprovedStatus.REJECTED);
        complex.setRejectionReason(reason);
        StadiumComplex saved = stadiumComplexRepository.save(complex);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ComplexResponse updateComplex(Integer complexId, CreateComplexRequest request, Integer userId) {
        log.info("Updating complex ID: {} for user ID: {}", complexId, userId);

        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        StadiumComplex complex = stadiumComplexRepository.findById(complexId)
                .orElseThrow(() -> new ResourceNotFoundException("StadiumComplex not found with ID: " + complexId));

        if (!complex.getOwner().getOwnerId().equals(owner.getOwnerId())) {
            throw new BadRequestException("You do not own this complex");
        }

        // Check if location changed, put status to PENDING for re-approval
        boolean addressChanged = !complex.getAddress().trim().equals(request.getAddress().trim());
        boolean latChanged = (complex.getLatitude() == null && request.getLatitude() != null) ||
                (complex.getLatitude() != null && request.getLatitude() == null) ||
                (complex.getLatitude() != null && request.getLatitude() != null && !complex.getLatitude().equals(request.getLatitude().doubleValue()));
        boolean lngChanged = (complex.getLongitude() == null && request.getLongitude() != null) ||
                (complex.getLongitude() != null && request.getLongitude() == null) ||
                (complex.getLongitude() != null && request.getLongitude() != null && !complex.getLongitude().equals(request.getLongitude().doubleValue()));

        if (addressChanged || latChanged || lngChanged) {
            complex.setApprovedStatus(ApprovedStatus.PENDING);
        }

        complex.setName(request.getName().trim());
        complex.setDescription(request.getDescription());
        complex.setAddress(request.getAddress().trim());
        complex.setPhone(request.getPhone());
        complex.setLatitude(request.getLatitude() != null ? request.getLatitude().doubleValue() : null);
        complex.setLongitude(request.getLongitude() != null ? request.getLongitude().doubleValue() : null);
        complex.setCoverImageUrl(request.getCoverImageUrl());

        // Update sport types
        Set<SportType> sportTypes = new HashSet<>();
        if (request.getSportTypeIds() != null && !request.getSportTypeIds().isEmpty()) {
            sportTypes = new HashSet<>(sportTypeRepository.findAllById(request.getSportTypeIds()));
        }
        complex.setSportTypes(sportTypes);

        // Update amenities
        Set<Amenity> amenities = new HashSet<>();
        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
        }
        complex.setAmenities(amenities);

        // Update gallery images
        complex.getImages().clear();
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            Set<StadiumComplexImage> images = request.getImageUrls().stream()
                    .map(url -> StadiumComplexImage.builder()
                            .complex(complex)
                            .imageUrl(url)
                            .uploadedAt(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toSet());
            complex.getImages().addAll(images);
        }

        StadiumComplex saved = stadiumComplexRepository.save(complex);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void suspendComplex(Integer complexId, Integer userId) {
        log.info("Owner {} requesting suspend for complex {}", userId, complexId);
        StadiumComplex complex = loadOwnedComplex(complexId, userId);
        complex.setComplexStatus(ComplexStatus.MAINTENANCE);
        stadiumComplexRepository.save(complex);
        log.info("Complex {} successfully suspended by owner {}", complexId, userId);
    }

    @Override
    @Transactional
    public void activateComplex(Integer complexId, Integer userId) {
        log.info("Owner {} requesting activation for complex {}", userId, complexId);
        StadiumComplex complex = loadOwnedComplex(complexId, userId);
        complex.setComplexStatus(ComplexStatus.AVAILABLE);
        stadiumComplexRepository.save(complex);
        log.info("Complex {} successfully activated by owner {}", complexId, userId);
    }

    /** Load Complex + xác thực đúng chủ sở hữu — dùng chung cho suspend/activate. */
    private StadiumComplex loadOwnedComplex(Integer complexId, Integer userId) {
        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        StadiumComplex complex = stadiumComplexRepository.findById(complexId)
                .orElseThrow(() -> new ResourceNotFoundException("StadiumComplex not found with ID: " + complexId));

        if (!complex.getOwner().getOwnerId().equals(owner.getOwnerId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return complex;
    }

    private ComplexResponse mapToResponse(StadiumComplex complex) {
        return ComplexResponse.builder()
                .complexId(complex.getComplexId())
                .ownerId(complex.getOwner().getOwnerId())
                .name(complex.getName())
                .description(complex.getDescription())
                .address(complex.getAddress())
                .phone(complex.getPhone())
                .latitude(complex.getLatitude())
                .longitude(complex.getLongitude())
                .coverImageUrl(complex.getCoverImageUrl())
                .complexStatus(complex.getComplexStatus().name())
                .approvedStatus(complex.getApprovedStatus().name())
                .rejectionReason(complex.getRejectionReason())
                .averageRating(complex.getAverageRating())
                .reviewCount(complex.getReviewCount())
                .sportTypeIds(complex.getSportTypes().stream().map(SportType::getSportTypeId).collect(Collectors.toSet()))
                .sportNames(complex.getSportTypes().stream().map(SportType::getSportName).collect(Collectors.toList()))
                .amenityIds(complex.getAmenities().stream().map(Amenity::getAmenityId).collect(Collectors.toSet()))
                .imageUrls(complex.getImages().stream().map(StadiumComplexImage::getImageUrl).collect(Collectors.toList()))
                .createdAt(complex.getCreatedAt())
                .build();
    }
}
