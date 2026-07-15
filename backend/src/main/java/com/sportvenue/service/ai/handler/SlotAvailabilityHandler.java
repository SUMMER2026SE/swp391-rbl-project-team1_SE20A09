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

    void setClock(Clock clock) {
        this.clock = clock;
    }

    public AiChatTurnResponse handle(JsonNode args, String llmMessage, String conversationKey) {
        if (args == null || args.isNull() || args.isMissingNode()) {
            // Bug #4: Thử lấy stadium từ context trước khi yêu cầu user tìm lại
            if (conversationKey != null) {
                Integer contextStadiumId = conversationContextService.getCurrentStadiumId(conversationKey).orElse(null);
                if (contextStadiumId != null) {
                    // Tái tạo args với stadiumId từ context
                    com.fasterxml.jackson.databind.node.ObjectNode newArgs = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
                    newArgs.put("stadiumId", contextStadiumId);
                    args = newArgs;
                    log.info("Bug #4: Lấy stadiumId {} từ context cho get_slots", contextStadiumId);
                }
            }
            if (args == null || args.isNull()) {
                return errorResponse("Chưa xác định được sân bạn muốn xem — hãy tìm sân trước (vd hỏi \"tìm sân bóng đá ở Thủ Đức\") rồi hỏi lại giờ trống.");
            }
        }

        Integer stadiumId = resolveStadiumId(args, conversationKey);
        if (stadiumId == null) {
            return errorResponse("Chưa xác định được sân bạn muốn xem — hãy tìm sân trước (vd hỏi \"tìm sân bóng đá ở Thủ Đức\") rồi hỏi lại giờ trống.");
        }
        // Model từng tự bịa ID 0 khi search rỗng — chặn sớm với hướng dẫn rõ ràng.
        if (stadiumId <= 0) {
            return errorResponse("ID sân không hợp lệ. Hãy tìm sân trước để lấy đúng sân bạn muốn xem giờ trống.");
        }

        // Guardrail: model đôi khi tự bịa/nhầm ID (vd truyền ID của Facility cha thay vì Court
        // con) — validate đúng loại trước khi query, tránh trả mảng rỗng gây hiểu lầm "sân
        // không có slot" trong khi thực ra ID không hợp lệ cho thao tác này.
        Optional<Stadium> stadiumOpt = stadiumRepository.findById(stadiumId);
        if (stadiumOpt.isEmpty()) {
            return errorResponse("Không tìm thấy sân với ID " + stadiumId + ". Hãy tìm sân trước để lấy đúng ID.");
        }
        if (stadiumOpt.get().getNodeType() != StadiumNodeType.COURT) {
            return errorResponse("Sân này không phải là sân lẻ có thể đặt lịch. Hãy tìm sân cụ thể trước khi tra giờ trống.");
        }
        if (stadiumOpt.get().getStadiumStatus() != StadiumStatus.AVAILABLE) {
            return errorResponse("Sân này hiện đang bảo trì hoặc tạm đóng cửa, không thể đặt lịch. Bạn có thể tìm sân khác.");
        }

        // Mặc định hôm nay theo giờ Việt Nam — không dùng giờ hệ thống (server Docker thường
        // chạy UTC, lệch 7 tiếng).
        LocalDate today = LocalDate.now(clock);
        LocalDate requestedDate = today;
        if (args.hasNonNull("date")) {
            try {
                requestedDate = LocalDate.parse(args.get("date").asText());
            } catch (Exception e) {
                log.warn("Invalid date format for get_slots: {}", args.get("date").asText());
            }
        }

        // Định dạng ngày dd/MM/yyyy cho user-friendly
        String formattedDate = requestedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        if (requestedDate.isBefore(today)) {
            return errorResponse("Ngày " + formattedDate + " đã qua, không thể xem giờ trống cho ngày trong quá khứ.");
        }

        // Sân có lịch bảo trì (MaintenanceSchedule) trùm ngày này — kể cả khi stadiumStatus vẫn
        // AVAILABLE (bảo trì theo khung ngày cố tình không đổi status).
        Stadium court = stadiumRepository.findCourtsForAiToolByIds(List.of(stadiumId)).stream()
                .findFirst().orElse(stadiumOpt.get());
        if (maintenanceScheduleService.isStadiumUnderMaintenance(court, requestedDate)) {
            return errorResponse("Sân này có lịch bảo trì vào ngày " + formattedDate + ", không thể đặt. Bạn có thể chọn ngày khác hoặc sân khác.");
        }

        // Availability THẬT theo ngày: getSlotsByDate đối chiếu booking (PENDING/CONFIRMED),
        // TimeSlotException (đóng/đổi giờ/đổi giá) — không phải khung giờ mẫu tĩnh.
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

        // Lưu slot IDs để user có thể chọn theo thứ tự khi đặt sân, đồng thời lưu lại sân hiện tại
        conversationContextService.saveLastShownSlots(conversationKey,
                slots.stream().map(TimeSlotResponse::getSlotId).toList());
        conversationContextService.saveCurrentStadiumId(conversationKey, stadiumId);

        return AiChatTurnResponse.builder()
                .message(llmMessage)
                .intent("get_slots")
                .slots(slots)
                .build();
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
