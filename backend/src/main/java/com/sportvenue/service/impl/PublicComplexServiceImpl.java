package com.sportvenue.service.impl;

import com.sportvenue.dto.request.StadiumComplexSearchRequest;
import com.sportvenue.dto.response.CourtResponse;
import com.sportvenue.dto.response.FacilityResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.PublicComplexDetailResponse;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.ComplexStatus;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.StadiumComplexRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.specification.StadiumComplexSpecification;
import com.sportvenue.service.PublicComplexService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicComplexServiceImpl implements PublicComplexService {

    private final StadiumComplexRepository stadiumComplexRepository;
    private final StadiumRepository stadiumRepository;

    @Override
    @Transactional(readOnly = true)
    public PublicComplexDetailResponse getPublicComplexById(Integer complexId) {
        log.info("Public request to get complex detail by ID: {}", complexId);
        StadiumComplex complex = stadiumComplexRepository.findWithDetailsByComplexId(complexId)
                .orElseThrow(() -> new ResourceNotFoundException("Complex not found with ID: " + complexId));

        // Filter: only show if APPROVED and not CLOSED
        if (complex.getApprovedStatus() != ApprovedStatus.APPROVED
                || complex.getComplexStatus() == ComplexStatus.CLOSED) {
            throw new ResourceNotFoundException("Complex is not available or does not exist");
        }

        return mapToDetailResponse(complex);
    }

    private PublicComplexDetailResponse mapToDetailResponse(StadiumComplex complex) {
        String ownerName = complex.getOwner() != null 
                ? (complex.getOwner().getBusinessName() != null ? complex.getOwner().getBusinessName() : complex.getOwner().getUser().getFullName())
                : "N/A";
        String ownerPhone = complex.getPhone() != null 
                ? complex.getPhone() 
                : (complex.getOwner() != null ? complex.getOwner().getUser().getPhoneNumber() : "N/A");

        return PublicComplexDetailResponse.builder()
                .complexId(complex.getComplexId())
                .name(complex.getName())
                .description(complex.getDescription())
                .address(complex.getAddress())
                .phone(complex.getPhone())
                .latitude(complex.getLatitude())
                .longitude(complex.getLongitude())
                .coverImageUrl(complex.getCoverImageUrl())
                .complexStatus(complex.getComplexStatus().name())
                .approvedStatus(complex.getApprovedStatus().name())
                .averageRating(complex.getAverageRating())
                .reviewCount(complex.getReviewCount())
                .ownerName(ownerName)
                .ownerPhone(ownerPhone)
                .sportTypes(complex.getSportTypes() != null 
                        ? complex.getSportTypes().stream()
                                .map(st -> PublicComplexDetailResponse.SportTypeInfo.builder()
                                        .sportTypeId(st.getSportTypeId())
                                        .sportName(st.getSportName())
                                        .build())
                                .collect(Collectors.toList())
                        : Collections.emptyList())
                .amenities(complex.getAmenities() != null
                        ? complex.getAmenities().stream()
                                .map(am -> PublicComplexDetailResponse.AmenityInfo.builder()
                                        .amenityId(am.getAmenityId())
                                        .name(am.getName())
                                        .icon(am.getIcon())
                                        .build())
                                .collect(Collectors.toList())
                        : java.util.Collections.emptyList())
                .images(complex.getImages() != null
                        ? complex.getImages().stream()
                                .map(img -> PublicComplexDetailResponse.ImageInfo.builder()
                                        .imageId(img.getImageId())
                                        .imageUrl(img.getImageUrl())
                                        .build())
                                .collect(Collectors.toList())
                        : java.util.Collections.emptyList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacilityResponse> getFacilitiesByComplexId(Integer complexId) {
        log.info("Public request to get facilities under complex ID: {}", complexId);
        // Verify complex exists and is approved and not closed
        StadiumComplex complex = stadiumComplexRepository.findById(complexId)
                .orElseThrow(() -> new ResourceNotFoundException("Complex not found with ID: " + complexId));
        if (complex.getApprovedStatus() != ApprovedStatus.APPROVED
                || complex.getComplexStatus() == ComplexStatus.CLOSED) {
            throw new ResourceNotFoundException("Complex is not available or does not exist");
        }

        List<Stadium> facilities = stadiumRepository.findFacilitiesByComplexId(complexId);
        return facilities.stream()
                .map(this::mapToFacilityResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtResponse> getCourtsByFacilityId(Integer facilityId) {
        log.info("Public request to get courts under facility ID: {}", facilityId);
        // Verify facility exists and is a facility node
        Stadium facility = stadiumRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + facilityId));

        if (facility.getNodeType() != StadiumNodeType.FACILITY) {
            throw new BadRequestException("Provided ID is not a facility");
        }
        
        List<Stadium> courts = stadiumRepository.findCourtsByFacilityId(facilityId);
        return courts.stream()
                .map(this::mapToCourtResponse)
                .collect(Collectors.toList());
    }

    private FacilityResponse mapToFacilityResponse(Stadium s) {
        FacilityResponse.SportTypeInfo sportTypeInfo = null;
        if (s.getSportType() != null) {
            sportTypeInfo = FacilityResponse.SportTypeInfo.builder()
                    .sportTypeId(s.getSportType().getSportTypeId())
                    .sportName(s.getSportType().getSportName())
                    .build();
        }

        List<String> imageUrls = s.getImages() != null
                ? s.getImages().stream().map(img -> img.getImageUrl()).collect(Collectors.toList())
                : Collections.emptyList();

        return FacilityResponse.builder()
                .stadiumId(s.getStadiumId())
                .stadiumName(s.getStadiumName())
                .description(s.getDescription())
                .sportType(sportTypeInfo)
                .openTime(s.getOpenTime())
                .closeTime(s.getCloseTime())
                .stadiumStatus(s.getStadiumStatus().name())
                .imageUrls(imageUrls)
                .build();
    }

    private CourtResponse mapToCourtResponse(Stadium s) {
        List<String> imageUrls = s.getImages() != null
                ? s.getImages().stream().map(img -> img.getImageUrl()).collect(Collectors.toList())
                : Collections.emptyList();

        Integer parentId = s.getParentStadium() != null ? s.getParentStadium().getStadiumId() : null;

        return CourtResponse.builder()
                .stadiumId(s.getStadiumId())
                .stadiumName(s.getStadiumName())
                .description(s.getDescription())
                .pricePerHour(s.getPricePerHour())
                .parentStadiumId(parentId)
                .stadiumStatus(s.getStadiumStatus().name())
                .imageUrls(imageUrls)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PublicComplexDetailResponse> searchComplexes(StadiumComplexSearchRequest request) {
        log.info("Public request to search complexes with keyword: {}", request.getKeyword());

        Specification<StadiumComplex> spec = StadiumComplexSpecification.withDynamicFilter(request);
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        List<StadiumComplex> contentList;
        long totalElements;
        int totalPages;

        if (request.hasLocation()) {
            List<StadiumComplex> allMatched = stadiumComplexRepository.findAll(spec);

            List<StadiumComplex> sorted = allMatched.stream()
                    .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                    .sorted((c1, c2) -> {
                        Double d1 = calculateHaversineDistance(request.getUserLat(), request.getUserLng(), c1.getLatitude(), c1.getLongitude());
                        Double d2 = calculateHaversineDistance(request.getUserLat(), request.getUserLng(), c2.getLatitude(), c2.getLongitude());
                        return d1.compareTo(d2);
                    })
                    .collect(Collectors.toList());

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), sorted.size());
            contentList = (start <= end) ? sorted.subList(start, end) : Collections.emptyList();

            totalElements = sorted.size();
            totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        } else {
            Page<StadiumComplex> dbPage = stadiumComplexRepository.findAll(spec, pageable);
            contentList = dbPage.getContent();
            totalElements = dbPage.getTotalElements();
            totalPages = dbPage.getTotalPages();
        }

        if (contentList.isEmpty()) {
            return PageResponse.<PublicComplexDetailResponse>builder()
                    .content(Collections.emptyList())
                    .pageNumber(pageable.getPageNumber())
                    .pageSize(pageable.getPageSize())
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }

        List<PublicComplexDetailResponse> responses = buildSearchResponses(contentList, request);
        boolean isLast = pageable.getOffset() + responses.size() >= totalElements;

        return PageResponse.<PublicComplexDetailResponse>builder()
                .content(responses)
                .pageNumber(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(isLast)
                .build();
    }

    private List<PublicComplexDetailResponse> buildSearchResponses(List<StadiumComplex> contentList, StadiumComplexSearchRequest request) {
        List<Integer> complexIds = contentList.stream().map(StadiumComplex::getComplexId).collect(Collectors.toList());
        List<Object[]> minMaxPrices = stadiumRepository.findMinMaxPriceByComplexIds(complexIds);

        java.util.Map<Integer, java.math.BigDecimal[]> priceMap = new java.util.HashMap<>();
        for (Object[] row : minMaxPrices) {
            Integer complexId = (Integer) row[0];
            java.math.BigDecimal minPrice = (java.math.BigDecimal) row[1];
            java.math.BigDecimal maxPrice = (java.math.BigDecimal) row[2];
            priceMap.put(complexId, new java.math.BigDecimal[]{minPrice, maxPrice});
        }

        return contentList.stream()
                .map(complex -> {
                    PublicComplexDetailResponse detail = mapToDetailResponse(complex);
                    if (request.hasLocation() && complex.getLatitude() != null && complex.getLongitude() != null) {
                        double dist = calculateHaversineDistance(request.getUserLat(), request.getUserLng(), complex.getLatitude(), complex.getLongitude());
                        detail.setDistanceInKm(dist);
                    }
                    java.math.BigDecimal[] prices = priceMap.get(complex.getComplexId());
                    if (prices != null) {
                        detail.setMinPrice(prices[0]);
                        detail.setMaxPrice(prices[1]);
                    }
                    return detail;
                })
                .collect(Collectors.toList());
    }

    private Double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
