package com.sportvenue.service.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportvenue.dto.response.AiChatTurnResponse;
import com.sportvenue.dto.response.DraftBookingResponse;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.BookingService;
import com.sportvenue.service.MaintenanceScheduleService;
import com.sportvenue.service.ai.AiConversationContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Xử lý intent "create_booking" — đặt sân trực tiếp từ chat.
 * Yêu cầu: người dùng phải đăng nhập và đã xác định được sân + slot + ngày.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingHandler {

    private final BookingService bookingService;
    private final StadiumRepository stadiumRepository;
    private final UserRepository userRepository;
    private final com.sportvenue.repository.BookingRepository bookingRepository;
    private final MaintenanceScheduleService maintenanceScheduleService;
    private final AiConversationContextService conversationContextService;

    private Clock clock = Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));

    void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Xử lý yêu cầu đặt sân từ chat.
     *
     * @param args JSON params từ LLM với các trường:
     *             - stadiumId (Integer): ID sân, hoặc
     *             - targetIndex (Integer): chỉ số sân đã hiển thị gần nhất (0-based)
     *             - slotId (Integer): ID khung giờ, hoặc
     *             - slotIndex (Integer): chỉ số khung giờ đã hiển thị (0-based)
     *             - date (String): ngày đặt (YYYY-MM-DD), mặc định hôm nay
     *             - note (String, optional): ghi chú cho sân
     * @param llmMessage message từ LLM
     * @param conversationKey key của cuộc hội thoại
     * @param userId ID của user đang đăng nhập
     * @return AiChatTurnResponse chứa kết quả đặt sân
     */
    public AiChatTurnResponse handle(JsonNode args, String llmMessage, String conversationKey, Integer userId) {
        if (userId == null) {
            return errorResponse("Bạn cần đăng nhập để đặt sân qua chat. Vui lòng đăng nhập và thử lại.");
        }

        if (args == null || args.isNull() || args.isMissingNode()) {
            return errorResponse("Chưa xác định được sân bạn muốn đặt. Bạn muốn đặt sân nào? (Vd: sân bóng đá Thủ Đức)");
        }

        Integer stadiumId = resolveStadiumId(args, conversationKey);
        if (stadiumId == null) {
            if (args.hasNonNull("targetIndex") && conversationContextService.resolveStadiumIdByIndex(conversationKey, args.get("targetIndex").asInt()).isEmpty()) {
                return systemBugResponse("Không thể xác định được sân từ lịch sử chat. Vui lòng chọn lại sân bạn muốn đặt.");
            }
            return errorResponse("Chưa xác định được sân bạn muốn đặt. Bạn muốn đặt sân nào? (Vd: sân bóng đá Thủ Đức)");
        }
        if (stadiumId <= 0) {
            return errorResponse("ID sân không hợp lệ. Hãy tìm sân trước để lấy đúng sân bạn muốn đặt.");
        }

        // Validate stadium exists and is bookable
        Optional<Stadium> stadiumOpt = stadiumRepository.findById(stadiumId);
        if (stadiumOpt.isEmpty()) {
            return errorResponse("Không tìm thấy sân với ID " + stadiumId + ". Hãy tìm sân trước.");
        }
        Stadium stadium = stadiumOpt.get();
        if (stadium.getNodeType() != StadiumNodeType.COURT) {
            return errorResponse("Sân này không phải là sân lẻ có thể đặt lịch. Hãy chọn một sân cụ thể.");
        }
        if (stadium.getStadiumStatus() != StadiumStatus.AVAILABLE) {
            return errorResponse("Sân này hiện không khả dụng để đặt (đang bảo trì hoặc tạm đóng). Bạn có thể tìm sân khác.");
        }

        // Resolve date
        LocalDate requestedDate = resolveDate(args);
        if (requestedDate.isBefore(LocalDate.now(clock))) {
            return errorResponse("Không thể đặt sân cho ngày trong quá khứ.");
        }

        // Check maintenance
        if (maintenanceScheduleService.isStadiumUnderMaintenance(stadium, requestedDate)) {
            return errorResponse("Sân này có lịch bảo trì vào ngày " + requestedDate + ", không thể đặt. Bạn có thể chọn ngày khác hoặc sân khác.");
        }

        // Resolve slot
        List<TimeSlotResponse> slots = bookingService.getSlotsByDate(stadiumId, requestedDate);
        TimeSlotResponse targetSlot = resolveSlot(args, conversationKey, slots);

        if (targetSlot == null) {
            if (args.hasNonNull("slotIndex") && conversationContextService.resolveSlotIdByIndex(conversationKey, args.get("slotIndex").asInt()).isEmpty()) {
                return systemBugResponse("Không thể xác định được khung giờ từ lịch sử chat. Vui lòng hỏi lại giờ trống.");
            }
            return errorResponse("Chưa xác định được khung giờ bạn muốn đặt. Bạn muốn đặt lúc mấy giờ? (Vd: 2h chiều thứ 7)");
        }

        AiChatTurnResponse availabilityResponse = checkSlotAvailability(targetSlot, slots, requestedDate, conversationKey, stadiumId);
        if (availabilityResponse != null) {
            return availabilityResponse;
        }

        AiChatTurnResponse duplicateResponse = checkDuplicateBookings(userId, targetSlot.getSlotId(), requestedDate);
        if (duplicateResponse != null) {
            return duplicateResponse;
        }

        // Tạo Draft Booking thay vì gọi createBooking
        DraftBookingResponse draft = DraftBookingResponse.builder()
                .stadiumId(stadium.getStadiumId())
                .stadiumName(stadium.getStadiumName())
                .date(requestedDate.toString())
                .startTime(targetSlot.getStartTime().toString())
                .price(targetSlot.getPricePerSlot())
                .build();

        return AiChatTurnResponse.builder()
                .message("Thông tin đặt sân đã sẵn sàng. Vui lòng kiểm tra lại và bấm nút bên dưới để tiến hành thanh toán.")
                .intent("confirm_booking") // Intent mới để UI hiển thị thẻ xác nhận
                .draftBooking(draft)
                .build();
    }



    private LocalDate resolveDate(JsonNode args) {
        if (args.hasNonNull("date")) {
            try {
                return LocalDate.parse(args.get("date").asText());
            } catch (Exception e) {
                log.warn("Invalid date format: {}", args.get("date").asText());
            }
        }
        return LocalDate.now(clock);
    }

    private Integer resolveStadiumId(JsonNode args, String conversationKey) {
        Integer stadiumId = null;
        if (args.hasNonNull("stadiumId")) {
            stadiumId = args.get("stadiumId").asInt();
        } else if (args.hasNonNull("targetIndex")) {
            int targetIndex = args.get("targetIndex").asInt();
            stadiumId = conversationContextService.resolveStadiumIdByIndex(conversationKey, targetIndex).orElse(null);
            if (stadiumId == null) {
                log.warn("Resolve context failed for stadiumId. conversationKey={}, targetIndex={}", conversationKey, targetIndex);
            }
        } else if (args.hasNonNull("keyword")) {
            String keyword = args.get("keyword").asText().toLowerCase();
            List<Integer> lastShown = conversationContextService.getLastShownStadiumIds(conversationKey);
            if (lastShown != null && !lastShown.isEmpty()) {
                List<Stadium> recentStadiums = stadiumRepository.findAllById(lastShown);
                stadiumId = recentStadiums.stream()
                        .filter(s -> s.getStadiumName().toLowerCase().contains(keyword))
                        .findFirst()
                        .map(Stadium::getStadiumId)
                        .orElse(null);
            }
        }
        
        if (stadiumId == null) {
            stadiumId = conversationContextService.getCurrentStadiumId(conversationKey).orElse(null);
        }
        return stadiumId;
    }

    private TimeSlotResponse resolveSlot(JsonNode args, String conversationKey, List<TimeSlotResponse> slots) {
        if (args.hasNonNull("slotId")) {
            int sid = args.get("slotId").asInt();
            return slots.stream().filter(s -> s.getSlotId() != null && s.getSlotId() == sid).findFirst().orElse(null);
        } else if (args.hasNonNull("slotIndex")) {
            int slotIndex = args.get("slotIndex").asInt();
            Integer resolvedId = conversationContextService.resolveSlotIdByIndex(conversationKey, slotIndex).orElse(null);
            if (resolvedId != null) {
                return slots.stream().filter(s -> s.getSlotId() != null && s.getSlotId().equals(resolvedId)).findFirst().orElse(null);
            } else {
                log.warn("Resolve context failed for slotId. conversationKey={}, slotIndex={}", conversationKey, slotIndex);
                return null;
            }
        } else if (args.hasNonNull("startTime")) {
            try {
                java.time.LocalTime targetStartTime = java.time.LocalTime.parse(args.get("startTime").asText());
                return slots.stream()
                        .filter(s -> s.getStartTime() != null && s.getStartTime().equals(targetStartTime))
                        .findFirst().orElse(null);
            } catch (Exception e) {
                log.warn("Invalid startTime format: {}", e.getMessage());
            }
        }
        return null;
    }

    private AiChatTurnResponse checkSlotAvailability(TimeSlotResponse targetSlot, List<TimeSlotResponse> slots, LocalDate requestedDate, String conversationKey, Integer stadiumId) {
        if (!Boolean.TRUE.equals(targetSlot.getAvailable())) {
            if (requestedDate.isEqual(LocalDate.now(clock))) {
                java.time.LocalTime nowVietnam = java.time.LocalTime.now(clock);
                slots = slots.stream().filter(s -> s.getStartTime() != null && s.getStartTime().isAfter(nowVietnam)).toList();
            }
            if (slots.stream().noneMatch(s -> Boolean.TRUE.equals(s.getAvailable()))) {
                return AiChatTurnResponse.builder()
                        .message("Sân này hiện đã kín lịch trong ngày " + requestedDate + ". Vui lòng chọn ngày khác.")
                        .intent("get_slots")
                        .slots(slots)
                        .build();
            } else {
                conversationContextService.saveLastShownSlots(conversationKey, slots.stream().map(TimeSlotResponse::getSlotId).toList());
                conversationContextService.saveCurrentStadiumId(conversationKey, stadiumId);
                return AiChatTurnResponse.builder()
                        .message("Khung giờ bạn chọn hiện không có sẵn hoặc đã có người đặt. Đây là các giờ còn trống trong ngày " + requestedDate + " để bạn chọn:")
                        .intent("get_slots")
                        .slots(slots)
                        .build();
            }
        }
        return null;
    }

    private AiChatTurnResponse checkDuplicateBookings(Integer userId, Integer slotId, LocalDate requestedDate) {
        List<com.sportvenue.entity.enums.BookingStatus> pendingStatuses = List.of(
                com.sportvenue.entity.enums.BookingStatus.PENDING,
                com.sportvenue.entity.enums.BookingStatus.PENDING_PAYMENT
        );
        List<com.sportvenue.entity.Booking> duplicateBookings = bookingRepository.findUserActiveBookingsForSlot(
                userId, slotId, requestedDate, pendingStatuses);

        if (!duplicateBookings.isEmpty()) {
            com.sportvenue.entity.Booking dup = duplicateBookings.get(0);
            return AiChatTurnResponse.builder()
                    .message(String.format(
                            "Bạn đã có một đơn đặt sân (Mã: BK%06d) đang chờ thanh toán cho khung giờ này rồi. Bạn có muốn xem lại và thanh toán không?",
                            dup.getBookingId()
                    ))
                    .intent("create_booking")
                    .bookingId(dup.getBookingId())
                    .build();
        }
        return null;
    }

    private AiChatTurnResponse errorResponse(String message) {
        return AiChatTurnResponse.builder()
                .message(message)
                .intent("create_booking")
                .build();
    }

    private AiChatTurnResponse systemBugResponse(String message) {
        return AiChatTurnResponse.builder()
                .message(message)
                .intent("system_bug_context_resolve")
                .build();
    }
}
