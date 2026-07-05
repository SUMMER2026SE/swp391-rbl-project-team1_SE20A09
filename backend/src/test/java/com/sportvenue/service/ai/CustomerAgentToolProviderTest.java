package com.sportvenue.service.ai;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.MatchResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.mapper.StadiumMapper;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.service.MatchRequestService;
import com.sportvenue.service.PublicStadiumService;
import com.sportvenue.service.TimeSlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
    private TimeSlotService timeSlotService;

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
                publicStadiumService, timeSlotService, matchRequestService,
                sportTypeRepository, stadiumRepository, stadiumMapper);
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
        assertTrue(((Map<?, ?>) result).get("error").toString().contains("Không tìm thấy loại môn thể thao"));
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
        assertEquals("Quận 9", captor.getValue().getAddress());
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
        assertEquals("Thủ Đức", captor.getValue().getAddress());
    }

    @Test
    void executeTool_findMatchRequests_locationWithWrongDiacritics_stillNormalizes() {
        Page<MatchResponse> page = new PageImpl<>(List.of());
        when(matchRequestService.getActiveMatches(any(Pageable.class), eq("Bình Thạnh"))).thenReturn(page);

        provider.executeTool("findMatchRequests", "{\"location\":\"Bính Thánh\"}", 1);

        verify(matchRequestService).getActiveMatches(any(Pageable.class), eq("Bình Thạnh"));
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
        verify(timeSlotService, never()).getSlotsByStadiumId(anyInt());
    }

    @Test
    void executeTool_getStadiumSlots_validCourt_returnsSlots() {
        Stadium court = Stadium.builder().stadiumId(219).nodeType(StadiumNodeType.COURT).build();
        when(stadiumRepository.findById(219)).thenReturn(Optional.of(court));
        when(timeSlotService.getSlotsByStadiumId(219)).thenReturn(List.of(TimeSlotResponse.builder().slotId(1).build()));

        Object result = provider.executeTool("getStadiumSlots", "{\"stadiumId\":219}", 1);

        assertInstanceOf(List.class, result);
        assertEquals(1, ((List<?>) result).size());
    }

    @Test
    void executeTool_findMatchRequests_normalizesLocationAndDelegates() {
        Page<MatchResponse> page = new PageImpl<>(List.of(MatchResponse.builder().matchId(1).build()));
        when(matchRequestService.getActiveMatches(any(Pageable.class), eq("Thủ Đức"))).thenReturn(page);

        Object result = provider.executeTool("findMatchRequests", "{\"location\":\"Thu Duc\"}", 1);

        assertInstanceOf(List.class, result);
        assertEquals(1, ((List<?>) result).size());
    }

    @Test
    void executeTool_findMatchRequests_noLocation_passesNull() {
        Page<MatchResponse> page = new PageImpl<>(List.of());
        when(matchRequestService.getActiveMatches(any(Pageable.class), eq((String) null))).thenReturn(page);

        provider.executeTool("findMatchRequests", "{}", 1);

        verify(matchRequestService).getActiveMatches(any(Pageable.class), eq((String) null));
    }

    @Test
    void executeTool_unknownToolName_returnsError() {
        Object result = provider.executeTool("notARealTool", "{}", 1);
        assertTrue(((Map<?, ?>) result).containsKey("error"));
    }
}
