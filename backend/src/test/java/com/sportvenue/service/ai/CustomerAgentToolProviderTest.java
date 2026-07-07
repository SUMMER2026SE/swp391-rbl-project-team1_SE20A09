package com.sportvenue.service.ai;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.MatchResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.mapper.StadiumMapper;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.service.BookingService;
import com.sportvenue.service.MaintenanceScheduleService;
import com.sportvenue.service.MatchRequestService;
import com.sportvenue.service.PublicStadiumService;
import com.sportvenue.util.location.VietnamLocationResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAgentToolProviderTest {

    @Mock
    private PublicStadiumService publicStadiumService;

    @Mock
    private BookingService bookingService;

    @Mock
    private MaintenanceScheduleService maintenanceScheduleService;

    @Mock
    private MatchRequestService matchRequestService;

    @Mock
    private SportTypeRepository sportTypeRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private StadiumMapper stadiumMapper;

    private CustomerAgentToolProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CustomerAgentToolProvider(
                publicStadiumService, bookingService, maintenanceScheduleService,
                matchRequestService, sportTypeRepository, stadiumRepository, stadiumMapper,
                new VietnamLocationResolver());
    }

    @Test
    void getRoleName_returnsCustomer() {
        assertEquals("Customer", provider.getRoleName());
    }

    @Test
    void getSystemPrompt_loggedInUser_noGuestSuffix() {
        String prompt = provider.getSystemPrompt(42);
        assertFalse(prompt.contains("khách vãng lai"));
    }

    @Test
    void getSystemPrompt_guest_includesGuestSuffix() {
        String prompt = provider.getSystemPrompt(null);
        assertTrue(prompt.contains("khách vãng lai"));
    }

    @Test
    void getToolDefinitions_returnsAllThreeTools() {
        List<Map<String, Object>> tools = provider.getToolDefinitions();

        assertEquals(3, tools.size());
        List<String> names = tools.stream()
                .map(t -> (Map<?, ?>) t.get("function"))
                .map(f -> (String) f.get("name"))
                .toList();
        assertTrue(names.containsAll(List.of("searchStadiums", "getStadiumSlots", "findMatchRequests")));
    }

    @Test
    void executeTool_searchStadiums_unknownSportName_returnsError() {
        when(sportTypeRepository.findAll()).thenReturn(List.of());

        Object result = provider.executeTool("searchStadiums", "{\"sportName\":\"Không tồn tại\"}", 1);

        assertInstanceOf(Map.class, result);
        assertTrue(((Map<?, ?>) result).get("error").toString().contains("Không tìm thấy môn thể thao"));
    }

    @Test
    void executeTool_searchStadiums_resolvesSportNameAndDistrict() {
        SportType football = SportType.builder().sportTypeId(1).sportName("Bóng đá").sportCode("FOOTBALL").build();
        when(sportTypeRepository.findAll()).thenReturn(List.of(football));

        PageResponse<StadiumResponse> pageResponse = PageResponse.<StadiumResponse>builder()
                .content(List.of(StadiumResponse.builder().stadiumId(1).stadiumName("Sân Thủ Đức").build()))
                .pageNumber(0).pageSize(10).totalElements(1).totalPages(1).last(true)
                .build();
        when(publicStadiumService.searchStadiums(any())).thenReturn(pageResponse);

        Object result = provider.executeTool("searchStadiums", "{\"sportName\":\"Bóng đá\",\"district\":\"Q9\"}", 1);

        assertInstanceOf(List.class, result);
        assertEquals(1, ((List<?>) result).size());

        ArgumentCaptor<StadiumSearchRequest> captor = ArgumentCaptor.forClass(StadiumSearchRequest.class);
        verify(publicStadiumService).searchStadiums(captor.capture());
        assertEquals(1, captor.getValue().getSportTypeId());
        assertEquals("Quận 9", captor.getValue().getDistrict());
    }

    @Test
    void executeTool_searchStadiums_districtWithWrongDiacritics_stillNormalizes() {
        PageResponse<StadiumResponse> pageResponse = PageResponse.<StadiumResponse>builder()
                .content(List.of()).pageNumber(0).pageSize(10).totalElements(0).totalPages(0).last(true).build();
        when(publicStadiumService.searchStadiums(any())).thenReturn(pageResponse);

        // Model tự đánh sai dấu ("Thù Đùc" thay vì "Thủ Đức") — trước đây so khớp chính xác dấu
        // sẽ thất bại và rơi vào nhánh fallback dùng nguyên văn sai.
        provider.executeTool("searchStadiums", "{\"district\":\"Thù Đùc\"}", 1);

        ArgumentCaptor<StadiumSearchRequest> captor = ArgumentCaptor.forClass(StadiumSearchRequest.class);
        verify(publicStadiumService).searchStadiums(captor.capture());
        assertEquals("Thủ Đức", captor.getValue().getDistrict());
    }

    @Test
    void executeTool_findMatchRequests_locationWithWrongDiacritics_stillNormalizes() {
        Page<MatchResponse> page = new PageImpl<>(List.of());
        when(matchRequestService.getActiveMatches(any(Pageable.class), eq("Bình Thạnh"), eq((Integer) null))).thenReturn(page);

        provider.executeTool("findMatchRequests", "{\"location\":\"Bính Thánh\"}", 1);

        verify(matchRequestService).getActiveMatches(any(Pageable.class), eq("Bình Thạnh"), eq((Integer) null));
    }

    @Test
    void executeTool_searchStadiums_emptyResult_fallsBackToParentFacilityName() {
        PageResponse<StadiumResponse> emptyResponse = PageResponse.<StadiumResponse>builder()
                .content(List.of()).pageNumber(0).pageSize(10).totalElements(0).totalPages(0).last(true).build();
        when(publicStadiumService.searchStadiums(any())).thenReturn(emptyResponse);

        Stadium court = Stadium.builder().stadiumId(219).stadiumName("Sân 1").nodeType(StadiumNodeType.COURT).build();
        when(stadiumRepository.findCourtsByParentFacilityNameKeyword("Sân vận động Cẩm Lệ")).thenReturn(List.of(court));
        when(stadiumMapper.toResponse(court)).thenReturn(StadiumResponse.builder().stadiumId(219).stadiumName("Sân 1").build());

        Object result = provider.executeTool("searchStadiums", "{\"keyword\":\"Sân vận động Cẩm Lệ\"}", 1);

        assertInstanceOf(List.class, result);
        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        assertEquals(219, ((StadiumResponse) list.get(0)).getStadiumId());
    }

    @Test
    void executeTool_searchStadiums_emptyResult_diacriticInsensitiveFallback() {
        PageResponse<StadiumResponse> emptyResponse = PageResponse.<StadiumResponse>builder()
                .content(List.of()).pageNumber(0).pageSize(10).totalElements(0).totalPages(0).last(true).build();
        when(publicStadiumService.searchStadiums(any())).thenReturn(emptyResponse);
        when(stadiumRepository.findCourtsByParentFacilityNameKeyword(anyString())).thenReturn(List.of());

        StadiumRepository.CourtFacilityNameProjection projection = mock(StadiumRepository.CourtFacilityNameProjection.class);
        when(projection.getStadiumId()).thenReturn(219);
        when(projection.getFacilityName()).thenReturn("Sân vận động Cẩm Lệ");
        when(stadiumRepository.findAllCourtFacilityNames()).thenReturn(List.of(projection));

        Stadium court = Stadium.builder().stadiumId(219).stadiumName("Sân 1").nodeType(StadiumNodeType.COURT).build();
        when(stadiumRepository.findCourtsForAiToolByIds(List.of(219))).thenReturn(List.of(court));
        when(stadiumMapper.toResponse(court)).thenReturn(StadiumResponse.builder().stadiumId(219).build());

        // Model gõ sai dấu ("San van dong Cam Le" không dấu) khác chuỗi lưu trong DB có dấu đầy đủ
        Object result = provider.executeTool("searchStadiums", "{\"keyword\":\"San van dong Cam Le\"}", 1);

        assertInstanceOf(List.class, result);
        assertEquals(1, ((List<?>) result).size());
    }

    @Test
    void executeTool_getStadiumSlots_missingStadiumId_returnsError() {
        Object result = provider.executeTool("getStadiumSlots", "{}", 1);
        assertTrue(((Map<?, ?>) result).containsKey("error"));
    }

    @Test
    void executeTool_getStadiumSlots_stadiumNotFound_returnsError() {
        when(stadiumRepository.findById(999)).thenReturn(Optional.empty());

        Object result = provider.executeTool("getStadiumSlots", "{\"stadiumId\":999}", 1);

        assertTrue(((Map<?, ?>) result).get("error").toString().contains("Không tìm thấy sân"));
    }

    @Test
    void executeTool_getStadiumSlots_facilityIdInsteadOfCourt_returnsGuardrailError() {
        Stadium facility = Stadium.builder().stadiumId(218).nodeType(StadiumNodeType.FACILITY).build();
        when(stadiumRepository.findById(218)).thenReturn(Optional.of(facility));

        Object result = provider.executeTool("getStadiumSlots", "{\"stadiumId\":218}", 1);

        assertTrue(((Map<?, ?>) result).get("error").toString().contains("không phải là sân lẻ"));
        verify(bookingService, never()).getSlotsByDate(anyInt(), any(LocalDate.class));
    }

    @Test
    void executeTool_getStadiumSlots_validCourt_returnsSlots() {
        Stadium court = Stadium.builder().stadiumId(219).nodeType(StadiumNodeType.COURT).build();
        when(stadiumRepository.findById(219)).thenReturn(Optional.of(court));
        when(bookingService.getSlotsByDate(eq(219), any(LocalDate.class)))
                .thenReturn(List.of(TimeSlotResponse.builder().slotId(1).available(true).build()));

        Object result = provider.executeTool("getStadiumSlots", "{\"stadiumId\":219}", 1);

        assertInstanceOf(List.class, result);
        assertEquals(1, ((List<?>) result).size());
    }

    @Test
    void executeTool_findMatchRequests_normalizesLocationAndDelegates() {
        Page<MatchResponse> page = new PageImpl<>(List.of(MatchResponse.builder().matchId(1).build()));
        when(matchRequestService.getActiveMatches(any(Pageable.class), eq("Thủ Đức"), eq((Integer) null))).thenReturn(page);

        Object result = provider.executeTool("findMatchRequests", "{\"location\":\"Thu Duc\"}", 1);

        assertInstanceOf(List.class, result);
        assertEquals(1, ((List<?>) result).size());
    }

    @Test
    void executeTool_findMatchRequests_noLocation_passesNull() {
        Page<MatchResponse> page = new PageImpl<>(List.of());
        when(matchRequestService.getActiveMatches(any(Pageable.class), eq((String) null), eq((Integer) null))).thenReturn(page);

        provider.executeTool("findMatchRequests", "{}", 1);

        verify(matchRequestService).getActiveMatches(any(Pageable.class), eq((String) null), eq((Integer) null));
    }

    @Test
    void executeTool_findMatchRequests_resolvesSportNameAndDelegates() {
        SportType badminton = SportType.builder().sportTypeId(2).sportName("Cầu lông").sportCode("BADMINTON").build();
        when(sportTypeRepository.findAll()).thenReturn(List.of(badminton));

        Page<MatchResponse> page = new PageImpl<>(List.of(MatchResponse.builder().matchId(6).sportName("Badminton").build()));
        when(matchRequestService.getActiveMatches(any(Pageable.class), eq((String) null), eq(2))).thenReturn(page);

        Object result = provider.executeTool("findMatchRequests", "{\"sportName\":\"Cầu lông\"}", 1);

        assertInstanceOf(List.class, result);
        assertEquals(1, ((List<?>) result).size());
        verify(matchRequestService).getActiveMatches(any(Pageable.class), eq((String) null), eq(2));
    }

    @Test
    void executeTool_findMatchRequests_unknownSportName_returnsError() {
        when(sportTypeRepository.findAll()).thenReturn(List.of());

        Object result = provider.executeTool("findMatchRequests", "{\"sportName\":\"Không tồn tại\"}", 1);

        assertTrue(((Map<?, ?>) result).get("error").toString().contains("Không tìm thấy môn thể thao"));
    }

    @Test
    void executeTool_unknownToolName_returnsError() {
        Object result = provider.executeTool("notARealTool", "{}", 1);
        assertTrue(((Map<?, ?>) result).containsKey("error"));
    }

    @Test
    void executeTool_searchStadiums_wrongDiacriticsSportName_stillResolves() {
        SportType football = SportType.builder().sportTypeId(1).sportName("Bóng đá").sportCode("FOOTBALL").build();
        when(sportTypeRepository.findAll()).thenReturn(List.of(football));

        PageResponse<StadiumResponse> pageResponse = PageResponse.<StadiumResponse>builder()
                .content(List.of()).pageNumber(0).pageSize(5).totalElements(0).totalPages(0).last(true).build();
        when(publicStadiumService.searchStadiums(any())).thenReturn(pageResponse);

        // Model đánh sai dấu ("Bông đá") — trước đây rơi thẳng vào nhánh lỗi enum (bug #4)
        provider.executeTool("searchStadiums", "{\"sportName\":\"Bông đá\"}", 1);

        ArgumentCaptor<StadiumSearchRequest> captor = ArgumentCaptor.forClass(StadiumSearchRequest.class);
        verify(publicStadiumService).searchStadiums(captor.capture());
        assertEquals(1, captor.getValue().getSportTypeId());
    }

    @Test
    void executeTool_searchStadiums_colloquialAlias_daBanh_resolvesFootball() {
        SportType football = SportType.builder().sportTypeId(1).sportName("Bóng đá").sportCode("FOOTBALL").build();
        when(sportTypeRepository.findAll()).thenReturn(List.of(football));

        PageResponse<StadiumResponse> pageResponse = PageResponse.<StadiumResponse>builder()
                .content(List.of()).pageNumber(0).pageSize(5).totalElements(0).totalPages(0).last(true).build();
        when(publicStadiumService.searchStadiums(any())).thenReturn(pageResponse);

        provider.executeTool("searchStadiums", "{\"sportName\":\"đá banh\"}", 1);

        ArgumentCaptor<StadiumSearchRequest> captor = ArgumentCaptor.forClass(StadiumSearchRequest.class);
        verify(publicStadiumService).searchStadiums(captor.capture());
        assertEquals(1, captor.getValue().getSportTypeId());
    }

    @Test
    void executeTool_searchStadiums_unknownSport_returnsAvailableSportsList() {
        SportType football = SportType.builder().sportTypeId(1).sportName("Bóng đá").sportCode("FOOTBALL").build();
        when(sportTypeRepository.findAll()).thenReturn(List.of(football));

        Object result = provider.executeTool("searchStadiums", "{\"sportName\":\"Bóng bầu dục\"}", 1);

        assertInstanceOf(Map.class, result);
        Map<?, ?> error = (Map<?, ?>) result;
        assertTrue(error.get("error").toString().contains("Không tìm thấy môn thể thao"));
        assertEquals(List.of("Bóng đá"), error.get("availableSports"));
    }

    @Test
    void executeTool_searchStadiums_defaultsToSizeFiveAndPriceAscSort() {
        PageResponse<StadiumResponse> pageResponse = PageResponse.<StadiumResponse>builder()
                .content(List.of()).pageNumber(0).pageSize(5).totalElements(0).totalPages(0).last(true).build();
        when(publicStadiumService.searchStadiums(any())).thenReturn(pageResponse);

        provider.executeTool("searchStadiums", "{\"district\":\"Quận 9\"}", 1);

        ArgumentCaptor<StadiumSearchRequest> captor = ArgumentCaptor.forClass(StadiumSearchRequest.class);
        verify(publicStadiumService).searchStadiums(captor.capture());
        assertEquals(CustomerAgentToolProvider.AI_SEARCH_RESULT_LIMIT, captor.getValue().getSize());
        assertEquals("pricePerHour", captor.getValue().getSortBy());
        assertEquals("ASC", captor.getValue().getSortDirection());
    }

    @Test
    void executeTool_searchStadiums_ratingSort_mapsToAverageRatingDesc() {
        PageResponse<StadiumResponse> pageResponse = PageResponse.<StadiumResponse>builder()
                .content(List.of()).pageNumber(0).pageSize(5).totalElements(0).totalPages(0).last(true).build();
        when(publicStadiumService.searchStadiums(any())).thenReturn(pageResponse);

        provider.executeTool("searchStadiums", "{\"sortBy\":\"rating\"}", 1);

        ArgumentCaptor<StadiumSearchRequest> captor = ArgumentCaptor.forClass(StadiumSearchRequest.class);
        verify(publicStadiumService).searchStadiums(captor.capture());
        assertEquals("averageRating", captor.getValue().getSortBy());
        assertEquals("DESC", captor.getValue().getSortDirection());
    }

    @Test
    void executeTool_searchStadiums_enrichesCourtNameWithParentFacilityName() {
        Stadium parent = Stadium.builder().stadiumId(100).stadiumName("Sân vận động Cẩm Lệ").nodeType(StadiumNodeType.FACILITY).build();
        Stadium court = Stadium.builder().stadiumId(219).stadiumName("Sân 1").nodeType(StadiumNodeType.COURT).parentStadium(parent).build();

        PageResponse<StadiumResponse> pageResponse = PageResponse.<StadiumResponse>builder()
                .content(List.of(StadiumResponse.builder().stadiumId(219).stadiumName("Sân 1").build()))
                .pageNumber(0).pageSize(5).totalElements(1).totalPages(1).last(true).build();
        when(publicStadiumService.searchStadiums(any())).thenReturn(pageResponse);
        when(stadiumRepository.findCourtsForAiToolByIds(List.of(219))).thenReturn(List.of(court));

        Object result = provider.executeTool("searchStadiums", "{\"keyword\":\"Cẩm Lệ\"}", 1);

        List<?> list = (List<?>) result;
        assertEquals("Sân vận động Cẩm Lệ - Sân 1", ((StadiumResponse) list.get(0)).getStadiumName());
    }

    @Test
    void executeTool_getStadiumSlots_maintenanceStadium_returnsErrorWithoutSlots() {
        Stadium court = Stadium.builder().stadiumId(219).nodeType(StadiumNodeType.COURT)
                .stadiumStatus(StadiumStatus.MAINTENANCE).build();
        when(stadiumRepository.findById(219)).thenReturn(Optional.of(court));

        Object result = provider.executeTool("getStadiumSlots", "{\"stadiumId\":219}", 1);

        assertTrue(((Map<?, ?>) result).get("error").toString().contains("bảo trì"));
        verify(bookingService, never()).getSlotsByDate(anyInt(), any(LocalDate.class));
    }

    @Test
    void executeTool_getStadiumSlots_today_filtersPastSlots() {
        // Cố định giờ VN = 14:00 ngày 2026-07-07 (07:00 UTC) — slot 10h sáng phải bị loại (bug #9)
        provider.setClock(Clock.fixed(Instant.parse("2026-07-07T07:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));

        Stadium court = Stadium.builder().stadiumId(219).nodeType(StadiumNodeType.COURT).build();
        when(stadiumRepository.findById(219)).thenReturn(Optional.of(court));
        when(bookingService.getSlotsByDate(219, LocalDate.of(2026, 7, 7))).thenReturn(List.of(
                TimeSlotResponse.builder().slotId(1).startTime(LocalTime.of(10, 0)).available(true).build(),
                TimeSlotResponse.builder().slotId(2).startTime(LocalTime.of(18, 0)).available(true).build()
        ));

        Object result = provider.executeTool("getStadiumSlots", "{\"stadiumId\":219}", 1);

        List<?> slots = (List<?>) result;
        assertEquals(1, slots.size());
        assertEquals(2, ((TimeSlotResponse) slots.get(0)).getSlotId());
    }

    @Test
    void executeTool_getStadiumSlots_futureDate_keepsAllSlots() {
        provider.setClock(Clock.fixed(Instant.parse("2026-07-07T07:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));

        Stadium court = Stadium.builder().stadiumId(219).nodeType(StadiumNodeType.COURT).build();
        when(stadiumRepository.findById(219)).thenReturn(Optional.of(court));
        when(bookingService.getSlotsByDate(219, LocalDate.of(2026, 7, 8))).thenReturn(List.of(
                TimeSlotResponse.builder().slotId(1).startTime(LocalTime.of(10, 0)).available(true).build(),
                TimeSlotResponse.builder().slotId(2).startTime(LocalTime.of(18, 0)).available(false).build()
        ));

        Object result = provider.executeTool("getStadiumSlots", "{\"stadiumId\":219,\"date\":\"2026-07-08\"}", 1);

        assertEquals(2, ((List<?>) result).size());
    }

    @Test
    void executeTool_getStadiumSlots_pastDate_returnsError() {
        provider.setClock(Clock.fixed(Instant.parse("2026-07-07T07:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));

        Stadium court = Stadium.builder().stadiumId(219).nodeType(StadiumNodeType.COURT).build();
        when(stadiumRepository.findById(219)).thenReturn(Optional.of(court));

        Object result = provider.executeTool("getStadiumSlots", "{\"stadiumId\":219,\"date\":\"2026-07-06\"}", 1);

        assertTrue(((Map<?, ?>) result).get("error").toString().contains("đã qua"));
        verify(bookingService, never()).getSlotsByDate(anyInt(), any(LocalDate.class));
    }

    @Test
    void getSystemPrompt_containsCardSyncAndNoGuessingRules() {
        String prompt = provider.getSystemPrompt(1);
        assertTrue(prompt.contains("KHÔNG tự đoán môn thể thao"));
        assertTrue(prompt.contains("HỎI LẠI vị trí"));
        assertTrue(prompt.contains("card"));
    }

    @Test
    void executeTool_getStadiumSlots_maintenanceScheduleActive_returnsErrorWithoutSlots() {
        // stadiumStatus vẫn AVAILABLE nhưng có MaintenanceSchedule trùm ngày — phải chặn (cơ chế
        // bảo trì theo khung ngày cố tình không đổi status nên check status là không đủ).
        Stadium court = Stadium.builder().stadiumId(219).nodeType(StadiumNodeType.COURT).build();
        when(stadiumRepository.findById(219)).thenReturn(Optional.of(court));
        when(stadiumRepository.findCourtsForAiToolByIds(List.of(219))).thenReturn(List.of(court));
        when(maintenanceScheduleService.isStadiumUnderMaintenance(eq(court), any(LocalDate.class))).thenReturn(true);

        Object result = provider.executeTool("getStadiumSlots", "{\"stadiumId\":219}", 1);

        assertTrue(((Map<?, ?>) result).get("error").toString().contains("lịch bảo trì"));
        verify(bookingService, never()).getSlotsByDate(anyInt(), any(LocalDate.class));
    }

    @Test
    void executeTool_searchStadiums_dropsStadiumUnderMaintenanceSchedule() {
        Stadium normalCourt = Stadium.builder().stadiumId(1).stadiumName("Sân A").nodeType(StadiumNodeType.COURT).build();
        Stadium maintainedCourt = Stadium.builder().stadiumId(2).stadiumName("Sân B").nodeType(StadiumNodeType.COURT).build();

        PageResponse<StadiumResponse> pageResponse = PageResponse.<StadiumResponse>builder()
                .content(List.of(
                        StadiumResponse.builder().stadiumId(1).stadiumName("Sân A").build(),
                        StadiumResponse.builder().stadiumId(2).stadiumName("Sân B").build()))
                .pageNumber(0).pageSize(5).totalElements(2).totalPages(1).last(true).build();
        when(publicStadiumService.searchStadiums(any())).thenReturn(pageResponse);
        when(stadiumRepository.findCourtsForAiToolByIds(List.of(1, 2))).thenReturn(List.of(normalCourt, maintainedCourt));
        when(maintenanceScheduleService.isStadiumUnderMaintenance(eq(normalCourt), any(LocalDate.class))).thenReturn(false);
        when(maintenanceScheduleService.isStadiumUnderMaintenance(eq(maintainedCourt), any(LocalDate.class))).thenReturn(true);

        Object result = provider.executeTool("searchStadiums", "{\"district\":\"Quận 9\"}", 1);

        List<?> list = (List<?>) result;
        assertEquals(1, list.size());
        assertEquals(1, ((StadiumResponse) list.get(0)).getStadiumId());
    }

    @Test
    void getSystemPrompt_containsCurrentVietnamDateAndFaq() {
        provider.setClock(Clock.fixed(Instant.parse("2026-07-07T07:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")));

        String prompt = provider.getSystemPrompt(1);

        assertTrue(prompt.contains("2026-07-07"));
        assertTrue(prompt.contains("14:00"));
        assertTrue(prompt.contains("GIỮ CHỖ 5 PHÚT"));
        assertTrue(prompt.contains("KHÔNG tự bịa"));
    }

    @Test
    void executeTool_searchStadiums_noisyKeyword_retriesWithoutKeyword() {
        // Model nhét cả cụm "sân bóng thủ đức" vào keyword -> search 1 rỗng, fallback rỗng,
        // retry lần 2 bỏ keyword (giữ district) phải ra kết quả.
        PageResponse<StadiumResponse> empty = PageResponse.<StadiumResponse>builder()
                .content(List.of()).pageNumber(0).pageSize(5).totalElements(0).totalPages(0).last(true).build();
        PageResponse<StadiumResponse> found = PageResponse.<StadiumResponse>builder()
                .content(List.of(StadiumResponse.builder().stadiumId(7).stadiumName("Sân Thủ Đức").build()))
                .pageNumber(0).pageSize(5).totalElements(1).totalPages(1).last(true).build();
        // retrySearchWithoutNoisyKeyword mutate thẳng lên CÙNG 1 StadiumSearchRequest instance
        // (setKeyword(null)) rồi gọi lại searchStadiums — ArgumentCaptor.getAllValues() sẽ trả
        // về CÙNG 1 reference cho cả 2 lần gọi (đã bị mutate), nên phải snapshot keyword ngay
        // lúc gọi (thenAnswer) thay vì đọc lại object sau khi cả 2 lần gọi đã xong.
        List<String> capturedKeywords = new java.util.ArrayList<>();
        when(publicStadiumService.searchStadiums(any())).thenAnswer(invocation -> {
            StadiumSearchRequest req = invocation.getArgument(0);
            capturedKeywords.add(req.getKeyword());
            return capturedKeywords.size() == 1 ? empty : found;
        });
        when(stadiumRepository.findCourtsByParentFacilityNameKeyword(anyString())).thenReturn(List.of());
        when(stadiumRepository.findAllCourtFacilityNames()).thenReturn(List.of());

        Object result = provider.executeTool("searchStadiums",
                "{\"keyword\":\"sân bóng thủ đức\",\"district\":\"Thủ Đức\"}", 1);

        assertInstanceOf(List.class, result);
        assertEquals(1, ((List<?>) result).size());

        ArgumentCaptor<StadiumSearchRequest> captor = ArgumentCaptor.forClass(StadiumSearchRequest.class);
        verify(publicStadiumService, org.mockito.Mockito.times(2)).searchStadiums(captor.capture());
        assertEquals("sân bóng thủ đức", capturedKeywords.get(0));
        // Lần 2 keyword bị bỏ, district giữ nguyên
        assertEquals(null, capturedKeywords.get(1));
        assertEquals("Thủ Đức", captor.getAllValues().get(1).getDistrict());
    }

    @Test
    void executeTool_searchStadiums_tokenFallback_matchesFacilityWithExtraWords() {
        // "San bong Vinh Hoang" phải match facility "Sân bóng đá Vĩnh Hoàng" — contains cả cụm
        // fail vì thiếu chữ "đá" ở giữa, token-match bỏ từ chung thì đậu.
        PageResponse<StadiumResponse> empty = PageResponse.<StadiumResponse>builder()
                .content(List.of()).pageNumber(0).pageSize(5).totalElements(0).totalPages(0).last(true).build();
        when(publicStadiumService.searchStadiums(any())).thenReturn(empty);
        when(stadiumRepository.findCourtsByParentFacilityNameKeyword(anyString())).thenReturn(List.of());

        StadiumRepository.CourtFacilityNameProjection projection = mock(StadiumRepository.CourtFacilityNameProjection.class);
        when(projection.getStadiumId()).thenReturn(300);
        when(projection.getFacilityName()).thenReturn("Sân bóng đá Vĩnh Hoàng");
        when(stadiumRepository.findAllCourtFacilityNames()).thenReturn(List.of(projection));

        Stadium court = Stadium.builder().stadiumId(300).stadiumName("Sân 1").nodeType(StadiumNodeType.COURT).build();
        when(stadiumRepository.findCourtsForAiToolByIds(List.of(300))).thenReturn(List.of(court));
        when(stadiumMapper.toResponse(court)).thenReturn(StadiumResponse.builder().stadiumId(300).stadiumName("Sân 1").build());

        Object result = provider.executeTool("searchStadiums", "{\"keyword\":\"San bong Vinh Hoang\"}", 1);

        assertInstanceOf(List.class, result);
        assertEquals(1, ((List<?>) result).size());
    }

    @Test
    void executeTool_getStadiumSlots_fabricatedIdZero_returnsGuidanceError() {
        Object result = provider.executeTool("getStadiumSlots", "{\"stadiumId\":0}", 1);

        assertTrue(((Map<?, ?>) result).get("error").toString().contains("KHÔNG được tự bịa ID"));
        verify(bookingService, never()).getSlotsByDate(anyInt(), any(LocalDate.class));
    }
}
