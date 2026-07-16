package com.sportvenue.service.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.AiChatTurnResponse;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.service.PublicStadiumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetPriceHandler {

    /** Số sân tối đa trả về trong chat. */
    private static final int MAX_RESULTS = 5;

    private final PublicStadiumService publicStadiumService;
    private final SportTypeRepository sportTypeRepository;
    private final TimeSlotRepository timeSlotRepository;

    public AiChatTurnResponse handle(JsonNode args, String message) {
        // Lấy sportName từ params (Groq đã extract)
        String sportName = (args != null && args.hasNonNull("sportName"))
                ? args.get("sportName").asText() : null;
        String location = (args != null && args.hasNonNull("location"))
                ? args.get("location").asText() : null;

        // Resolve sportTypeId từ tên môn
        Integer sportTypeId = resolveSportTypeId(sportName);

        // Build search request — chỉ lấy sân đang hoạt động, sort theo giá tăng dần
        StadiumSearchRequest req = StadiumSearchRequest.builder()
                .sportTypeId(sportTypeId)
                .keyword(location)
                .sortBy("pricePerHour")
                .sortDirection("ASC")
                .page(0)
                .size(MAX_RESULTS)
                .build();

        List<StadiumResponse> stadiums;
        try {
            stadiums = publicStadiumService.searchStadiums(req).getContent();
        } catch (Exception e) {
            log.warn("GetPriceHandler: search failed", e);
            stadiums = List.of();
        }

        if (stadiums.isEmpty()) {
            String sportLabel = sportName != null ? " môn " + sportName : "";
            String locationLabel = location != null ? " tại " + location : "";
            return AiChatTurnResponse.builder()
                    .intent("get_price")
                    .message("Hiện chưa tìm thấy sân" + sportLabel + locationLabel
                            + " nào đang hoạt động. Bạn thử tìm với tiêu chí khác nhé.")
                    .stadiums(List.of())
                    .build();
        }

        // Bổ sung giá slot min vào từng sân nếu pricePerHour chưa có
        List<StadiumResponse> enriched = stadiums.stream()
                .map(s -> {
                    if (s.getPricePerHour() == null || s.getPricePerHour().compareTo(BigDecimal.ZERO) == 0) {
                        BigDecimal minSlotPrice = resolveMinSlotPrice(s.getStadiumId());
                        if (minSlotPrice != null) {
                            return copyWithPrice(s, minSlotPrice);
                        }
                    }
                    return s;
                })
                .toList();

        // Build message
        String sportLabel = sportName != null ? " sân " + sportName : " các sân";
        String locationLabel = location != null ? " tại " + location : "";
        String msg = "Đây là giá tham khảo của" + sportLabel + locationLabel + " hiện tại:";

        return AiChatTurnResponse.builder()
                .intent("get_price")
                .message(msg)
                .stadiums(enriched)
                .build();
    }

    /**
     * Tìm giá slot thấp nhất của sân — dùng khi pricePerHour chưa được set.
     */
    private BigDecimal resolveMinSlotPrice(Integer stadiumId) {
        try {
            List<TimeSlot> slots = timeSlotRepository
                    .findByStadiumStadiumIdAndSlotStatus(stadiumId, SlotStatus.AVAILABLE);
            return slots.stream()
                    .map(TimeSlot::getPricePerSlot)
                    .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                    .min(BigDecimal::compareTo)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("GetPriceHandler: could not resolve slot price for stadium {}", stadiumId, e);
            return null;
        }
    }

    /**
     * Copy StadiumResponse với giá mới (builder immutable).
     */
    private StadiumResponse copyWithPrice(StadiumResponse s, BigDecimal price) {
        return StadiumResponse.builder()
                .stadiumId(s.getStadiumId())
                .stadiumName(s.getStadiumName())
                .description(s.getDescription())
                .sportTypeId(s.getSportTypeId())
                .sportName(s.getSportName())
                .address(s.getAddress())
                .averageRating(s.getAverageRating())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .distanceInKm(s.getDistanceInKm())
                .isFootballType(s.getIsFootballType())
                .firstImageUrl(s.getFirstImageUrl())
                .imageUrls(s.getImageUrls())
                .openTime(s.getOpenTime())
                .closeTime(s.getCloseTime())
                .pricePerHour(price.setScale(0, RoundingMode.HALF_UP))
                .stadiumStatus(s.getStadiumStatus())
                .approvedStatus(s.getApprovedStatus())
                .footballFieldType(s.getFootballFieldType())
                .nodeType(s.getNodeType())
                .complexId(s.getComplexId())
                .complexName(s.getComplexName())
                .parentStadiumId(s.getParentStadiumId())
                .amenities(s.getAmenities())
                .build();
    }

    /**
     * Normalize và khớp tên môn thể thao — dùng fuzzy match giống StadiumSearchHandler.
     */
    private Integer resolveSportTypeId(String sportName) {
        if (sportName == null || sportName.isBlank()) {
            return null;
        }
        List<SportType> all = sportTypeRepository.findAll();
        String normalized = normalize(sportName);
        // Exact match trước
        for (SportType st : all) {
            if (normalize(st.getSportName()).equals(normalized)) {
                return st.getSportTypeId();
            }
        }
        // Alias match (đá banh → bóng đá, cầu lông → cầu lông...)
        for (SportType st : all) {
            if (normalize(st.getSportName()).contains(normalized)
                    || normalized.contains(normalize(st.getSportName()))) {
                return st.getSportTypeId();
            }
        }
        return null;
    }

    private String normalize(String s) {
        return s == null ? "" : s.toLowerCase()
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[đ]", "d")
                .replaceAll("[^a-z0-9\\s]", "")
                .trim();
    }
}
