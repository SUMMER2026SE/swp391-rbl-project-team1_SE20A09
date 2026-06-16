package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateMatchRequest;
import com.sportvenue.dto.response.MatchResponse;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.SkillLevel;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.MatchRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchRequestControllerTest {

    @Mock
    private MatchRequestService matchRequestService;

    @InjectMocks
    private MatchRequestController matchRequestController;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        User user = User.builder().userId(1).email("customer@test.com").build();
        userPrincipal = new UserPrincipal(user);
    }

    @Test
    void createMatch_Success() {
        // Arrange
        CreateMatchRequest request = CreateMatchRequest.builder()
                .stadiumId(10)
                .sportTypeId(1)
                .title("Trận cầu đỉnh cao")
                .description("Cần giao lưu vui vẻ")
                .playDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(17, 0))
                .endTime(LocalTime.of(19, 0))
                .maxPlayers(10)
                .skillLevel(SkillLevel.BEGINNER)
                .splitPrice(false)
                .build();

        MatchResponse response = MatchResponse.builder()
                .matchId(100)
                .title("Trận cầu đỉnh cao")
                .build();

        when(matchRequestService.createMatch(eq(request), eq(1))).thenReturn(response);

        // Act
        ResponseEntity<MatchResponse> result = matchRequestController.createMatch(userPrincipal, request);

        // Assert
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(100, result.getBody().getMatchId());
        verify(matchRequestService).createMatch(request, 1);
    }

    @Test
    void getActiveMatches_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<MatchResponse> pageResponse = new PageImpl<>(List.of(new MatchResponse()));
        when(matchRequestService.getActiveMatches(pageable)).thenReturn(pageResponse);

        // Act
        ResponseEntity<Page<MatchResponse>> result = matchRequestController.getActiveMatches(pageable);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getTotalElements());
        verify(matchRequestService).getActiveMatches(pageable);
    }
}
