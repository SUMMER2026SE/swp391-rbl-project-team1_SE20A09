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
                        return handleDistancePagination(request, pageable, spec);
                } else {
                        return handleStandardPagination(request, pageable, spec);
                }
        }

        private PageResponse<StadiumResponse> handleDistancePagination(StadiumSearchRequest request,
                        Pageable pageable,
                        Specification<Stadium> spec) {
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

                List<Stadium> sortedStadiums = allStadiums.stream()
                                .sorted((s1, s2) -> {
                                        Double d1 = (s1.getLatitude() != null && s1.getLongitude() != null)
                                                        ? calculateHaversineDistance(request.getUserLat(),
                                                                        request.getUserLng(), s1.getLatitude(),
                                                                        s1.getLongitude())
                                                        : null;
                                        Double d2 = (s2.getLatitude() != null && s2.getLongitude() != null)
                                                        ? calculateHaversineDistance(request.getUserLat(),
                                                                        request.getUserLng(), s2.getLatitude(),
                                                                        s2.getLongitude())
                                                        : null;
                                        if (d1 == null && d2 == null) {
                                                return 0;
                                        }
                                        if (d1 == null) {
                                                return 1;
                                        }
                                        if (d2 == null) {
                                                return -1;
                                        }
                                        return d1.compareTo(d2);
                                })
                                .toList();

                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), sortedStadiums.size());
                List<Stadium> pagedStadiums = (start <= end) ? sortedStadiums.subList(start, end) : List.of();

                List<Integer> stadiumIds = pagedStadiums.stream().map(Stadium::getStadiumId).toList();
                List<Stadium> detailedStadiums = stadiumIds.isEmpty() ? List.of()
                                : stadiumRepository.findAllById(stadiumIds);
                Map<Integer, Stadium> stadiumMap = detailedStadiums.stream()
                                .collect(Collectors.toMap(Stadium::getStadiumId, Function.identity()));

                List<StadiumResponse> pagedResponses = pagedStadiums.stream()
                                .map(s -> stadiumMap.getOrDefault(s.getStadiumId(), s))
                                .map(stadium -> mapToResponse(stadium, request.getUserLat(), request.getUserLng()))
                                .collect(Collectors.toList());

                return PageResponse.<StadiumResponse>builder()
                                .content(pagedResponses)
                                .pageNo(pageable.getPageNumber())
                                .pageSize(pageable.getPageSize())
                                .totalElements(sortedStadiums.size())
                                .totalPages((int) Math.ceil((double) sortedStadiums.size() / pageable.getPageSize()))
                                .last(end >= sortedStadiums.size())
                                .build();
        }

        private PageResponse<StadiumResponse> handleStandardPagination(StadiumSearchRequest request,
                        Pageable pageable,
                        Specification<Stadium> spec) {
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

                List<Stadium> detailedStadiums = stadiumRepository.findAllById(stadiumIds);

                // Map them back to keep the original paginated order (or apply distance sort)
                Map<Integer, Stadium> stadiumMap = detailedStadiums.stream()
                                .collect(Collectors.toMap(Stadium::getStadiumId, Function.identity()));

                List<StadiumResponse> content = stadiumPage.getContent().stream()
                                .map(s -> stadiumMap.get(s.getStadiumId()))
                                .filter(java.util.Objects::nonNull)
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

        private StadiumResponse mapToResponse(Stadium stadium, Double userLat, Double userLng) {
                String firstImageUrl = null;
                if (stadium.getImages() != null && !stadium.getImages().isEmpty()) {
                        firstImageUrl = stadium.getImages().get(0).getImageUrl();
                }

                Double distance = null;
                if (userLat != null && userLng != null && stadium.getLatitude() != null
                                && stadium.getLongitude() != null) {
                        distance = calculateHaversineDistance(userLat, userLng, stadium.getLatitude(),
                                        stadium.getLongitude());
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
                        imageUrls = stadium.getImages().stream().map(com.sportvenue.entity.StadiumImage::getImageUrl)
                                        .toList();
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
                                .stadiumStatus(stadium.getStadiumStatus() != null ? stadium.getStadiumStatus().name()
                                                : null)
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
