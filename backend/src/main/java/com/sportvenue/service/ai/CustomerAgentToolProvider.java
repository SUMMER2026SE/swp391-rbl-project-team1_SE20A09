package com.sportvenue.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.dto.response.MatchResponse;
import com.sportvenue.entity.SportType;
import com.sportvenue.repository.SportTypeRepository;
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
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAgentToolProvider implements AgentToolProvider {

    private final PublicStadiumService publicStadiumService;
    private final TimeSlotService timeSlotService;
    private final MatchRequestService matchRequestService;
    private final SportTypeRepository sportTypeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public List<Map<String, Object>> getToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // 1. searchStadiums tool definition
        Map<String, Object> searchStadiums = new HashMap<>();
        searchStadiums.put("type", "function");
        
        Map<String, Object> functionSearch = new HashMap<>();
        functionSearch.put("name", "searchStadiums");
        functionSearch.put("description", "Tìm kiếm các sân thể thao dựa trên các điều kiện lọc như môn thể thao, quận/khu vực, khoảng giá, và thời gian.");
        
        Map<String, Object> parametersSearch = new HashMap<>();
        parametersSearch.put("type", "object");
        
        Map<String, Object> propertiesSearch = new HashMap<>();
        
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
            "description", "Giờ bắt đầu dưới định dạng HH:mm."
        ));
        
        propertiesSearch.put("endTime", Map.of(
            "type", "string",
            "description", "Giờ kết thúc dưới định dạng HH:mm."
        ));
        
        parametersSearch.put("properties", propertiesSearch);
        functionSearch.put("parameters", parametersSearch);
        searchStadiums.put("function", functionSearch);
        tools.add(searchStadiums);

        // 2. getStadiumSlots tool definition
        Map<String, Object> getStadiumSlots = new HashMap<>();
        getStadiumSlots.put("type", "function");
        
        Map<String, Object> functionSlots = new HashMap<>();
        functionSlots.put("name", "getStadiumSlots");
        functionSlots.put("description", "Lấy các khung giờ hoạt động hoặc trạng thái trống của một sân thể thao cụ thể theo ID sân.");
        
        Map<String, Object> parametersSlots = new HashMap<>();
        parametersSlots.put("type", "object");
        
        Map<String, Object> propertiesSlots = new HashMap<>();
        propertiesSlots.put("stadiumId", Map.of(
            "type", "integer",
            "description", "ID của sân đấu cần lấy khung giờ."
        ));
        
        parametersSlots.put("properties", propertiesSlots);
        parametersSlots.put("required", List.of("stadiumId"));
        functionSlots.put("parameters", parametersSlots);
        getStadiumSlots.put("function", functionSlots);
        tools.add(getStadiumSlots);

        // 3. findMatchRequests tool definition
        Map<String, Object> findMatchRequests = new HashMap<>();
        findMatchRequests.put("type", "function");
        
        Map<String, Object> functionMatches = new HashMap<>();
        functionMatches.put("name", "findMatchRequests");
        functionMatches.put("description", "Tìm kiếm danh sách các kèo ghép thể thao (matchmaking) đang mở và cần tuyển người chơi.");
        
        Map<String, Object> parametersMatches = new HashMap<>();
        parametersMatches.put("type", "object");
        
        Map<String, Object> propertiesMatches = new HashMap<>();
        propertiesMatches.put("page", Map.of(
            "type", "integer",
            "description", "Số thứ tự trang kết quả (mặc định 0)."
        ));
        propertiesMatches.put("size", Map.of(
            "type", "integer",
            "description", "Số lượng phần tử mỗi trang (mặc định 10)."
        ));
        
        parametersMatches.put("properties", propertiesMatches);
        functionMatches.put("parameters", parametersMatches);
        findMatchRequests.put("function", functionMatches);
        tools.add(findMatchRequests);

        return tools;
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
        
        // 1. Sport Name validation & conversion to SportTypeId
        if (args.hasNonNull("sportName")) {
            String rawSportName = args.get("sportName").asText();
            Integer sportTypeId = resolveSportTypeId(rawSportName);
            if (sportTypeId != null) {
                builder.sportTypeId(sportTypeId);
            } else {
                return Map.of("error", "Không tìm thấy loại môn thể thao: " + rawSportName + ". Hãy chọn một trong các môn thể thao được hỗ trợ.");
            }
        }

        // 2. District normalization & fuzzy matching
        if (args.hasNonNull("district")) {
            String rawDistrict = args.get("district").asText();
            String normalizedDistrict = normalizeDistrict(rawDistrict);
            if (normalizedDistrict != null) {
                builder.address(normalizedDistrict);
            } else {
                builder.address(rawDistrict); // Fallback to raw text if no match
            }
        }

        // 3. Price mapping
        if (args.hasNonNull("minPrice")) {
            builder.minPrice(BigDecimal.valueOf(args.get("minPrice").asDouble()));
        }
        if (args.hasNonNull("maxPrice")) {
            builder.maxPrice(BigDecimal.valueOf(args.get("maxPrice").asDouble()));
        }

        // 4. Date and Time parsing
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

        builder.page(0);
        builder.size(10);

        StadiumSearchRequest searchRequest = builder.build();
        PageResponse<StadiumResponse> result = publicStadiumService.searchStadiums(searchRequest);
        return result.getContent();
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
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MatchResponse> matches = matchRequestService.getActiveMatches(pageable);
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
        if (key.contains("thủđức") || key.contains("thuduc")) return "Thủ Đức";
        if (key.contains("bìnhthạnh") || key.contains("binhthanh")) return "Bình Thạnh";
        if (key.contains("phúnhuận") || key.contains("phunhuan")) return "Phú Nhuận";
        if (key.contains("gòvấp") || key.contains("govap")) return "Gò Vấp";
        if (key.contains("tânbình") || key.contains("tanbinh")) return "Tân Bình";
        if (key.contains("tânphú") || key.contains("tanphu")) return "Tân Phú";
        if (key.contains("bìnhtân") || key.contains("binhtan")) return "Bình Tân";
        if (key.contains("bìnhchánh") || key.contains("binhchanh")) return "Bình Chánh";
        if (key.contains("hócmôn") || key.contains("hocmon")) return "Hóc Môn";
        if (key.contains("củchi") || key.contains("cuchi")) return "Củ Chi";
        if (key.contains("nhàbè") || key.contains("nhabe")) return "Nhà Bè";
        if (key.contains("cầngiờ") || key.contains("cangio")) return "Cần Giờ";
        
        // Handle District numbers (e.g., q1, q.1, quan 1, d1)
        if (key.matches(".*(q|quận|district|d)\\.?0?1$") || key.equals("1")) return "Quận 1";
        if (key.matches(".*(q|quận|district|d)\\.?0?2$") || key.equals("2")) return "Quận 2";
        if (key.matches(".*(q|quận|district|d)\\.?0?3$") || key.equals("3")) return "Quận 3";
        if (key.matches(".*(q|quận|district|d)\\.?0?4$") || key.equals("4")) return "Quận 4";
        if (key.matches(".*(q|quận|district|d)\\.?0?5$") || key.equals("5")) return "Quận 5";
        if (key.matches(".*(q|quận|district|d)\\.?0?6$") || key.equals("6")) return "Quận 6";
        if (key.matches(".*(q|quận|district|d)\\.?0?7$") || key.equals("7")) return "Quận 7";
        if (key.matches(".*(q|quận|district|d)\\.?0?8$") || key.equals("8")) return "Quận 8";
        if (key.matches(".*(q|quận|district|d)\\.?0?9$") || key.equals("9")) return "Quận 9";
        if (key.matches(".*(q|quận|district|d)\\.?10$") || key.equals("10")) return "Quận 10";
        if (key.matches(".*(q|quận|district|d)\\.?11$") || key.equals("11")) return "Quận 11";
        if (key.matches(".*(q|quận|district|d)\\.?12$") || key.equals("12")) return "Quận 12";
        
        return null;
    }
}
