package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateMatchRequest;
import com.sportvenue.dto.response.MatchResponse;
import com.sportvenue.entity.MatchRequest;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.MatchStatus;
import com.sportvenue.entity.enums.SkillLevel;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.MatchRequestRepository;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchRequestServiceImplTest {

    @Mock
    private MatchRequestRepository matchRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private SportTypeRepository sportTypeRepository;

    @Mock
    private BookingRepository bookingRepository;

    private MatchRequestServiceImpl matchRequestService;

    @BeforeEach
    void setUp() {
        matchRequestService = new MatchRequestServiceImpl(
                matchRequestRepository,
                userRepository,
                stadiumRepository,
                sportTypeRepository,
                bookingRepository
        );
    }

    @Test
    void createMatch_Success() {
        // Arrange
        Integer userId = 1;
        CreateMatchRequest request = validRequest();

        User user = User.builder()
                .userId(userId)
                .firstName("Nguyen")
                .lastName("An")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        SportType sportType = SportType.builder()
                .sportTypeId(1)
                .sportName("Football")
                .build();

        Stadium stadium = Stadium.builder()
                .stadiumId(10)
                .stadiumName("Sân Hoa Lư")
                .address("Quận 1")
                .stadiumStatus(StadiumStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.APPROVED)
                .sportType(sportType)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stadiumRepository.findById(request.getStadiumId())).thenReturn(Optional.of(stadium));
        when(sportTypeRepository.findById(request.getSportTypeId())).thenReturn(Optional.of(sportType));

        when(matchRequestRepository.existsOverlappingMatchRequest(any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any(), any())).thenReturn(false);

        when(matchRequestRepository.save(any(MatchRequest.class))).thenAnswer(invocation -> {
            MatchRequest mr = invocation.getArgument(0);
            mr.setMatchId(100);
            mr.setCreatedAt(LocalDateTime.now());
            return mr;
        });

        // Act
        MatchResponse response = matchRequestService.createMatch(request, userId);

        // Assert
        assertNotNull(response);
        assertEquals(100, response.getMatchId());
        assertEquals("Nguyen An", response.getHostName());
        assertEquals("Sân Hoa Lư", response.getStadiumName());
        assertEquals("Football", response.getSportName());
        assertEquals(MatchStatus.OPEN, response.getMatchStatus());
        assertEquals(1, response.getCurrentPlayers());

        ArgumentCaptor<MatchRequest> captor = ArgumentCaptor.forClass(MatchRequest.class);
        verify(matchRequestRepository).save(captor.capture());
        MatchRequest saved = captor.getValue();
        assertEquals("Trận đấu vui vẻ", saved.getTitle());
        assertEquals(LocalTime.of(18, 0), saved.getStartTime());
        assertEquals(LocalTime.of(20, 0), saved.getEndTime());
    }

    @Test
    void createMatch_UserNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                matchRequestService.createMatch(validRequest(), 1));

        verify(matchRequestRepository, never()).save(any());
    }

    @Test
    void createMatch_UserNotActive() {
        User user = User.builder().userId(1).accountStatus(AccountStatus.BLOCKED).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () ->
                matchRequestService.createMatch(validRequest(), 1));

        verify(matchRequestRepository, never()).save(any());
    }

    @Test
    void createMatch_StadiumNotFound() {
        User user = User.builder().userId(1).accountStatus(AccountStatus.ACTIVE).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(stadiumRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                matchRequestService.createMatch(validRequest(), 1));

        verify(matchRequestRepository, never()).save(any());
    }

    @Test
    void createMatch_StadiumNotAvailable() {
        User user = User.builder().userId(1).accountStatus(AccountStatus.ACTIVE).build();
        Stadium stadium = Stadium.builder().stadiumId(10).stadiumStatus(StadiumStatus.MAINTENANCE).build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));

        assertThrows(BadRequestException.class, () ->
                matchRequestService.createMatch(validRequest(), 1));

        verify(matchRequestRepository, never()).save(any());
    }

    @Test
    void createMatch_StadiumNotApproved() {
        User user = User.builder().userId(1).accountStatus(AccountStatus.ACTIVE).build();
        Stadium stadium = Stadium.builder()
                .stadiumId(10)
                .stadiumStatus(StadiumStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.PENDING)
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));

        assertThrows(BadRequestException.class, () ->
                matchRequestService.createMatch(validRequest(), 1));

        verify(matchRequestRepository, never()).save(any());
    }

    @Test
    void createMatch_SportTypeNotSupportedByStadium() {
        User user = User.builder().userId(1).accountStatus(AccountStatus.ACTIVE).build();
        SportType sportFootball = SportType.builder().sportTypeId(1).sportName("Football").build();
        SportType sportBadminton = SportType.builder().sportTypeId(2).sportName("Badminton").build();

        Stadium stadium = Stadium.builder()
                .stadiumId(10)
                .stadiumStatus(StadiumStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.APPROVED)
                .sportType(sportBadminton) // Sân Cầu Lông
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(sportTypeRepository.findById(1)).thenReturn(Optional.of(sportFootball)); // Kèo bóng đá

        assertThrows(BadRequestException.class, () ->
                matchRequestService.createMatch(validRequest(), 1));

        verify(matchRequestRepository, never()).save(any());
    }

    @Test
    void createMatch_OverlappingMatchRequest() {
        User user = User.builder().userId(1).accountStatus(AccountStatus.ACTIVE).build();
        SportType sport = SportType.builder().sportTypeId(1).sportName("Football").build();
        Stadium stadium = Stadium.builder()
                .stadiumId(10)
                .stadiumStatus(StadiumStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.APPROVED)
                .sportType(sport)
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(sportTypeRepository.findById(1)).thenReturn(Optional.of(sport));

        // Có kèo khác trùng lịch
        when(matchRequestRepository.existsOverlappingMatchRequest(any(), any(), any(), any())).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                matchRequestService.createMatch(validRequest(), 1));

        verify(matchRequestRepository, never()).save(any());
    }

    @Test
    void createMatch_OverlappingBooking() {
        User user = User.builder().userId(1).accountStatus(AccountStatus.ACTIVE).build();
        SportType sport = SportType.builder().sportTypeId(1).sportName("Football").build();
        Stadium stadium = Stadium.builder()
                .stadiumId(10)
                .stadiumStatus(StadiumStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.APPROVED)
                .sportType(sport)
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(sportTypeRepository.findById(1)).thenReturn(Optional.of(sport));

        when(matchRequestRepository.existsOverlappingMatchRequest(any(), any(), any(), any())).thenReturn(false);
        // Có đơn đặt sân trùng lịch
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any(), any())).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                matchRequestService.createMatch(validRequest(), 1));

        verify(matchRequestRepository, never()).save(any());
    }

    @Test
    void getActiveMatches_ReturnsPagedMatches() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        User user = User.builder().firstName("Nguyen").lastName("An").build();
        SportType sport = SportType.builder().sportName("Football").build();
        Stadium stadium = Stadium.builder().stadiumName("Sân Hoa Lư").address("Quận 1").build();

        MatchRequest mr = MatchRequest.builder()
                .matchId(1)
                .user(user)
                .stadium(stadium)
                .sportType(sport)
                .matchStatus(MatchStatus.OPEN)
                .build();

        Page<MatchRequest> page = new PageImpl<>(List.of(mr), pageable, 1);
        when(matchRequestRepository.findAllByMatchStatus(MatchStatus.OPEN, pageable)).thenReturn(page);

        // Act
        Page<MatchResponse> result = matchRequestService.getActiveMatches(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Nguyen An", result.getContent().get(0).getHostName());
    }

    private CreateMatchRequest validRequest() {
        return CreateMatchRequest.builder()
                .stadiumId(10)
                .sportTypeId(1)
                .title("Trận đấu vui vẻ")
                .description("Cần thêm 4 người đá bóng")
                .playDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 0))
                .maxPlayers(10)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .splitPrice(true)
                .pricePerPlayer(BigDecimal.valueOf(50000))
                .build();
    }
}
