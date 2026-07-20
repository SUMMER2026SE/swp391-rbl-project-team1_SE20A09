package com.sportvenue.service.ai.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sportvenue.dto.response.AiChatTurnResponse;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.BookingService;
import com.sportvenue.util.RelativeDateParser;
import com.sportvenue.service.MaintenanceScheduleService;
import com.sportvenue.service.ai.AiConversationContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Test cho fallback search trong BookingHandler.
 * Các test case single-turn booking không qua search trước.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingHandlerFallbackTest {

    @Mock
    private BookingService bookingService;
    @Mock
    private StadiumRepository stadiumRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private MaintenanceScheduleService maintenanceScheduleService;
    @Mock
    private SportTypeRepository sportTypeRepository;
    @Mock
    private StringRedisTemplate redisTemplate;

    private AiConversationContextService conversationContextService;
    private BookingHandler bookingHandler;
    private UserPrincipal userPrincipal;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);

        objectMapper = new ObjectMapper();
        conversationContextService = new AiConversationContextService(redisTemplate, objectMapper);

        RelativeDateParser dateParser = new RelativeDateParser();
        bookingHandler = new BookingHandler(
                bookingService, stadiumRepository, userRepository,
                bookingRepository, maintenanceScheduleService, conversationContextService,
                sportTypeRepository, dateParser);

        java.time.Clock fixedClock = java.time.Clock.fixed(
                java.time.Instant.parse("2026-07-15T01:00:00Z"),
                java.time.ZoneId.of("Asia/Ho_Chi_Minh")
        );
        bookingHandler.setClock(fixedClock);
        dateParser.setClock(fixedClock);

        User mockUser = new User();
        mockUser.setUserId(1);
        mockUser.setEmail("user@test.com");
        userPrincipal = new UserPrincipal(mockUser);
    }

    /**
     * Test case 1: Sân bóng đá 5 người 19h thứ 7
     * Input: "đặt sân bóng đá 5 người lúc 7h tối thứ 7"
     * Expected: Tìm được sân bóng đá, trả về danh sách hoặc 1 sân cụ thể
     */
    @Test
    void testSingleTurnFootballBooking_Saturday7PM() throws JsonProcessingException {
        // Mock LLM params: intent=create_booking với sportName và date/time
        // Simulate: user nói "đặt sân bóng đá 5 người lúc 7h tối thứ 7"
        // LLM sẽ extract thành: sportName="Bóng đá", date="2026-07-18", startTime="19:00"

        // Setup sport type
        SportType footballType = new SportType();
        footballType.setSportTypeId(1);
        footballType.setSportName("Bóng đá");
        footballType.setSportCode("FOOTBALL");
        lenient().when(sportTypeRepository.findBySportName("Bóng đá"))
                .thenReturn(Optional.of(footballType));
        lenient().when(sportTypeRepository.findAll()).thenReturn(List.of(footballType));

        // Mock stadium search by sport type
        Stadium stadium1 = createTestStadium(101, "Sân Bóng Thủ Đức 1", footballType);
        Stadium stadium2 = createTestStadium(102, "Sân Bóng Thủ Đức 2", footballType);

        Page<Stadium> stadiumPage = mock(Page.class);
        lenient().when(stadiumPage.getContent()).thenReturn(List.of(stadium1, stadium2));
        lenient().when(stadiumRepository.findBySportTypeSportTypeIdAndStadiumStatus(eq(1), eq(StadiumStatus.AVAILABLE), any(Pageable.class)))
                .thenReturn(stadiumPage);

        // Mock stadiums lookup
        lenient().when(stadiumRepository.findCourtsForAiToolByIds(any())).thenReturn(List.of(stadium1, stadium2));

        // Mock slot availability - cả 2 sân đều có slot trống
        TimeSlotResponse slot1 = createSlot(1, LocalTime.of(19, 0), true);
        TimeSlotResponse slot2 = createSlot(2, LocalTime.of(20, 0), true);
        lenient().when(bookingService.getSlotsByDate(eq(101), any())).thenReturn(List.of(slot1, slot2));
        lenient().when(bookingService.getSlotsByDate(eq(102), any())).thenReturn(List.of(slot1, slot2));

        // Mock maintenance check
        lenient().when(maintenanceScheduleService.isStadiumUnderMaintenance(any(), any())).thenReturn(false);

        // Mock user repository
        lenient().when(userRepository.findById(1)).thenReturn(Optional.of(new User()));

        // Build args as LLM would return (sử dụng JSON string thay vì ObjectNode)
        JsonNode args = objectMapper.readTree("{\"sportName\":\"Bóng đá\",\"date\":\"2026-07-18\",\"startTime\":\"19:00\"}");

        // Execute
        AiChatTurnResponse response = bookingHandler.handle(args, "Để mình kiểm tra", "test:case1", 1);

        // Verify
        assertThat(response).isNotNull();
        // Với 2 sân còn trống, nên trả về danh sách để user chọn
        assertThat(response.getIntent()).isEqualTo("search_stadiums");
        assertThat(response.getMessage()).contains("sân phù hợp");
        assertThat(response.getStadiums()).isNotNull();
        assertThat(response.getStadiums()).hasSize(2);
    }

    /**
     * Test case 2: Sân bóng rổ chủ nhật 9h sáng
     * Input: "đặt sân bóng rổ sáng chủ nhật 9h"
     * Expected: Tìm được sân bóng rổ với slot 9h sáng
     */
    @Test
    void testSingleTurnBasketballBooking_Sunday9AM() throws JsonProcessingException {
        // Setup sport type
        SportType basketballType = new SportType();
        basketballType.setSportTypeId(2);
        basketballType.setSportName("Bóng rổ");
        basketballType.setSportCode("BASKETBALL");
        lenient().when(sportTypeRepository.findBySportName("Bóng rổ"))
                .thenReturn(Optional.of(basketballType));
        lenient().when(sportTypeRepository.findAll()).thenReturn(List.of(basketballType));

        // Mock stadium search
        Stadium stadium1 = createTestStadium(201, "Sân Bóng Rổ Quận 1", basketballType);

        Page<Stadium> stadiumPage = mock(Page.class);
        lenient().when(stadiumPage.getContent()).thenReturn(List.of(stadium1));
        lenient().when(stadiumRepository.findBySportTypeSportTypeIdAndStadiumStatus(eq(2), eq(StadiumStatus.AVAILABLE), any(Pageable.class)))
                .thenReturn(stadiumPage);

        lenient().when(stadiumRepository.findCourtsForAiToolByIds(any())).thenReturn(List.of(stadium1));
        lenient().when(stadiumRepository.findById(201)).thenReturn(Optional.of(stadium1));

        // Mock slot availability - có slot 9h trống
        TimeSlotResponse slot9AM = createSlot(9, LocalTime.of(9, 0), true);
        TimeSlotResponse slot10AM = createSlot(10, LocalTime.of(10, 0), true);
        lenient().when(bookingService.getSlotsByDate(eq(201), any())).thenReturn(List.of(slot9AM, slot10AM));

        lenient().when(maintenanceScheduleService.isStadiumUnderMaintenance(any(), any())).thenReturn(false);
        lenient().when(userRepository.findById(1)).thenReturn(Optional.of(new User()));

        // Build args
        JsonNode args = objectMapper.readTree("{\"sportName\":\"Bóng rổ\",\"date\":\"2026-07-19\",\"startTime\":\"09:00\"}");

        // Execute
        AiChatTurnResponse response = bookingHandler.handle(args, "OK", "test:case2", 1);

        // Verify
        assertThat(response).isNotNull();
        // 1 sân có slot trống → tiếp tục flow đến confirm_booking vì có slot 9h trống
        assertThat(response.getIntent()).isEqualTo("confirm_booking");
        assertThat(response.getDraftBooking()).isNotNull();
    }

    /**
     * Test case 3: Sân bóng đá 7 người 20/7 18h-20h
     * Input: "đặt sân bóng đá 7 người ngày 20/7 từ 6h đến 8h tối"
     * Expected: Tìm sân bóng đá còn trống trong khoảng 18h-20h
     */
    @Test
    void testSingleTurnFootballBooking_July20_18h20h() throws JsonProcessingException {
        // Setup sport type
        SportType footballType = new SportType();
        footballType.setSportTypeId(1);
        footballType.setSportName("Bóng đá");
        footballType.setSportCode("FOOTBALL");
        lenient().when(sportTypeRepository.findBySportName("Bóng đá"))
                .thenReturn(Optional.of(footballType));
        lenient().when(sportTypeRepository.findAll()).thenReturn(List.of(footballType));

        // Mock stadium search
        Stadium stadium1 = createTestStadium(301, "Sân Bóng Cẩm Lệ", footballType);

        Page<Stadium> stadiumPage = mock(Page.class);
        lenient().when(stadiumPage.getContent()).thenReturn(List.of(stadium1));
        lenient().when(stadiumRepository.findBySportTypeSportTypeIdAndStadiumStatus(eq(1), eq(StadiumStatus.AVAILABLE), any(Pageable.class)))
                .thenReturn(stadiumPage);

        lenient().when(stadiumRepository.findCourtsForAiToolByIds(any())).thenReturn(List.of(stadium1));
        lenient().when(stadiumRepository.findById(301)).thenReturn(Optional.of(stadium1));

        // Mock slot availability - có slot 18h trống
        TimeSlotResponse slot18h = createSlot(18, LocalTime.of(18, 0), true);
        TimeSlotResponse slot19h = createSlot(19, LocalTime.of(19, 0), false); // Đã đặt
        TimeSlotResponse slot20h = createSlot(20, LocalTime.of(20, 0), true);
        lenient().when(bookingService.getSlotsByDate(eq(301), any())).thenReturn(List.of(slot18h, slot19h, slot20h));

        lenient().when(maintenanceScheduleService.isStadiumUnderMaintenance(any(), any())).thenReturn(false);
        lenient().when(userRepository.findById(1)).thenReturn(Optional.of(new User()));

        // Build args với endTime
        JsonNode args = objectMapper.readTree("{\"sportName\":\"Bóng đá\",\"date\":\"2026-07-20\",\"startTime\":\"18:00\",\"endTime\":\"20:00\"}");

        // Execute
        AiChatTurnResponse response = bookingHandler.handle(args, "OK", "test:case3", 1);

        // Verify
        assertThat(response).isNotNull();
        // 1 sân có slot trong khoảng 18h-20h → tiếp tục flow đến confirm_booking
        assertThat(response.getIntent()).isEqualTo("confirm_booking");
        assertThat(response.getDraftBooking()).isNotNull();
    }

    /**
     * Test: Không có sportName và keyword → không search được → trả message yêu cầu cung cấp thêm
     */
    @Test
    void testNoSportNameOrKeyword_ReturnsNeedMoreInfo() throws JsonProcessingException {
        // Build args không có sportName hay keyword
        JsonNode args = objectMapper.readTree("{\"date\":\"2026-07-18\"}");

        // Execute
        AiChatTurnResponse response = bookingHandler.handle(args, "OK", "test:noinfo", 1);

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("Chưa xác định được sân");
    }

    /**
     * Test: Tìm được sân nhưng không có slot trống → báo hết chỗ
     */
    @Test
    void testNoAvailableSlots_ReturnsNoAvailabilityMessage() throws JsonProcessingException {
        // Setup sport type
        SportType footballType = new SportType();
        footballType.setSportTypeId(1);
        footballType.setSportName("Bóng đá");
        footballType.setSportCode("FOOTBALL");
        lenient().when(sportTypeRepository.findBySportName("Bóng đá"))
                .thenReturn(Optional.of(footballType));
        lenient().when(sportTypeRepository.findAll()).thenReturn(List.of(footballType));

        // Mock stadium search
        Stadium stadium1 = createTestStadium(401, "Sân Bóng Kín", footballType);

        Page<Stadium> stadiumPage = mock(Page.class);
        lenient().when(stadiumPage.getContent()).thenReturn(List.of(stadium1));
        lenient().when(stadiumRepository.findBySportTypeSportTypeIdAndStadiumStatus(eq(1), eq(StadiumStatus.AVAILABLE), any(Pageable.class)))
                .thenReturn(stadiumPage);

        lenient().when(stadiumRepository.findCourtsForAiToolByIds(any())).thenReturn(List.of(stadium1));

        // Mock slot availability - tất cả đã đặt
        TimeSlotResponse slot1 = createSlot(1, LocalTime.of(17, 0), false);
        TimeSlotResponse slot2 = createSlot(2, LocalTime.of(18, 0), false);
        lenient().when(bookingService.getSlotsByDate(eq(401), any())).thenReturn(List.of(slot1, slot2));

        // Build args
        JsonNode args = objectMapper.readTree("{\"sportName\":\"Bóng đá\",\"date\":\"2026-07-18\",\"startTime\":\"17:00\"}");

        // Execute
        AiChatTurnResponse response = bookingHandler.handle(args, "OK", "test:nocapacity", 1);

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.getIntent()).isEqualTo("search_stadiums");
        assertThat(response.getMessage()).contains("không có sân nào còn trống");
    }

    // Helper methods

    private Stadium createTestStadium(int id, String name, SportType sportType) {
        Stadium stadium = new Stadium();
        stadium.setStadiumId(id);
        stadium.setStadiumName(name);
        stadium.setSportType(sportType);
        stadium.setNodeType(StadiumNodeType.COURT);
        stadium.setStadiumStatus(StadiumStatus.AVAILABLE);
        stadium.setPricePerHour(BigDecimal.valueOf(150000));
        return stadium;
    }

    private TimeSlotResponse createSlot(int slotId, LocalTime startTime, boolean available) {
        return TimeSlotResponse.builder()
                .slotId(slotId)
                .startTime(startTime)
                .endTime(startTime.plusHours(1))
                .pricePerSlot(BigDecimal.valueOf(150000))
                .available(available)
                .build();
    }
}
