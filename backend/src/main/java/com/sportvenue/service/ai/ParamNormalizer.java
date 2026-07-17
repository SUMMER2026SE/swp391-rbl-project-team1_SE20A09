package com.sportvenue.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParamNormalizer {
    
    private final ObjectMapper objectMapper;
    
    private static final Map<String, String> SPORT_MAP = new HashMap<>();

    static {
        SPORT_MAP.put("đá banh", "Bóng đá");
        SPORT_MAP.put("bóng đá", "Bóng đá");
        SPORT_MAP.put("football", "Bóng đá");
        SPORT_MAP.put("cầu lông", "Cầu lông");
        SPORT_MAP.put("badminton", "Cầu lông");
        SPORT_MAP.put("tennis", "Tennis");
        SPORT_MAP.put("quần vợt", "Tennis");
        SPORT_MAP.put("bơi", "Bơi lội");
        SPORT_MAP.put("bơi lội", "Bơi lội");
        SPORT_MAP.put("bóng rổ", "Bóng rổ");
        SPORT_MAP.put("basketball", "Bóng rổ");
        SPORT_MAP.put("bóng chuyền", "Bóng chuyền");
        SPORT_MAP.put("volleyball", "Bóng chuyền");
        SPORT_MAP.put("pickleball", "Pickleball");
    }
    
    private static final Map<String, String> DISTRICT_MAP = new HashMap<>();

    static {
        DISTRICT_MAP.put("q1", "Quận 1");
        DISTRICT_MAP.put("quận 1", "Quận 1");
        DISTRICT_MAP.put("q2", "Quận 2");
        DISTRICT_MAP.put("quận 2", "Quận 2");
        DISTRICT_MAP.put("q3", "Quận 3");
        DISTRICT_MAP.put("quận 3", "Quận 3");
        DISTRICT_MAP.put("q4", "Quận 4");
        DISTRICT_MAP.put("quận 4", "Quận 4");
        DISTRICT_MAP.put("q5", "Quận 5");
        DISTRICT_MAP.put("quận 5", "Quận 5");
        DISTRICT_MAP.put("q6", "Quận 6");
        DISTRICT_MAP.put("quận 6", "Quận 6");
        DISTRICT_MAP.put("q7", "Quận 7");
        DISTRICT_MAP.put("quận 7", "Quận 7");
        DISTRICT_MAP.put("q8", "Quận 8");
        DISTRICT_MAP.put("quận 8", "Quận 8");
        DISTRICT_MAP.put("q9", "Quận 9");
        DISTRICT_MAP.put("quận 9", "Quận 9");
        DISTRICT_MAP.put("q10", "Quận 10");
        DISTRICT_MAP.put("quận 10", "Quận 10");
        DISTRICT_MAP.put("q11", "Quận 11");
        DISTRICT_MAP.put("quận 11", "Quận 11");
        DISTRICT_MAP.put("q12", "Quận 12");
        DISTRICT_MAP.put("quận 12", "Quận 12");
        DISTRICT_MAP.put("thủ đức", "Thủ Đức");
        DISTRICT_MAP.put("thu duc", "Thủ Đức");
        DISTRICT_MAP.put("bình thạnh", "Bình Thạnh");
        DISTRICT_MAP.put("binh thanh", "Bình Thạnh");
        DISTRICT_MAP.put("gò vấp", "Gò Vấp");
        DISTRICT_MAP.put("go vap", "Gò Vấp");
        DISTRICT_MAP.put("phú nhuận", "Phú Nhuận");
        DISTRICT_MAP.put("phu nhuan", "Phú Nhuận");
        DISTRICT_MAP.put("tân bình", "Tân Bình");
        DISTRICT_MAP.put("tan binh", "Tân Bình");
        DISTRICT_MAP.put("tân phú", "Tân Phú");
        DISTRICT_MAP.put("tan phu", "Tân Phú");
        DISTRICT_MAP.put("bình tân", "Bình Tân");
        DISTRICT_MAP.put("binh tan", "Bình Tân");
        
        // Da Nang
        DISTRICT_MAP.put("hải châu", "Hải Châu");
        DISTRICT_MAP.put("hai chau", "Hải Châu");
        DISTRICT_MAP.put("thanh khê", "Thanh Khê");
        DISTRICT_MAP.put("thanh khe", "Thanh Khê");
        DISTRICT_MAP.put("sơn trà", "Sơn Trà");
        DISTRICT_MAP.put("son tra", "Sơn Trà");
        DISTRICT_MAP.put("ngũ hành sơn", "Ngũ Hành Sơn");
        DISTRICT_MAP.put("ngu hanh son", "Ngũ Hành Sơn");
        DISTRICT_MAP.put("liên chiểu", "Liên Chiểu");
        DISTRICT_MAP.put("lien chieu", "Liên Chiểu");
        DISTRICT_MAP.put("cẩm lệ", "Cẩm Lệ");
        DISTRICT_MAP.put("cam le", "Cẩm Lệ");
        DISTRICT_MAP.put("hoà vang", "Hòa Vang");
        DISTRICT_MAP.put("hòa vang", "Hòa Vang");
        DISTRICT_MAP.put("hoa vang", "Hòa Vang");
        DISTRICT_MAP.put("hoàng sa", "Hoàng Sa");
        DISTRICT_MAP.put("hoang sa", "Hoàng Sa");
    }
    
    public ExtractedIntentResult normalize(ExtractedIntentResult input) {
        JsonNode params = input.getParams();
        if (params == null || !params.isObject()) {
            return input;
        }
        
        ObjectNode mutableParams = (ObjectNode) params;
        boolean changed = false;
        
        // Normalize sportName
        if (mutableParams.hasNonNull("sportName")) {
            String sport = mutableParams.get("sportName").asText().toLowerCase().trim();
            for (Map.Entry<String, String> entry : SPORT_MAP.entrySet()) {
                if (sport.contains(entry.getKey())) {
                    mutableParams.put("sportName", entry.getValue());
                    changed = true;
                    break;
                }
            }
        }
        
        // Normalize district
        if (mutableParams.hasNonNull("district")) {
            String district = mutableParams.get("district").asText().toLowerCase().trim();
            for (Map.Entry<String, String> entry : DISTRICT_MAP.entrySet()) {
                if (district.contains(entry.getKey())) {
                    mutableParams.put("district", entry.getValue());
                    changed = true;
                    break;
                }
            }
        }
        
        if (changed) {
            input.setParams(mutableParams);
        }
        
        return input;
    }
}
