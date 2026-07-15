package com.sportvenue.service.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportvenue.dto.response.AiChatTurnResponse;
import com.sportvenue.dto.response.MatchResponse;
import com.sportvenue.entity.SportType;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.service.MatchRequestService;
import com.sportvenue.service.ai.AiConversationContextService;
import com.sportvenue.util.location.VietnamLocationResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Xử lý intent "find_match" — port từ CustomerAgentToolProvider.handleFindMatchRequests
 * (nhánh ai-chatting cũ).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchRequestHandler {

    private final MatchRequestService matchRequestService;
    private final SportTypeRepository sportTypeRepository;
    private final VietnamLocationResolver locationResolver;
    private final AiConversationContextService conversationContextService;

    /** Mặc định 5, chặn trần 10 — card UI trong khung chat không bị quá dài. */
    private static final int DEFAULT_SIZE = 5;
    private static final int MAX_SIZE = 10;

    public AiChatTurnResponse handle(JsonNode args, String llmMessage) {
        return handle(args, llmMessage, null);
    }

    public AiChatTurnResponse handle(JsonNode args, String llmMessage, String conversationKey) {
        if (args == null || args.isNull() || args.isMissingNode()) {
            Pageable pageable = PageRequest.of(0, DEFAULT_SIZE);
            Page<MatchResponse> matches = matchRequestService.getActiveMatches(pageable, null, null);
            if (matches.isEmpty()) {
                return AiChatTurnResponse.builder()
                        .message("Hiện tại chưa có kèo ghép nào đang mở.")
                        .intent("find_match")
                        .matches(List.of())
                        .build();
            }
            if (conversationKey != null) {
                conversationContextService.saveLastShownMatches(conversationKey, matches.getContent().stream().map(MatchResponse::getMatchId).toList());
            }
            return AiChatTurnResponse.builder()
                    .message(llmMessage != null && !llmMessage.isBlank() ? llmMessage : "Dưới đây là một số kèo ghép hiện đang mở:")
                    .intent("find_match")
                    .matches(matches.getContent())
                    .build();
        }

        int page = args.hasNonNull("page") ? args.get("page").asInt() : 0;
        int size = args.hasNonNull("size") ? Math.min(args.get("size").asInt(), MAX_SIZE) : DEFAULT_SIZE;

        // Bug #3: Lấy location từ context province nếu không có location trong args
        String location = null;
        if (args.hasNonNull("location")) {
            String rawLocation = args.get("location").asText();
            VietnamLocationResolver.LocationMatch match = locationResolver.deriveFromAddress(rawLocation);
            if (match.district() != null) {
                location = match.district();
            } else if (match.province() != null) {
                location = match.province();
            } else {
                location = rawLocation;
            }
        } else if (conversationKey != null) {
            // Bug #3: Fallback sang context province khi không có location
            location = conversationContextService.getContext(conversationKey)
                    .filter(ctx -> ctx.getCurrentProvince() != null)
                    .map(ctx -> {
                        log.info("Merged province from context for match search: {}", ctx.getCurrentProvince());
                        return ctx.getCurrentProvince();
                    })
                    .orElse(null);
        }

        Integer sportTypeId = null;
        if (args.hasNonNull("sportName")) {
            String rawSportName = args.get("sportName").asText();
            sportTypeId = resolveSportTypeId(rawSportName);
            if (sportTypeId == null) {
                List<String> supportedSports = sportTypeRepository.findAll().stream()
                        .map(SportType::getSportName)
                        .toList();
                return AiChatTurnResponse.builder()
                        .message("Không tìm thấy môn thể thao \"" + rawSportName + "\" trong hệ thống. Các môn hiện có: "
                                + String.join(", ", supportedSports) + ".")
                        .intent("find_match")
                        .build();
            }
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<MatchResponse> matches = matchRequestService.getActiveMatches(pageable, location, sportTypeId);

        // Bug #7: Hiển thị rõ tiêu chí đã lọc khi không tìm thấy kèo
        if (matches.isEmpty()) {
            StringBuilder noMatchMessage = new StringBuilder("Chưa tìm thấy kèo ghép nào");
            if (location != null) {
                noMatchMessage.append(" ở ").append(location);
            }
            if (sportTypeId != null) {
                // Lấy tên sport từ repository
                String sportName = sportTypeRepository.findById(sportTypeId)
                        .map(SportType::getSportName)
                        .orElse("môn này");
                noMatchMessage.append(" cho ").append(sportName);
            }
            noMatchMessage.append(". Bạn có thể thử đổi khu vực hoặc môn thể thao.");
            return AiChatTurnResponse.builder()
                    .message(noMatchMessage.toString())
                    .intent("find_match")
                    .matches(List.of())
                    .build();
        }

        // Lưu match IDs để user có thể chọn theo thứ tự khi tham gia kèo
        if (conversationKey != null) {
            List<Integer> matchIds = matches.getContent().stream()
                    .map(MatchResponse::getMatchId)
                    .toList();
            conversationContextService.saveLastShownMatches(conversationKey, matchIds);
        }

        return AiChatTurnResponse.builder()
                .message(llmMessage)
                .intent("find_match")
                .matches(matches.getContent())
                .build();
    }

    /** So khớp không phân biệt dấu tiếng Việt — dùng chung alias với StadiumSearchHandler. */
    private Integer resolveSportTypeId(String sportName) {
        if (sportName == null || sportName.isBlank()) {
            return null;
        }
        List<SportType> sportTypes = sportTypeRepository.findAll();
        String searchKey = locationResolver.stripDiacritics(sportName).toLowerCase().trim();

        for (SportType st : sportTypes) {
            if (locationResolver.stripDiacritics(st.getSportName()).toLowerCase().equals(searchKey) ||
                    st.getSportCode().toLowerCase().equals(searchKey) ||
                    (st.getNameEn() != null && st.getNameEn().toLowerCase().equals(searchKey))) {
                return st.getSportTypeId();
            }
        }
        if (searchKey.contains("bong da") || searchKey.contains("da bong") || searchKey.contains("da banh")
                || searchKey.contains("football") || searchKey.contains("soccer") || searchKey.contains("futsal")) {
            return findSportTypeByCode(sportTypes, "FOOTBALL");
        }
        if (searchKey.contains("cau long") || searchKey.contains("badminton")) {
            return findSportTypeByCode(sportTypes, "BADMINTON");
        }
        if (searchKey.contains("bong ro") || searchKey.contains("basketball")) {
            return findSportTypeByCode(sportTypes, "BASKETBALL");
        }
        if (searchKey.contains("bong chuyen") || searchKey.contains("volleyball")) {
            return findSportTypeByCode(sportTypes, "VOLLEYBALL");
        }
        if (searchKey.contains("tennis") || searchKey.contains("quan vot")) {
            return findSportTypeByCode(sportTypes, "TENNIS");
        }
        if (searchKey.contains("pickleball") || searchKey.contains("pickle ball")) {
            return findSportTypeByCode(sportTypes, "PICKLEBALL");
        }
        if (searchKey.contains("bong ban") || searchKey.contains("table tennis") || searchKey.contains("ping pong")) {
            return findSportTypeByCode(sportTypes, "TABLE_TENNIS");
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
}
