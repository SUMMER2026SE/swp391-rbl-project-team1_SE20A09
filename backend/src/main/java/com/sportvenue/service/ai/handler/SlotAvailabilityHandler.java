package com.sportvenue.service.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportvenue.dto.response.AiChatTurnResponse;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.service.BookingService;
import com.sportvenue.service.MaintenanceScheduleService;
import com.sportvenue.service.ai.AiConversationContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Xử lý intent "get_slots" — port từ CustomerAgentToolProvider.handleGetStadiumSlots (nhánh
 * ai-chatting cũ). Hỗ trợ tham chiếu "sân đầu tiên/thứ hai" qua targetIndex tra trong
 * lastShownResults cache (xem docs/ai_chatbot_rebuild_plan.md mục 6.2).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlotAvailabilityHandler {

    private final StadiumRepository stadiumRepository;
    private final BookingService bookingService;
    private final MaintenanceScheduleService maintenanceScheduleService;
    private final AiConversationContextService conversationContextService;

    private Clock clock = Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public AiChatTurnResponse handle(JsonNode args, String llmMessage, String conversationKey) {
        JsonNode[] argsHolder = new JsonNode[]{args};
        Integer stadiumId = resolveStadiumIdFromArgsOrContext(argsHolder, conversationKey);
        args = argsHolder[0];

        if (stadiumId == null || stadiumId <= 0) {
            return errorResponse(stadiumId == null 
                ? "Chưa xác định được sân bạn muốn xem — hãy tìm sân trước (vd hỏi \"tìm sân bóng đá ở Thủ Đức\") rồi hỏi lại giờ trống."
                : "ID sân không hợp lệ. Hãy tìm sân trước để lấy đúng sân bạn muốn xem giờ trống.");
        }

        Optional<Stadium> stadiumOpt = stadiumRepository.findById(stadiumId);
        if (stadiumOpt.isEmpty()) {
            return errorResponse("Không tìm thấy sân với ID " + stadiumId + ". Hãy tìm sân trước để lấy đúng ID.");
        }
        Stadium court = stadiumOpt.get();
        AiChatTurnResponse validationError = validateStadium(court);
        if (validationError != null) {
            return validationError;
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate requestedDate = resolveDate(args, today);
        String formattedDate = requestedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        if (requestedDate.isBefore(today)) {
            return errorResponse("Ngày " + formattedDate + " đã qua, không thể xem giờ trống cho ngày trong quá khứ.");
        }

        Stadium detailedCourt = stadiumRepository.findCourtsForAiToolByIds(List.of(stadiumId)).stream()
                .findFirst().orElse(court);
        if (maintenanceScheduleService.isStadiumUnderMaintenance(detailedCourt, requestedDate)) {
            return errorResponse("Sân này có lịch bảo trì vào ngày " + formattedDate + ", không thể đặt. Bạn có thể chọn ngày khác hoặc sân khác.");
        }

        List<TimeSlotResponse> slots = bookingService.getSlotsByDate(stadiumId, requestedDate);
        if (requestedDate.isEqual(today)) {
            LocalTime nowVietnam = LocalTime.now(clock);
            slots = slots.stream()
                    .filter(slot -> slot.getStartTime() == null || slot.getStartTime().isAfter(nowVietnam))
                    .toList();
        }

        if (slots.isEmpty()) {
            return AiChatTurnResponse.builder()
                    .message("Không có khung giờ nào cho sân này vào ngày " + formattedDate + ".")
                    .intent("get_slots")
                    .slots(List.of())
                    .build();
        }

        conversationContextService.saveLastShownSlots(conversationKey,
                slots.stream().map(TimeSlotResponse::getSlotId).toList());
        conversationContextService.saveCurrentStadiumId(conversationKey, stadiumId);

        return AiChatTurnResponse.builder()
                .message(llmMessage)
                .intent("get_slots")
                .slots(slots)
                .build();
    }

    private Integer resolveStadiumIdFromArgsOrContext(JsonNode[] argsHolder, String conversationKey) {
        JsonNode args = argsHolder[0];
        if (args == null || args.isNull() || args.isMissingNode()) {
            if (conversationKey != null) {
                Integer contextStadiumId = conversationContextService.getCurrentStadiumId(conversationKey).orElse(null);
                if (contextStadiumId != null) {
                    com.fasterxml.jackson.databind.node.ObjectNode newArgs = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
                    newArgs.put("stadiumId", contextStadiumId);
                    argsHolder[0] = newArgs;
                    log.info("Bug #4: Lấy stadiumId {} từ context cho get_slots", contextStadiumId);
                    return contextStadiumId;
                }
            }
            return null;
        }
        return resolveStadiumId(args, conversationKey);
    }

    private AiChatTurnResponse validateStadium(Stadium stadium) {
        if (stadium.getNodeType() != StadiumNodeType.COURT) {
            return errorResponse("Sân này không phải là sân lẻ có thể đặt lịch. Hãy tìm sân cụ thể trước khi tra giờ trống.");
        }
        if (stadium.getStadiumStatus() != StadiumStatus.AVAILABLE) {
            return errorResponse("Sân này hiện đang bảo trì hoặc tạm đóng cửa, không thể đặt lịch. Bạn có thể tìm sân khác.");
        }
        return null;
    }

    private LocalDate resolveDate(JsonNode args, LocalDate today) {
        if (args != null && args.hasNonNull("date")) {
            try {
                return LocalDate.parse(args.get("date").asText());
            } catch (Exception e) {
                log.warn("Invalid date format for get_slots: {}", args.get("date").asText());
            }
        }
        return today;
    }

    private AiChatTurnResponse errorResponse(String message) {
        return AiChatTurnResponse.builder().message(message).intent("get_slots").build();
    }

    /**
     * Ưu tiên stadiumId tường minh. Nếu thiếu nhưng có targetIndex (LLM nhận diện "sân đầu
     * tiên/thứ hai" từ câu người dùng), tra ID thật theo thứ tự đã hiển thị ở lượt search gần
     * nhất — KHÔNG bao giờ để LLM tự đoán ID (xem docs/ai_chatbot_rebuild_plan.md mục 6.2).
     */
    private Integer resolveStadiumId(JsonNode args, String conversationKey) {
        if (args.hasNonNull("stadiumId")) {
            return args.get("stadiumId").asInt();
        }
        if (args.hasNonNull("targetIndex")) {
            int targetIndex = args.get("targetIndex").asInt();
            return conversationContextService.resolveStadiumIdByIndex(conversationKey, targetIndex).orElse(null);
        }
        return null;
    }
}
