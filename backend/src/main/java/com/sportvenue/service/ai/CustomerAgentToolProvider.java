package com.sportvenue.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.dto.response.MatchResponse;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.mapper.StadiumMapper;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.service.MatchRequestService;
import com.sportvenue.service.PublicStadiumService;
import com.sportvenue.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAgentToolProvider implements AgentToolProvider {

    private final PublicStadiumService publicStadiumService;
    private final TimeSlotService timeSlotService;
    private final MatchRequestService matchRequestService;
    private final SportTypeRepository sportTypeRepository;
    private final StadiumRepository stadiumRepository;
    private final StadiumMapper stadiumMapper;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(buildSearchStadiumsToolDefinition());
        tools.add(buildGetStadiumSlotsToolDefinition());
        tools.add(buildFindMatchRequestsToolDefinition());
        return tools;
    }

    private Map<String, Object> buildSearchStadiumsToolDefinition() {
        Map<String, Object> propertiesSearch = new HashMap<>();

        propertiesSearch.put("keyword", Map.of(
            "type", "string",
            "description", "Tên riêng hoặc một phần tên của sân đấu, nếu người dùng nhắc đến một sân cụ thể (ví dụ: 'Sân vận động Cẩm Lệ')."
        ));

        propertiesSearch.put("sportName", Map.of(
            "type", "string",
            "description", "Tên môn thể thao bằng tiếng Việt (ví dụ: Bóng đá, Cầu lông, Bóng rổ)."
        ));

        propertiesSearch.put("district", Map.of(
            "type", "string",
            "description", "Tên quận hoặc khu vực tại TP.HCM (ví dụ: Quận 9, Thủ Đức, Quận 1, Bình Thạnh)."
        ));

        propertiesSearch.put("minPrice", Map.of(
            "type", "number",
            "description", "Giá thuê tối thiểu mỗi giờ (VND)."
        ));

        propertiesSearch.put("maxPrice", Map.of(
            "type", "number",
            "description", "Giá thuê tối đa mỗi giờ (VND)."
        ));

        propertiesSearch.put("targetDate", Map.of(
            "type", "string",
            "description", "Ngày muốn đặt sân dưới định dạng YYYY-MM-DD."
        ));

        propertiesSearch.put("startTime", Map.of(
            "type", "string",
            "description", "Giờ bắt đầu dưới định dạng HH:mm theo hệ 24 giờ. Phải tự quy đổi giờ nói thông thường: " +
                    "'sáng' giữ nguyên (7h sáng=07:00), 'trưa' quanh 12h (12h trưa=12:00), " +
                    "'chiều'/'tối'/'đêm' cộng thêm 12 nếu số giờ nhỏ hơn 12 (5h chiều=17:00, 9h tối=21:00, 11h đêm=23:00)."
        ));

        propertiesSearch.put("endTime", Map.of(
            "type", "string",
            "description", "Giờ kết thúc dưới định dạng HH:mm theo hệ 24 giờ — áp dụng cùng quy tắc quy đổi như startTime."
        ));

        Map<String, Object> parametersSearch = new HashMap<>();
        parametersSearch.put("type", "object");
        parametersSearch.put("properties", propertiesSearch);

        Map<String, Object> functionSearch = new HashMap<>();
        functionSearch.put("name", "searchStadiums");
        functionSearch.put("description", "Tìm kiếm các sân thể thao dựa trên các điều kiện lọc như tên sân, môn thể thao, quận/khu vực, khoảng giá, và thời gian. " +
                "LUÔN gọi tool này trước để lấy đúng stadiumId khi người dùng nhắc tên một sân cụ thể — KHÔNG được tự đoán/bịa stadiumId.");
        functionSearch.put("parameters", parametersSearch);

        Map<String, Object> searchStadiums = new HashMap<>();
        searchStadiums.put("type", "function");
        searchStadiums.put("function", functionSearch);
        return searchStadiums;
    }

    private Map<String, Object> buildGetStadiumSlotsToolDefinition() {
        Map<String, Object> propertiesSlots = new HashMap<>();
        propertiesSlots.put("stadiumId", Map.of(
            "type", "integer",
            "description", "ID số nguyên của sân đấu (lấy từ kết quả searchStadiums), cần lấy khung giờ."
        ));

        Map<String, Object> parametersSlots = new HashMap<>();
        parametersSlots.put("type", "object");
        parametersSlots.put("properties", propertiesSlots);
        parametersSlots.put("required", List.of("stadiumId"));

        Map<String, Object> functionSlots = new HashMap<>();
        functionSlots.put("name", "getStadiumSlots");
        functionSlots.put("description", "Lấy các khung giờ hoạt động hoặc trạng thái trống của một sân thể thao cụ thể theo ID sân. " +
                "Nếu chưa biết stadiumId (người dùng chỉ nhắc tên sân), PHẢI gọi searchStadiums trước để lấy ID thật — không được tự đoán số.");
        functionSlots.put("parameters", parametersSlots);

        Map<String, Object> getStadiumSlots = new HashMap<>();
        getStadiumSlots.put("type", "function");
        getStadiumSlots.put("function", functionSlots);
        return getStadiumSlots;
    }

    private Map<String, Object> buildFindMatchRequestsToolDefinition() {
        Map<String, Object> propertiesMatches = new HashMap<>();
        propertiesMatches.put("location", Map.of(
            "type", "string",
            "description", "Khu vực/thành phố/quận muốn tìm kèo ghép (ví dụ: Đà Nẵng, Hà Nội, Quận 9, Thủ Đức). Bỏ trống nếu muốn tìm ở mọi khu vực."
        ));
        propertiesMatches.put("page", Map.of(
            "type", "integer",
            "description", "Số thứ tự trang kết quả (mặc định 0)."
        ));
        propertiesMatches.put("size", Map.of(
            "type", "integer",
            "description", "Số lượng phần tử mỗi trang (mặc định 10)."
        ));

        Map<String, Object> parametersMatches = new HashMap<>();
        parametersMatches.put("type", "object");
        parametersMatches.put("properties", propertiesMatches);

        Map<String, Object> functionMatches = new HashMap<>();
        functionMatches.put("name", "findMatchRequests");
        functionMatches.put("description", "Tìm kiếm danh sách các kèo ghép thể thao (matchmaking) đang mở và cần tuyển người chơi.");
        functionMatches.put("parameters", parametersMatches);

        Map<String, Object> findMatchRequests = new HashMap<>();
        findMatchRequests.put("type", "function");
        findMatchRequests.put("function", functionMatches);
        return findMatchRequests;
    }

    @Override
    public Object executeTool(String toolName, String jsonArguments, Integer userId) {
        log.info("Executing tool: {} with arguments: {} for user: {}", toolName, jsonArguments, userId);
        try {
            JsonNode argsNode = objectMapper.readTree(jsonArguments);
            
            switch (toolName) {
                case "searchStadiums":
                    return handleSearchStadiums(argsNode);
                case "getStadiumSlots":
                    return handleGetStadiumSlots(argsNode);
                case "findMatchRequests":
                    return handleFindMatchRequests(argsNode);
                default:
                    throw new IllegalArgumentException("Unknown tool name: " + toolName);
            }
        } catch (Exception e) {
            log.error("Error executing tool: {}", toolName, e);
            return Map.of("error", "Failed to execute tool: " + e.getMessage());
        }
    }

    private Object handleSearchStadiums(JsonNode args) {
        StadiumSearchRequest.StadiumSearchRequestBuilder builder = StadiumSearchRequest.builder();

        if (args.hasNonNull("keyword")) {
            builder.keyword(args.get("keyword").asText());
        }

        Object sportNameError = applySportNameFilter(builder, args);
        if (sportNameError != null) {
            return sportNameError;
        }

        applyDistrictFilter(builder, args);
        applyPriceFilters(builder, args);
        applyDateTimeFilters(builder, args);

        builder.page(0);
        builder.size(10);

        StadiumSearchRequest searchRequest = builder.build();
        PageResponse<StadiumResponse> result = publicStadiumService.searchStadiums(searchRequest);

        if (result.getContent().isEmpty()) {
            List<StadiumResponse> fallback = findStadiumsByParentFacilityNameFallback(searchRequest.getKeyword());
            if (!fallback.isEmpty()) {
                return fallback;
            }
        }

        return result.getContent();
    }

    private Object applySportNameFilter(StadiumSearchRequest.StadiumSearchRequestBuilder builder, JsonNode args) {
        if (!args.hasNonNull("sportName")) {
            return null;
        }
        String rawSportName = args.get("sportName").asText();
        Integer sportTypeId = resolveSportTypeId(rawSportName);
        if (sportTypeId == null) {
            return Map.of("error", "Không tìm thấy loại môn thể thao: " + rawSportName + ". Hãy chọn một trong các môn thể thao được hỗ trợ.");
        }
        builder.sportTypeId(sportTypeId);
        return null;
    }

    private void applyDistrictFilter(StadiumSearchRequest.StadiumSearchRequestBuilder builder, JsonNode args) {
        if (!args.hasNonNull("district")) {
            return;
        }
        String rawDistrict = args.get("district").asText();
        String normalizedDistrict = normalizeDistrict(rawDistrict);
        builder.address(normalizedDistrict != null ? normalizedDistrict : rawDistrict);
    }

    private void applyPriceFilters(StadiumSearchRequest.StadiumSearchRequestBuilder builder, JsonNode args) {
        if (args.hasNonNull("minPrice")) {
            builder.minPrice(BigDecimal.valueOf(args.get("minPrice").asDouble()));
        }
        if (args.hasNonNull("maxPrice")) {
            builder.maxPrice(BigDecimal.valueOf(args.get("maxPrice").asDouble()));
        }
    }

    private void applyDateTimeFilters(StadiumSearchRequest.StadiumSearchRequestBuilder builder, JsonNode args) {
        if (args.hasNonNull("targetDate")) {
            try {
                builder.targetDate(LocalDate.parse(args.get("targetDate").asText()));
            } catch (Exception e) {
                log.warn("Invalid targetDate format", e);
            }
        }
        if (args.hasNonNull("startTime")) {
            try {
                builder.startTime(LocalTime.parse(args.get("startTime").asText(), DateTimeFormatter.ofPattern("HH:mm")));
            } catch (Exception e) {
                log.warn("Invalid startTime format", e);
            }
        }
        if (args.hasNonNull("endTime")) {
            try {
                builder.endTime(LocalTime.parse(args.get("endTime").asText(), DateTimeFormatter.ofPattern("HH:mm")));
            } catch (Exception e) {
                log.warn("Invalid endTime format", e);
            }
        }
    }

    /**
     * Người dùng chat thường gọi tên Facility cha (vd "Sân vận động Cẩm Lệ"), trong khi Court con
     * bên dưới lại đặt tên chung chung ("Sân 1") nên search theo keyword chính có thể rỗng dù sân
     * đó tồn tại thật. Thử lại theo tên Facility cha, rồi thử lại lần 2 không phân biệt dấu tiếng
     * Việt (model đôi khi tự đánh sai dấu không ổn định giữa các lần gọi).
     */
    private List<StadiumResponse> findStadiumsByParentFacilityNameFallback(String rawKeyword) {
        if (rawKeyword == null || rawKeyword.isBlank()) {
            return List.of();
        }

        List<Stadium> fallbackCourts = stadiumRepository.findCourtsByParentFacilityNameKeyword(rawKeyword);

        if (fallbackCourts.isEmpty()) {
            String strippedKeyword = stripDiacritics(rawKeyword).toLowerCase();
            List<Integer> matchedIds = stadiumRepository.findAllCourtFacilityNames().stream()
                    .filter(p -> p.getFacilityName() != null
                            && stripDiacritics(p.getFacilityName()).toLowerCase().contains(strippedKeyword))
                    .map(StadiumRepository.CourtFacilityNameProjection::getStadiumId)
                    .distinct()
                    .limit(10)
                    .toList();
            if (!matchedIds.isEmpty()) {
                fallbackCourts = stadiumRepository.findCourtsForAiToolByIds(matchedIds);
            }
        }

        return fallbackCourts.stream()
                .limit(10)
                .map(stadiumMapper::toResponse)
                .toList();
    }

    private Object handleGetStadiumSlots(JsonNode args) {
        if (!args.hasNonNull("stadiumId")) {
            return Map.of("error", "Missing required parameter: stadiumId");
        }
        int stadiumId = args.get("stadiumId").asInt();
        List<TimeSlotResponse> slots = timeSlotService.getSlotsByStadiumId(stadiumId);
        return slots;
    }

    private Object handleFindMatchRequests(JsonNode args) {
        int page = args.hasNonNull("page") ? args.get("page").asInt() : 0;
        int size = args.hasNonNull("size") ? args.get("size").asInt() : 10;

        String location = null;
        if (args.hasNonNull("location")) {
            String rawLocation = args.get("location").asText();
            String normalized = normalizeDistrict(rawLocation);
            location = normalized != null ? normalized : rawLocation;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<MatchResponse> matches = matchRequestService.getActiveMatches(pageable, location);
        return matches.getContent();
    }

    private Integer resolveSportTypeId(String sportName) {
        if (sportName == null || sportName.isBlank()) {
            return null;
        }
        
        List<SportType> sportTypes = sportTypeRepository.findAll();
        String searchKey = sportName.toLowerCase().trim();
        
        // Direct matching
        for (SportType st : sportTypes) {
            if (st.getSportName().toLowerCase().equals(searchKey) ||
                st.getSportCode().toLowerCase().equals(searchKey) ||
                (st.getNameEn() != null && st.getNameEn().toLowerCase().equals(searchKey))) {
                return st.getSportTypeId();
            }
        }
        
        // Fuzzy matching logic for common Vietnamese sports
        if (searchKey.contains("bóng đá") || searchKey.contains("da bong") || searchKey.contains("football") || searchKey.contains("soccer")) {
            return findSportTypeByCode(sportTypes, "FOOTBALL");
        }
        if (searchKey.contains("cầu lông") || searchKey.contains("cau long") || searchKey.contains("badminton")) {
            return findSportTypeByCode(sportTypes, "BADMINTON");
        }
        if (searchKey.contains("bóng rổ") || searchKey.contains("basketball")) {
            return findSportTypeByCode(sportTypes, "BASKETBALL");
        }
        if (searchKey.contains("volleyball") || searchKey.contains("bóng chuyền")) {
            return findSportTypeByCode(sportTypes, "VOLLEYBALL");
        }
        if (searchKey.contains("tennis") || searchKey.contains("quần vợt")) {
            return findSportTypeByCode(sportTypes, "TENNIS");
        }

        return null;
    }

    private Integer findSportTypeByCode(List<SportType> list, String code) {
        return list.stream()
                .filter(st -> st.getSportCode().equalsIgnoreCase(code))
                .map(SportType::getSportTypeId)
                .findFirst()
                .orElse(null);
    }

    private String normalizeDistrict(String district) {
        if (district == null || district.isBlank()) {
            return null;
        }
        
        String key = district.toLowerCase().replaceAll("\\s+", "").trim();
        
        // Mapping of key values to standardized HCM districts
        if (key.contains("thủđức") || key.contains("thuduc")) {
            return "Thủ Đức";
        }
        if (key.contains("bìnhthạnh") || key.contains("binhthanh")) {
            return "Bình Thạnh";
        }
        if (key.contains("phúnhuận") || key.contains("phunhuan")) {
            return "Phú Nhuận";
        }
        if (key.contains("gòvấp") || key.contains("govap")) {
            return "Gò Vấp";
        }
        if (key.contains("tânbình") || key.contains("tanbinh")) {
            return "Tân Bình";
        }
        if (key.contains("tânphú") || key.contains("tanphu")) {
            return "Tân Phú";
        }
        if (key.contains("bìnhtân") || key.contains("binhtan")) {
            return "Bình Tân";
        }
        if (key.contains("bìnhchánh") || key.contains("binhchanh")) {
            return "Bình Chánh";
        }
        if (key.contains("hócmôn") || key.contains("hocmon")) {
            return "Hóc Môn";
        }
        if (key.contains("củchi") || key.contains("cuchi")) {
            return "Củ Chi";
        }
        if (key.contains("nhàbè") || key.contains("nhabe")) {
            return "Nhà Bè";
        }
        if (key.contains("cầngiờ") || key.contains("cangio")) {
            return "Cần Giờ";
        }

        return matchDistrictNumber(key);
    }

    /** Handle District numbers (e.g., q1, q.1, quan 1, d1) — tách riêng để normalizeDistrict gọn hơn. */
    private String matchDistrictNumber(String key) {
        if (key.matches(".*(q|quận|district|d)\\.?0?1$") || key.equals("1")) {
            return "Quận 1";
        }
        if (key.matches(".*(q|quận|district|d)\\.?0?2$") || key.equals("2")) {
            return "Quận 2";
        }
        if (key.matches(".*(q|quận|district|d)\\.?0?3$") || key.equals("3")) {
            return "Quận 3";
        }
        if (key.matches(".*(q|quận|district|d)\\.?0?4$") || key.equals("4")) {
            return "Quận 4";
        }
        if (key.matches(".*(q|quận|district|d)\\.?0?5$") || key.equals("5")) {
            return "Quận 5";
        }
        if (key.matches(".*(q|quận|district|d)\\.?0?6$") || key.equals("6")) {
            return "Quận 6";
        }
        if (key.matches(".*(q|quận|district|d)\\.?0?7$") || key.equals("7")) {
            return "Quận 7";
        }
        if (key.matches(".*(q|quận|district|d)\\.?0?8$") || key.equals("8")) {
            return "Quận 8";
        }
        if (key.matches(".*(q|quận|district|d)\\.?0?9$") || key.equals("9")) {
            return "Quận 9";
        }
        if (key.matches(".*(q|quận|district|d)\\.?10$") || key.equals("10")) {
            return "Quận 10";
        }
        if (key.matches(".*(q|quận|district|d)\\.?11$") || key.equals("11")) {
            return "Quận 11";
        }
        if (key.matches(".*(q|quận|district|d)\\.?12$") || key.equals("12")) {
            return "Quận 12";
        }

        return null;
    }

    /**
     * Bỏ dấu tiếng Việt để so khớp không phân biệt dấu — Groq/Llama đôi khi tự đánh sai dấu
     * không ổn định giữa các lần gọi (vd "Sân vân đơng" thay vì "Sân vận động"), khiến LIKE
     * match diacritic-sensitive ở DB thất bại dù tên thật khớp về bản chất.
     */
    private String stripDiacritics(String input) {
        if (input == null) {
            return "";
        }
        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        String withoutMarks = normalized.replaceAll("\\p{M}", "");
        return withoutMarks.replace('đ', 'd').replace('Đ', 'D');
    }
}
