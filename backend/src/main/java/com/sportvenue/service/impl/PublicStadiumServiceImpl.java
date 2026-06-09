package com.sportvenue.service.impl;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.AccessoryResponse;
import com.sportvenue.dto.response.AmenityResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.StadiumDetailResponse;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.entity.Review;
import com.sportvenue.entity.Stadium;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.ReviewRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.specification.StadiumSpecification;
import com.sportvenue.service.PublicStadiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sportvenue.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicStadiumServiceImpl implements PublicStadiumService {

    private final StadiumRepository stadiumRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StadiumResponse> searchStadiums(StadiumSearchRequest request) {
        log.info("Searching stadiums with keyword: {}, targetDate: {}...", request.getKeyword(),
                request.getTargetDate());
        
        if (request.getStartTime() != null && request.getEndTime() != null
                && !request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("Giờ kết thúc phải sau giờ bắt đầu");
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        Specification<Stadium> spec = StadiumSpecification.withDynamicFilter(request, true);

        // Step 1: Paginated fetch (without JOIN FETCH collections) to avoid HHH000104
        // memory pagination
        Page<Stadium> stadiumPage = stadiumRepository.findAll(spec, pageable);

        if (stadiumPage.isEmpty()) {
            return PageResponse.<StadiumResponse>builder()
                    .content(List.of())
                    .pageNo(stadiumPage.getNumber())
                    .pageSize(stadiumPage.getSize())
                    .totalElements(stadiumPage.getTotalElements())
                    .totalPages(stadiumPage.getTotalPages())
                    .last(stadiumPage.isLast())
                    .build();
        }

        // Step 2: Fetch detailed entities using EntityGraph to prevent N+1
        List<Integer> stadiumIds = stadiumPage.getContent().stream()
                .map(Stadium::getStadiumId)
                .toList();

        List<Stadium> detailedStadiums = stadiumRepository.findAllById(stadiumIds);

        // Map them back to keep the original paginated order (or apply distance sort)
        Map<Integer, Stadium> stadiumMap = detailedStadiums.stream()
                .collect(Collectors.toMap(Stadium::getStadiumId, Function.identity()));

        List<StadiumResponse> content = stadiumPage.getContent().stream()
                .map(s -> stadiumMap.get(s.getStadiumId()))
                .map(stadium -> mapToResponse(stadium, request.getUserLat(), request.getUserLng()))
                .collect(Collectors.toList());

        // Sort by distance if GPS is provided
        if (request.getUserLat() != null && request.getUserLng() != null) {
            content.sort(
                    Comparator.comparing(StadiumResponse::getDistanceInKm, Comparator.nullsLast(Double::compareTo)));
        }

        return PageResponse.<StadiumResponse>builder()
                .content(content)
                .pageNo(stadiumPage.getNumber())
                .pageSize(stadiumPage.getSize())
                .totalElements(stadiumPage.getTotalElements())
                .totalPages(stadiumPage.getTotalPages())
                .last(stadiumPage.isLast())
                .build();
    }

    private StadiumResponse mapToResponse(Stadium stadium, Double userLat, Double userLng) {
        String firstImageUrl = null;
        if (stadium.getImages() != null && !stadium.getImages().isEmpty()) {
            firstImageUrl = stadium.getImages().get(0).getImageUrl();
        }

        Double distance = null;
        if (userLat != null && userLng != null && stadium.getLatitude() != null && stadium.getLongitude() != null) {
            distance = calculateHaversineDistance(userLat, userLng, stadium.getLatitude(), stadium.getLongitude());
        }

        List<AmenityResponse> amenityResponses = stadium.getAmenities().stream()
                .map(a -> AmenityResponse.builder()
                        .amenityId(a.getAmenityId())
                        .name(a.getName())
                        .icon(a.getIcon())
                        .build())
                .toList();

        List<String> imageUrls = null;
        if (stadium.getImages() != null && !stadium.getImages().isEmpty()) {
            imageUrls = stadium.getImages().stream().map(com.sportvenue.entity.StadiumImage::getImageUrl).toList();
        }

        return StadiumResponse.builder()
                .stadiumId(stadium.getStadiumId())
                .stadiumName(stadium.getStadiumName())
                .description(stadium.getDescription())
                .address(stadium.getAddress())
                .pricePerHour(stadium.getPricePerHour())
                .capacity(stadium.getCapacity())
                .averageRating(stadium.getAverageRating())
                .latitude(stadium.getLatitude())
                .longitude(stadium.getLongitude())
                .distanceInKm(distance)
                .sportName(stadium.getSportType().getSportName())
                .firstImageUrl(firstImageUrl)
                .imageUrls(imageUrls)
                .openTime(stadium.getOpenTime())
                .closeTime(stadium.getCloseTime())
                .stadiumStatus(stadium.getStadiumStatus() != null ? stadium.getStadiumStatus().name() : null)
                .amenities(amenityResponses)
                .build();
    }

    private Double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    @Transactional(readOnly = true)
    public StadiumDetailResponse getStadiumDetail(Integer stadiumId) {
        log.info("Fetching detail for stadium ID: {}", stadiumId);
        
        Stadium stadium = stadiumRepository.findWithDetailsByStadiumId(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Sân với ID " + stadiumId + " không tồn tại"));
        
        Pageable reviewPage = PageRequest.of(0, 5);
        List<Review> recentReviews = reviewRepository
                .findByStadiumStadiumIdOrderByCreatedAtDesc(stadiumId, reviewPage)
                .getContent();
        
        long totalReviews = reviewRepository.countByStadiumStadiumId(stadiumId);
        
        return StadiumDetailResponse.builder()
                .stadiumId(stadium.getStadiumId())
                .stadiumName(stadium.getStadiumName())
                .description(stadium.getDescription())
                .address(stadium.getAddress())
                .pricePerHour(stadium.getPricePerHour())
                .capacity(stadium.getCapacity())
                .averageRating(stadium.getAverageRating())
                .totalReviews(totalReviews)
                .latitude(stadium.getLatitude())
                .longitude(stadium.getLongitude())
                .sportName(stadium.getSportType().getSportName())
                .imageUrls(stadium.getImages().stream()
                        .map(img -> img.getImageUrl())
                        .toList())
                .openTime(stadium.getOpenTime())
                .closeTime(stadium.getCloseTime())
                .stadiumStatus(stadium.getStadiumStatus() != null ? stadium.getStadiumStatus().name() : null)
                .amenities(stadium.getAmenities().stream()
                        .map(a -> AmenityResponse.builder()
                                .amenityId(a.getAmenityId())
                                .name(a.getName())
                                .icon(a.getIcon())
                                .build())
                        .toList())
                .accessories(stadium.getAccessories().stream()
                        .map(acc -> AccessoryResponse.builder()
                                .accessoryId(acc.getAccessoryId())
                                .name(acc.getName())
                                .pricePerUnit(acc.getPricePerUnit())
                                .quantity(acc.getQuantity())
                                .build())
                        .toList())
                .timeSlots(stadium.getTimeSlots().stream()
                        .map(ts -> TimeSlotResponse.builder()
                                .slotId(ts.getSlotId())
                                .startTime(ts.getStartTime())
                                .endTime(ts.getEndTime())
                                .slotStatus(ts.getSlotStatus() != null ? ts.getSlotStatus().name() : null)
                                .build())
                        .toList())
                .owner(StadiumDetailResponse.OwnerInfoDto.builder()
                        .ownerId(stadium.getOwner().getOwnerId())
                        .ownerName(stadium.getOwner().getUser().getFullName())
                        .phoneNumber(stadium.getOwner().getUser().getPhoneNumber())
                        .build())
                .recentReviews(recentReviews.stream()
                        .map(r -> StadiumDetailResponse.ReviewDto.builder()
                                .reviewId(r.getReviewId())
                                .userName(r.getUser().getFullName())
                                .userAvatar(r.getUser().getAvatarUrl())
                                .ratingScore(r.getRatingScore())
                                .comment(r.getComment())
                                .ownerResponse(r.getOwnerResponse())
                                .createdAt(r.getCreatedAt())
                                .build())
                        .toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StadiumDetailResponse.ReviewDto> getStadiumReviews(Integer stadiumId, int page, int size) {
        log.info("Fetching reviews for stadium ID: {}, page: {}, size: {}", stadiumId, page, size);

        if (!stadiumRepository.existsById(stadiumId)) {
            throw new ResourceNotFoundException("Sân với ID " + stadiumId + " không tồn tại");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository
                .findByStadiumStadiumIdOrderByCreatedAtDesc(stadiumId, pageable);

        List<StadiumDetailResponse.ReviewDto> content = reviewPage.getContent().stream()
                .map(r -> StadiumDetailResponse.ReviewDto.builder()
                        .reviewId(r.getReviewId())
                        .userName(r.getUser().getFullName())
                        .userAvatar(r.getUser().getAvatarUrl())
                        .ratingScore(r.getRatingScore())
                        .comment(r.getComment())
                        .ownerResponse(r.getOwnerResponse())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();

        return PageResponse.<StadiumDetailResponse.ReviewDto>builder()
                .content(content)
                .pageNo(reviewPage.getNumber())
                .pageSize(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .build();
    }
}
