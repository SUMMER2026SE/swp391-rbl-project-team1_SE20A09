package com.sportvenue.service.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportvenue.dto.response.AiChatTurnResponse;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.service.BookingService;
import com.sportvenue.service.MaintenanceScheduleService;
import com.sportvenue.service.ai.AiConversationContextService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cho phần mới của Phase 2: resolve stadiumId qua targetIndex (tra lastShownResults cache)
 * thay vì để LLM tự bịa ID — xem docs/ai_chatbot_rebuild_plan.md mục 6.2.
 */
@ExtendWith(MockitoExtension.class)
class SlotAvailabilityHandlerTest {

    @Mock
    private StadiumRepository stadiumRepository;
    @Mock
    private BookingService bookingService;
    @Mock
    private MaintenanceScheduleService maintenanceScheduleService;
    @Mock
    private AiConversationContextService conversationContextService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SlotAvailabilityHandler handler() {
        return new SlotAvailabilityHandler(stadiumRepository, bookingService, maintenanceScheduleService,
                conversationContextService);
    }

    private JsonNode json(String raw) throws Exception {
        return objectMapper.readTree(raw);
    }

    @Test
    void targetIndex_resolvesRealStadiumId_fromConversationContext() throws Exception {
        Stadium court = Stadium.builder()
                .stadiumId(102)
                .nodeType(StadiumNodeType.COURT)
                .stadiumStatus(StadiumStatus.AVAILABLE)
                .build();
        when(conversationContextService.resolveStadiumIdByIndex(eq("s:test"), eq(0))).thenReturn(Optional.of(102));
        when(stadiumRepository.findById(102)).thenReturn(Optional.of(court));
        when(stadiumRepository.findCourtsForAiToolByIds(List.of(102))).thenReturn(List.of(court));
        when(maintenanceScheduleService.isStadiumUnderMaintenance(eq(court), any())).thenReturn(false);
        when(bookingService.getSlotsByDate(eq(102), any())).thenReturn(List.of());

        AiChatTurnResponse response = handler().handle(json("{\"targetIndex\":0}"), "...", "s:test");

        // Không có slot -> message rỗng xác định, nhưng KHÔNG phải lỗi "chưa xác định được sân"
        // (tức là đã resolve đúng ID 102 từ targetIndex, không bịa ID).
        assertThat(response.getMessage()).doesNotContain("Chưa xác định được sân");
        assertThat(response.getSlots()).isEmpty();
    }

    @Test
    void targetIndex_outOfCache_returnsAskToSearchFirst_insteadOfGuessing() throws Exception {
        when(conversationContextService.resolveStadiumIdByIndex(eq("s:test"), anyInt())).thenReturn(Optional.empty());

        AiChatTurnResponse response = handler().handle(json("{\"targetIndex\":3}"), "...", "s:test");

        assertThat(response.getMessage()).contains("Chưa xác định được sân");
        assertThat(response.getSlots()).isNull();
    }

    @Test
    void noStadiumIdAndNoTargetIndex_asksToSearchFirst() throws Exception {
        AiChatTurnResponse response = handler().handle(json("{}"), "...", "s:test");

        assertThat(response.getMessage()).contains("Chưa xác định được sân");
    }

    @Test
    void getStadiumSlots_stadiumNotFound_returnsError() throws Exception {
        when(stadiumRepository.findById(999)).thenReturn(Optional.empty());

        AiChatTurnResponse response = handler().handle(json("{\"stadiumId\":999}"), "...", "s:test");

        assertThat(response.getMessage()).contains("Không tìm thấy sân");
        assertThat(response.getSlots()).isNull();
    }

    @Test
    void getStadiumSlots_fabricatedIdZero_returnsGuidanceError() throws Exception {
        AiChatTurnResponse response = handler().handle(json("{\"stadiumId\":0}"), "...", "s:test");

        assertThat(response.getMessage()).contains("ID sân không hợp lệ");
        assertThat(response.getSlots()).isNull();
        verify(bookingService, never()).getSlotsByDate(anyInt(), any(LocalDate.class));
    }

    @Test
    void getStadiumSlots_facilityIdInsteadOfCourt_returnsGuardrailError() throws Exception {
        Stadium facility = Stadium.builder().stadiumId(218).nodeType(StadiumNodeType.FACILITY).build();
        when(stadiumRepository.findById(218)).thenReturn(Optional.of(facility));

        AiChatTurnResponse response = handler().handle(json("{\"stadiumId\":218}"), "...", "s:test");

        assertThat(response.getMessage()).contains("không phải là sân lẻ");
        assertThat(response.getSlots()).isNull();
        verify(bookingService, never()).getSlotsByDate(anyInt(), any(LocalDate.class));
    }

    @Test
    void getStadiumSlots_maintenanceStadium_returnsErrorWithoutSlots() throws Exception {
        Stadium court = Stadium.builder().stadiumId(219).nodeType(StadiumNodeType.COURT)
                .stadiumStatus(StadiumStatus.MAINTENANCE).build();
        when(stadiumRepository.findById(219)).thenReturn(Optional.of(court));

        AiChatTurnResponse response = handler().handle(json("{\"stadiumId\":219}"), "...", "s:test");

        assertThat(response.getMessage()).contains("bảo trì");
        assertThat(response.getSlots()).isNull();
        verify(bookingService, never()).getSlotsByDate(anyInt(), any(LocalDate.class));
    }

    @Test
    void getStadiumSlots_maintenanceScheduleActive_returnsErrorWithoutSlots() throws Exception {
        Stadium court = Stadium.builder().stadiumId(219).nodeType(StadiumNodeType.COURT)
                .stadiumStatus(StadiumStatus.AVAILABLE).build();
        when(stadiumRepository.findById(219)).thenReturn(Optional.of(court));
        when(stadiumRepository.findCourtsForAiToolByIds(List.of(219))).thenReturn(List.of(court));
        when(maintenanceScheduleService.isStadiumUnderMaintenance(eq(court), any(LocalDate.class))).thenReturn(true);

        AiChatTurnResponse response = handler().handle(json("{\"stadiumId\":219}"), "...", "s:test");

        assertThat(response.getMessage()).contains("lịch bảo trì");
        assertThat(response.getSlots()).isNull();
        verify(bookingService, never()).getSlotsByDate(anyInt(), any(LocalDate.class));
    }

    @Test
    void getStadiumSlots_validCourt_returnsSlots() throws Exception {
        Stadium court = Stadium.builder().stadiumId(219).nodeType(StadiumNodeType.COURT)
                .stadiumStatus(StadiumStatus.AVAILABLE).build();
        when(stadiumRepository.findById(219)).thenReturn(Optional.of(court));
        when(stadiumRepository.findCourtsForAiToolByIds(List.of(219))).thenReturn(List.of(court));
        when(maintenanceScheduleService.isStadiumUnderMaintenance(eq(court), any(LocalDate.class))).thenReturn(false);
        when(bookingService.getSlotsByDate(eq(219), any(LocalDate.class)))
                .thenReturn(List.of(TimeSlotResponse.builder().slotId(1).available(true).build()));

        AiChatTurnResponse response = handler().handle(json("{\"stadiumId\":219}"), "lời thoại", "s:test");

        assertThat(response.getSlots()).hasSize(1);
        assertThat(response.getMessage()).isEqualTo("lời thoại");
    }

    @Test
    void getStadiumSlots_today_filtersPastSlots() throws Exception {
        SlotAvailabilityHandler testHandler = handler();
        // Cố định giờ VN = 14:00 ngày 2026-07-07 (07:00 UTC) — slot 10h sáng phải bị loại
        testHandler.setClock(Clock.fixed(Instant.parse("2026-07-07T07:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));

        Stadium court = Stadium.builder().stadiumId(219).nodeType(StadiumNodeType.COURT)
                .stadiumStatus(StadiumStatus.AVAILABLE).build();
        when(stadiumRepository.findById(219)).thenReturn(Optional.of(court));
        when(stadiumRepository.findCourtsForAiToolByIds(List.of(219))).thenReturn(List.of(court));
        when(maintenanceScheduleService.isStadiumUnderMaintenance(eq(court), any(LocalDate.class))).thenReturn(false);
        when(bookingService.getSlotsByDate(219, LocalDate.of(2026, 7, 7))).thenReturn(List.of(
                TimeSlotResponse.builder().slotId(1).startTime(LocalTime.of(10, 0)).available(true).build(),
                TimeSlotResponse.builder().slotId(2).startTime(LocalTime.of(18, 0)).available(true).build()
        ));

        AiChatTurnResponse response = testHandler.handle(json("{\"stadiumId\":219}"), "lời thoại", "s:test");

        assertThat(response.getSlots()).hasSize(1);
        assertThat(response.getSlots().get(0).getSlotId()).isEqualTo(2);
    }

    @Test
    void getStadiumSlots_futureDate_keepsAllSlots() throws Exception {
        SlotAvailabilityHandler testHandler = handler();
        testHandler.setClock(Clock.fixed(Instant.parse("2026-07-07T07:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));

        Stadium court = Stadium.builder().stadiumId(219).nodeType(StadiumNodeType.COURT)
                .stadiumStatus(StadiumStatus.AVAILABLE).build();
        when(stadiumRepository.findById(219)).thenReturn(Optional.of(court));
        when(stadiumRepository.findCourtsForAiToolByIds(List.of(219))).thenReturn(List.of(court));
        when(maintenanceScheduleService.isStadiumUnderMaintenance(eq(court), any(LocalDate.class))).thenReturn(false);
        when(bookingService.getSlotsByDate(219, LocalDate.of(2026, 7, 8))).thenReturn(List.of(
                TimeSlotResponse.builder().slotId(1).startTime(LocalTime.of(10, 0)).available(true).build(),
                TimeSlotResponse.builder().slotId(2).startTime(LocalTime.of(18, 0)).available(false).build()
        ));

        AiChatTurnResponse response = testHandler.handle(json("{\"stadiumId\":219,\"date\":\"2026-07-08\"}"), "...", "s:test");

        assertThat(response.getSlots()).hasSize(2);
    }

    @Test
    void getStadiumSlots_pastDate_returnsError() throws Exception {
        SlotAvailabilityHandler testHandler = handler();
        testHandler.setClock(Clock.fixed(Instant.parse("2026-07-07T07:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));

        Stadium court = Stadium.builder().stadiumId(219).nodeType(StadiumNodeType.COURT)
                .stadiumStatus(StadiumStatus.AVAILABLE).build();
        when(stadiumRepository.findById(219)).thenReturn(Optional.of(court));

        AiChatTurnResponse response = testHandler.handle(json("{\"stadiumId\":219,\"date\":\"2026-07-06\"}"), "...", "s:test");

        assertThat(response.getMessage()).contains("đã qua");
        assertThat(response.getSlots()).isNull();
    }
}

