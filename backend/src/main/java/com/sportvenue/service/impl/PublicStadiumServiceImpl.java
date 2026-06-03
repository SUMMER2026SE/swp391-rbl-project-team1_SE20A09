package com.sportvenue.service.impl;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.AmenityResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.entity.Stadium;
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

        if (request.getUserLat() != null && request.getUserLng() != null) {
            // Manual pagination for accurate distance sorting
            List<Stadium> allStadiums = stadiumRepository.findAll(spec);
            
            if (allStadiums.isEmpty()) {
                return PageResponse.<StadiumResponse>builder()
                        .content(List.of())
                        .pageNo(pageable.getPageNumber())
                        .pageSize(pageable.getPageSize())
                        .totalElements(0)
                        .totalPages(0)
                        .last(true)
                        .build();
            }

            List<Integer> stadiumIds = allStadiums.stream().map(Stadium::getStadiumId).toList();
            List<Stadium> detailedStadiums = stadiumRepository.findAllByStadiumIdIn(stadiumIds);
            Map<Integer, Stadium> stadiumMap = detailedStadiums.stream()
                    .collect(Collectors.toMap(Stadium::getStadiumId, Function.identity()));

            List<StadiumResponse> allResponses = allStadiums.stream()
                    .map(s -> stadiumMap.get(s.getStadiumId()))
                    .map(stadium -> mapToResponse(stadium, request.getUserLat(), request.getUserLng()))
                    .sorted(Comparator.comparing(StadiumResponse::getDistanceInKm, Comparator.nullsLast(Double::compareTo)))
                    .collect(Collectors.toList());

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allResponses.size());
            List<StadiumResponse> pagedResponses = (start <= end) ? allResponses.subList(start, end) : List.of();

            return PageResponse.<StadiumResponse>builder()
                    .content(pagedResponses)
                    .pageNo(pageable.getPageNumber())
                    .pageSize(pageable.getPageSize())
                    .totalElements(allResponses.size())
                    .totalPages((int) Math.ceil((double) allResponses.size() / pageable.getPageSize()))
                    .last(end >= allResponses.size())
                    .build();
        } else {
            // Optimized database pagination when no distance sorting is needed
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

            List<Integer> stadiumIds = stadiumPage.getContent().stream()
                    .map(Stadium::getStadiumId)
                    .toList();

            List<Stadium> detailedStadiums = stadiumRepository.findAllByStadiumIdIn(stadiumIds);
            Map<Integer, Stadium> stadiumMap = detailedStadiums.stream()
                    .collect(Collectors.toMap(Stadium::getStadiumId, Function.identity()));

            List<StadiumResponse> content = stadiumPage.getContent().stream()
                    .map(s -> stadiumMap.get(s.getStadiumId()))
                    .map(stadium -> mapToResponse(stadium, null, null))
                    .collect(Collectors.toList());

            return PageResponse.<StadiumResponse>builder()
                    .content(content)
                    .pageNo(stadiumPage.getNumber())
                    .pageSize(stadiumPage.getSize())
                    .totalElements(stadiumPage.getTotalElements())
                    .totalPages(stadiumPage.getTotalPages())
                    .last(stadiumPage.isLast())
                    .build();
        }
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
}
