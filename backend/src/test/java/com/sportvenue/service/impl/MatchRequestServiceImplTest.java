package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateMatchRequest;
import com.sportvenue.dto.response.MatchResponse;
import com.sportvenue.entity.MatchRequest;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.ComplexStatus;
import com.sportvenue.entity.enums.MatchStatus;
import com.sportvenue.entity.enums.MatchingType;
import com.sportvenue.entity.enums.SkillLevel;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.JoinRequest;
import com.sportvenue.entity.enums.JoinRequestStatus;
import com.sportvenue.dto.response.JoinRequestResponse;
import com.sportvenue.repository.JoinRequestRepository;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.MatchRequestRepository;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.StadiumComplexRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.ChatService;
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

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private StadiumComplexRepository stadiumComplexRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    private MatchRequestServiceImpl matchRequestService;

    @BeforeEach
    void setUp() {
        matchRequestService = new MatchRequestServiceImpl(
                matchRequestRepository,
                userRepository,
                stadiumRepository,
                sportTypeRepository,
                bookingRepository,
                joinRequestRepository,
                chatService,
                stadiumComplexRepository,
                timeSlotRepository
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
        when(stadiumRepository.findByIdWithComplexAndParent(request.getStadiumId())).thenReturn(Optional.of(stadium));
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
        assertEquals(MatchingType.INDIVIDUAL, saved.getMatchingType());
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
        when(stadiumRepository.findByIdWithComplexAndParent(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                matchRequestService.createMatch(validRequest(), 1));

        verify(matchRequestRepository, never()).save(any());
    }

    @Test
    void createMatch_StadiumNotAvailable() {
        User user = User.builder().userId(1).accountStatus(AccountStatus.ACTIVE).build();
        Stadium stadium = Stadium.builder().stadiumId(10).stadiumStatus(StadiumStatus.MAINTENANCE).build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(stadiumRepository.findByIdWithComplexAndParent(10)).thenReturn(Optional.of(stadium));

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
        when(stadiumRepository.findByIdWithComplexAndParent(10)).thenReturn(Optional.of(stadium));

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
        when(stadiumRepository.findByIdWithComplexAndParent(10)).thenReturn(Optional.of(stadium));
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
        when(stadiumRepository.findByIdWithComplexAndParent(10)).thenReturn(Optional.of(stadium));
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
        when(stadiumRepository.findByIdWithComplexAndParent(10)).thenReturn(Optional.of(stadium));
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
        when(matchRequestRepository.findActiveMatchesSorted(any(), any(), any(), eq(pageable))).thenReturn(page);

        // Act
        Page<MatchResponse> result = matchRequestService.getActiveMatches(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Nguyen An", result.getContent().get(0).getHostName());
    }

    @Test
    void createMatch_TeamVsTeam_Success() {
        // Arrange
        Integer userId = 1;
        CreateMatchRequest request = CreateMatchRequest.builder()
                .stadiumId(10)
                .sportTypeId(1)
                .title("FC Hoa Lư thách đấu")
                .description("Cần tìm đối thủ cáp kèo sân 7")
                .playDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 0))
                .maxPlayers(2)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .matchingType(MatchingType.TEAM_VS_TEAM)
                .splitPrice(true)
                .pricePerPlayer(BigDecimal.valueOf(150000))
                .build();

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
        when(stadiumRepository.findByIdWithComplexAndParent(request.getStadiumId())).thenReturn(Optional.of(stadium));
        when(sportTypeRepository.findById(request.getSportTypeId())).thenReturn(Optional.of(sportType));

        when(matchRequestRepository.existsOverlappingMatchRequest(any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any(), any())).thenReturn(false);

        when(matchRequestRepository.save(any(MatchRequest.class))).thenAnswer(invocation -> {
            MatchRequest mr = invocation.getArgument(0);
            mr.setMatchId(200);
            mr.setCreatedAt(LocalDateTime.now());
            return mr;
        });

        // Act
        MatchResponse response = matchRequestService.createMatch(request, userId);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getMatchId());
        assertEquals(MatchingType.TEAM_VS_TEAM, response.getMatchingType());
        assertEquals(2, response.getMaxPlayers());

        ArgumentCaptor<MatchRequest> captor = ArgumentCaptor.forClass(MatchRequest.class);
        verify(matchRequestRepository).save(captor.capture());
        MatchRequest saved = captor.getValue();
        assertEquals(MatchingType.TEAM_VS_TEAM, saved.getMatchingType());
        assertEquals(2, saved.getMaxPlayers());
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

    @Test
    void joinMatch_Success() {
        Integer matchId = 100;
        Integer guestUserId = 2;

        User host = User.builder().userId(1).build();
        MatchRequest match = MatchRequest.builder()
                .matchId(matchId)
                .user(host)
                .matchStatus(MatchStatus.OPEN)
                .playDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 0))
                .matchingType(MatchingType.INDIVIDUAL)
                .build();

        User guest = User.builder()
                .userId(guestUserId)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(matchRequestRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(userRepository.findById(guestUserId)).thenReturn(Optional.of(guest));
        when(joinRequestRepository.existsByMatchRequestMatchIdAndUserUserIdAndRequestStatusIn(
                eq(matchId), eq(guestUserId), any())).thenReturn(false);
        when(matchRequestRepository.existsOverlappingMatchRequest(any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any(), any())).thenReturn(false);

        matchRequestService.joinMatch(matchId, guestUserId, "Xin tham gia");

        ArgumentCaptor<JoinRequest> captor = ArgumentCaptor.forClass(JoinRequest.class);
        verify(joinRequestRepository).save(captor.capture());
        JoinRequest saved = captor.getValue();
        assertEquals(JoinRequestStatus.PENDING, saved.getRequestStatus());
        assertEquals("Xin tham gia", saved.getMessage());
        assertEquals(guestUserId, saved.getUser().getUserId());
    }

    @Test
    void joinMatch_OwnMatch_ThrowsBadRequestException() {
        Integer matchId = 100;
        Integer hostUserId = 1;

        User host = User.builder().userId(hostUserId).build();
        MatchRequest match = MatchRequest.builder()
                .matchId(matchId)
                .user(host)
                .matchStatus(MatchStatus.OPEN)
                .playDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 0))
                .build();

        when(matchRequestRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThrows(BadRequestException.class, () ->
                matchRequestService.joinMatch(matchId, hostUserId, "Xin gia nhập"));
    }

    @Test
    void getJoinRequestsForMatch_Success() {
        Integer matchId = 100;
        Integer hostUserId = 1;

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

        User host = User.builder().userId(hostUserId).firstName("Nguyen").lastName("A").email("host@gmail.com").build();
        MatchRequest match = MatchRequest.builder()
                .matchId(matchId)
                .user(host)
                .stadium(stadium)
                .sportType(sportType)
                .title("Kèo đá bóng")
                .playDate(LocalDate.now())
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 0))
                .matchStatus(MatchStatus.OPEN)
                .matchingType(MatchingType.INDIVIDUAL)
                .build();

        User guest = User.builder().userId(2).firstName("Nguyen").lastName("B").email("b@gmail.com").build();
        JoinRequest request = JoinRequest.builder()
                .joinId(500)
                .matchRequest(match)
                .user(guest)
                .requestStatus(JoinRequestStatus.PENDING)
                .message("Hello")
                .createdAt(LocalDateTime.now())
                .build();

        when(matchRequestRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(joinRequestRepository.findAllByMatchRequestMatchId(matchId)).thenReturn(List.of(request));

        List<JoinRequestResponse> responses = matchRequestService.getJoinRequestsForMatch(matchId, hostUserId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(500, responses.get(0).getJoinId());
        assertEquals("Nguyen B", responses.get(0).getFullName());
    }

    @Test
    void approveJoinRequest_Success_MatchBecomesFull() {
        Integer matchId = 100;
        Integer hostUserId = 1;
        Integer joinId = 500;

        User host = User.builder().userId(hostUserId).build();
        MatchRequest match = MatchRequest.builder()
                .matchId(matchId)
                .user(host)
                .matchStatus(MatchStatus.OPEN)
                .maxPlayers(2)
                .currentPlayers(1)
                .build();

        User guest = User.builder().userId(2).build();
        JoinRequest joinRequest = JoinRequest.builder()
                .joinId(joinId)
                .matchRequest(match)
                .user(guest)
                .requestStatus(JoinRequestStatus.PENDING)
                .build();

        // After atomic SQL increment, DB currentPlayers becomes 2 (equals maxPlayers)
        MatchRequest matchAfterIncrement = MatchRequest.builder()
                .matchId(matchId)
                .user(host)
                .matchStatus(MatchStatus.OPEN)
                .maxPlayers(2)
                .currentPlayers(2)
                .build();

        when(matchRequestRepository.findById(matchId))
                .thenReturn(Optional.of(match))           // initial lookup
                .thenReturn(Optional.of(matchAfterIncrement)); // reload after increment
        when(joinRequestRepository.findById(joinId)).thenReturn(Optional.of(joinRequest));
        when(matchRequestRepository.incrementCurrentPlayers(matchId)).thenReturn(1);

        matchRequestService.approveJoinRequest(matchId, joinId, hostUserId);

        assertEquals(JoinRequestStatus.APPROVED, joinRequest.getRequestStatus());
        verify(joinRequestRepository).saveAndFlush(joinRequest);
        verify(matchRequestRepository).incrementCurrentPlayers(matchId);
        verify(matchRequestRepository).updateStatusAndReason(eq(matchId), eq(MatchStatus.FULL), any());
        verify(joinRequestRepository).bulkUpdateStatus(eq(matchId), eq(JoinRequestStatus.REJECTED), any());
    }

    @Test
    void cancelMatch_Success() {
        Integer matchId = 100;
        Integer hostUserId = 1;

        User host = User.builder().userId(hostUserId).build();
        MatchRequest match = MatchRequest.builder()
                .matchId(matchId)
                .user(host)
                .matchStatus(MatchStatus.OPEN)
                .playDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .build();

        when(matchRequestRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(joinRequestRepository.bulkUpdateStatus(any(), any(), any())).thenReturn(2);

        matchRequestService.cancelMatch(matchId, hostUserId, "Lý do cá nhân");

        verify(matchRequestRepository).updateStatusAndReason(eq(matchId), eq(MatchStatus.CANCELLED), eq("Lý do cá nhân"));
        verify(joinRequestRepository).bulkUpdateStatus(eq(matchId), eq(JoinRequestStatus.CANCELLED), any());
    }

    @Test
    void cancelMatch_NotHost_ThrowsBadRequestException() {
        Integer matchId = 100;
        Integer hostUserId = 1;
        Integer otherUserId = 2;

        User host = User.builder().userId(hostUserId).build();
        MatchRequest match = MatchRequest.builder()
                .matchId(matchId)
                .user(host)
                .build();

        when(matchRequestRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThrows(BadRequestException.class, () ->
                matchRequestService.cancelMatch(matchId, otherUserId, "Lý do cá nhân"));
    }

    @Test
    void cancelMatch_AlreadyExpired_ThrowsBadRequestException() {
        Integer matchId = 100;
        Integer hostUserId = 1;

        User host = User.builder().userId(hostUserId).build();
        MatchRequest match = MatchRequest.builder()
                .matchId(matchId)
                .user(host)
                .matchStatus(MatchStatus.OPEN)
                .playDate(LocalDate.now().minusDays(1)) // Đã qua ngày
                .startTime(LocalTime.of(18, 0))
                .build();

        when(matchRequestRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThrows(BadRequestException.class, () ->
                matchRequestService.cancelMatch(matchId, hostUserId, "Lý do cá nhân"));
    }

    @Test
    void cancelMatch_InvalidStatus_ThrowsBadRequestException() {
        Integer matchId = 100;
        Integer hostUserId = 1;

        User host = User.builder().userId(hostUserId).build();
        MatchRequest match = MatchRequest.builder()
                .matchId(matchId)
                .user(host)
                .matchStatus(MatchStatus.COMPLETED) // Trạng thái không được hủy
                .playDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .build();

        when(matchRequestRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThrows(BadRequestException.class, () ->
                matchRequestService.cancelMatch(matchId, hostUserId, "Lý do cá nhân"));
    }

    @Test
    void createMatch_Complex_Success() {
        Integer userId = 1;
        
        CreateMatchRequest request = CreateMatchRequest.builder()
                .complexId(5)
                .sportTypeId(1)
                .title("Kèo bóng đá 5v5")
                .description("Vui vẻ")
                .playDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .maxPlayers(10)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .splitPrice(false)
                .matchingType(MatchingType.INDIVIDUAL)
                .build();

        User user = User.builder()
                .userId(userId)
                .accountStatus(AccountStatus.ACTIVE)
                .firstName("Nguyen")
                .lastName("An")
                .build();

        SportType sportType = SportType.builder()
                .sportTypeId(1)
                .sportName("Football")
                .build();

        StadiumComplex complex = StadiumComplex.builder()
                .complexId(5)
                .name("Tổ hợp Tuyên Sơn")
                .address("Đà Nẵng")
                .complexStatus(ComplexStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.APPROVED)
                .sportTypes(java.util.Set.of(sportType))
                .build();

        Stadium court = Stadium.builder()
                .stadiumId(20)
                .stadiumName("Sân số 1")
                .nodeType(StadiumNodeType.COURT)
                .complex(complex)
                .sportType(sportType)
                .build();

        TimeSlot slot = TimeSlot.builder()
                .slotId(50)
                .stadium(court)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .slotStatus(com.sportvenue.entity.enums.SlotStatus.AVAILABLE)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stadiumComplexRepository.findById(5)).thenReturn(Optional.of(complex));
        when(sportTypeRepository.findById(1)).thenReturn(Optional.of(sportType));
        when(stadiumRepository.findCourtsByComplexId(5)).thenReturn(List.of(court));
        when(timeSlotRepository.findOverlappingSlotsByStadiumIds(any(), any(), any())).thenReturn(List.of(slot));
        when(bookingRepository.findActiveBookingKeysByStadiumIds(any(), any(), any())).thenReturn(List.of());

        when(matchRequestRepository.existsOverlappingMatchRequest(any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.existsOverlappingBooking(any(), any(), any(), any(), any())).thenReturn(false);

        when(matchRequestRepository.save(any(MatchRequest.class))).thenAnswer(invocation -> {
            MatchRequest mr = invocation.getArgument(0);
            mr.setMatchId(300);
            mr.setCreatedAt(LocalDateTime.now());
            return mr;
        });

        MatchResponse response = matchRequestService.createMatch(request, userId);

        assertNotNull(response);
        assertEquals(300, response.getMatchId());
        assertEquals("Nguyen An", response.getHostName());
    }

    @Test
    void createMatch_Complex_NoAvailableCourt_ThrowsException() {
        Integer userId = 1;
        
        CreateMatchRequest request = CreateMatchRequest.builder()
                .complexId(5)
                .sportTypeId(1)
                .title("Kèo bóng đá 5v5")
                .playDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .maxPlayers(10)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .splitPrice(false)
                .build();

        User user = User.builder()
                .userId(userId)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        SportType sportType = SportType.builder()
                .sportTypeId(1)
                .sportName("Football")
                .build();

        StadiumComplex complex = StadiumComplex.builder()
                .complexId(5)
                .complexStatus(ComplexStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.APPROVED)
                .sportTypes(java.util.Set.of(sportType))
                .build();

        Stadium court = Stadium.builder()
                .stadiumId(20)
                .nodeType(StadiumNodeType.COURT)
                .complex(complex)
                .sportType(sportType)
                .build();

        // Không có slot trùng hoặc slot bận
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stadiumComplexRepository.findById(5)).thenReturn(Optional.of(complex));
        when(sportTypeRepository.findById(1)).thenReturn(Optional.of(sportType));
        when(stadiumRepository.findCourtsByComplexId(5)).thenReturn(List.of(court));
        when(timeSlotRepository.findOverlappingSlotsByStadiumIds(any(), any(), any())).thenReturn(List.of()); // slot rỗng

        assertThrows(BadRequestException.class, () ->
                matchRequestService.createMatch(request, userId));
    }

    @Test
    void createMatch_Complex_SportTypeNotSupported_ThrowsException() {
        Integer userId = 1;
        
        CreateMatchRequest request = CreateMatchRequest.builder()
                .complexId(5)
                .sportTypeId(2) // Cầu lông
                .title("Kèo cầu lông")
                .playDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .maxPlayers(10)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .splitPrice(false)
                .build();

        User user = User.builder()
                .userId(userId)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        SportType sportType = SportType.builder()
                .sportTypeId(2)
                .sportName("Badminton")
                .build();

        StadiumComplex complex = StadiumComplex.builder()
                .complexId(5)
                .complexStatus(ComplexStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.APPROVED)
                .sportTypes(java.util.Set.of()) // Không hỗ trợ môn nào
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(stadiumComplexRepository.findById(5)).thenReturn(Optional.of(complex));
        when(sportTypeRepository.findById(2)).thenReturn(Optional.of(sportType));

        assertThrows(BadRequestException.class, () ->
                matchRequestService.createMatch(request, userId));
    }
}
