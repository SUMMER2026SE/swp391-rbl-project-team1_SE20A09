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
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.JoinRequestRepository;
import com.sportvenue.repository.MatchRequestRepository;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.entity.JoinRequest;
import com.sportvenue.entity.enums.JoinRequestStatus;
import com.sportvenue.dto.response.JoinRequestResponse;
import com.sportvenue.service.MatchRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Implementation thực tế cho việc quản lý kèo ghép thể thao.
 * Thực hiện đầy đủ các ràng buộc nghiệp vụ của UC-CUS-10.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchRequestServiceImpl implements MatchRequestService {

    private final MatchRequestRepository matchRequestRepository;
    private final UserRepository userRepository;
    private final StadiumRepository stadiumRepository;
    private final SportTypeRepository sportTypeRepository;
    private final BookingRepository bookingRepository;
    private final JoinRequestRepository joinRequestRepository;

    @Override
    @Transactional
    public MatchResponse createMatch(CreateMatchRequest request, Integer userId) {
        log.info("Creating match request: '{}' for user ID: {}", request.getTitle(), userId);

        // 1. Kiểm tra User (Host) tồn tại và hoạt động
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("User account is not active");
        }

        // 2. Kiểm tra Stadium tồn tại, hoạt động và đã được duyệt
        Stadium stadium = stadiumRepository.findById(request.getStadiumId())
                .orElseThrow(() -> new ResourceNotFoundException("Stadium not found with ID: " + request.getStadiumId()));
        
        if (stadium.getStadiumStatus() != StadiumStatus.AVAILABLE) {
            throw new BadRequestException("Stadium is currently not available (e.g. CLOSED or under MAINTENANCE)");
        }
        
        if (stadium.getApprovedStatus() != ApprovedStatus.APPROVED) {
            throw new BadRequestException("Stadium is not approved yet");
        }

        // 3. Kiểm tra Sport Type tồn tại
        SportType sportType = sportTypeRepository.findById(request.getSportTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Sport Type not found with ID: " + request.getSportTypeId()));

        // 4. Đảm bảo Sân hỗ trợ đúng môn thể thao của kèo ghép
        if (!stadium.getSportType().getSportTypeId().equals(sportType.getSportTypeId())) {
            throw new BadRequestException("The selected stadium does not support the sport type: " + sportType.getSportName());
        }

        // 5. Kiểm tra trùng lịch kèo ghép và Booking của Host
        validateOverlappingSchedule(userId, request);

        // 7. Tạo và lưu MatchRequest
        MatchRequest matchRequest = MatchRequest.builder()
                .user(user)
                .stadium(stadium)
                .sportType(sportType)
                .title(request.getTitle())
                .description(request.getDescription())
                .playDate(request.getPlayDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxPlayers(request.getMaxPlayers())
                .currentPlayers(1) // Host là thành viên đầu tiên
                .skillLevel(request.getSkillLevel())
                .splitPrice(request.getSplitPrice())
                .pricePerPlayer(request.getPricePerPlayer() != null ? request.getPricePerPlayer() : BigDecimal.ZERO)
                .matchingType(request.getMatchingType())
                .matchStatus(MatchStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        MatchRequest savedMatch = matchRequestRepository.save(matchRequest);
        log.info("Successfully created match request with ID: {}", savedMatch.getMatchId());

        return mapToResponse(savedMatch);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchResponse> getActiveMatches(Pageable pageable) {
        log.info("Retrieving active match requests with pagination and future dates: {}", pageable);
        LocalDate nowDate = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        Page<MatchRequest> matches = matchRequestRepository.findActiveMatchesSorted(
                Arrays.asList(MatchStatus.OPEN, MatchStatus.FULL),
                nowDate,
                nowTime,
                pageable
        );
        return matches.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void joinMatch(Integer matchId, Integer userId, String message) {
        log.info("User ID {} requesting to join Match ID: {} with message: '{}'", userId, matchId, message);

        MatchRequest match = matchRequestRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match request not found with ID: " + matchId));

        if (match.getMatchStatus() != MatchStatus.OPEN) {
            throw new BadRequestException("Match request is not open for joining");
        }

        LocalDate nowDate = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        if (match.getPlayDate().isBefore(nowDate) || 
            (match.getPlayDate().isEqual(nowDate) && match.getStartTime().isBefore(nowTime))) {
            throw new BadRequestException("This match request has already expired");
        }

        if (match.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("You cannot join your own match request");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Your account is not active");
        }

        boolean alreadyRequested = joinRequestRepository.existsByMatchRequestMatchIdAndUserUserIdAndRequestStatusIn(
                matchId,
                userId,
                Arrays.asList(JoinRequestStatus.PENDING, JoinRequestStatus.APPROVED)
        );
        if (alreadyRequested) {
            throw new BadRequestException("You have already sent a pending or approved join request for this match");
        }

        boolean hasOverlappingMatch = matchRequestRepository.existsOverlappingMatchRequest(
                userId,
                match.getPlayDate(),
                match.getStartTime(),
                match.getEndTime()
        );
        if (hasOverlappingMatch) {
            throw new BadRequestException("You already have an active or joined match request overlapping with this time range");
        }

        LocalDateTime startOfDay = match.getPlayDate().atStartOfDay();
        LocalDateTime endOfDay = match.getPlayDate().atTime(LocalTime.MAX);
        boolean hasOverlappingBooking = bookingRepository.existsOverlappingBooking(
                userId,
                startOfDay,
                endOfDay,
                match.getStartTime(),
                match.getEndTime()
        );
        if (hasOverlappingBooking) {
            throw new BadRequestException("You have an active booking overlapping with this match request time range");
        }

        if (match.getMatchingType() == com.sportvenue.entity.enums.MatchingType.TEAM_VS_TEAM) {
            if (message == null || message.trim().isEmpty()) {
                throw new BadRequestException("Team name (message) is required to join a team match");
            }
        }

        JoinRequest joinRequest = JoinRequest.builder()
                .matchRequest(match)
                .user(user)
                .requestStatus(JoinRequestStatus.PENDING)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        joinRequestRepository.save(joinRequest);
        log.info("Successfully created join request for User ID: {} on Match ID: {}", userId, matchId);
    }

    private void validateOverlappingSchedule(Integer userId, CreateMatchRequest request) {
        boolean hasOverlappingMatch = matchRequestRepository.existsOverlappingMatchRequest(
                userId,
                request.getPlayDate(),
                request.getStartTime(),
                request.getEndTime()
        );
        if (hasOverlappingMatch) {
            throw new BadRequestException(
                "You already have another open or full match request overlapping with this time range"
            );
        }

        LocalDateTime startOfDay = request.getPlayDate().atStartOfDay();
        LocalDateTime endOfDay = request.getPlayDate().atTime(LocalTime.MAX);
        boolean hasOverlappingBooking = bookingRepository.existsOverlappingBooking(
                userId,
                startOfDay,
                endOfDay,
                request.getStartTime(),
                request.getEndTime()
        );
        if (hasOverlappingBooking) {
            throw new BadRequestException("You have an active booking overlapping with this match request time range");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> getJoinRequestsForMatch(Integer matchId, Integer hostUserId) {
        log.info("Host User ID {} retrieving join requests for Match ID: {}", hostUserId, matchId);

        MatchRequest match = matchRequestRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match request not found with ID: " + matchId));

        if (!match.getUser().getUserId().equals(hostUserId)) {
            throw new BadRequestException("Only the host of this match can view join requests");
        }

        List<JoinRequest> requests = joinRequestRepository.findAllByMatchRequestMatchId(matchId);
        return requests.stream()
                .map(req -> JoinRequestResponse.builder()
                        .joinId(req.getJoinId())
                        .matchId(req.getMatchRequest().getMatchId())
                        .userId(req.getUser().getUserId())
                        .fullName(req.getUser().getFullName())
                        .email(req.getUser().getEmail())
                        .requestStatus(req.getRequestStatus())
                        .message(req.getMessage())
                        .createdAt(req.getCreatedAt())
                        .matchTitle(req.getMatchRequest().getTitle())
                        .stadiumName(req.getMatchRequest().getStadium().getStadiumName())
                        .sportName(req.getMatchRequest().getSportType().getSportName())
                        .playDate(req.getMatchRequest().getPlayDate())
                        .startTime(req.getMatchRequest().getStartTime())
                        .endTime(req.getMatchRequest().getEndTime())
                        .hostName(req.getMatchRequest().getUser().getFullName())
                        .hostEmail(req.getMatchRequest().getUser().getEmail())
                        .matchStatus(req.getMatchRequest().getMatchStatus())
                        .matchingType(req.getMatchRequest().getMatchingType())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approveJoinRequest(Integer matchId, Integer joinId, Integer hostUserId) {
        log.info("Host User ID {} approving Join ID: {} for Match ID: {}", hostUserId, joinId, matchId);

        MatchRequest match = matchRequestRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match request not found with ID: " + matchId));

        if (!match.getUser().getUserId().equals(hostUserId)) {
            throw new BadRequestException("Only the host of this match can approve join requests");
        }

        if (match.getMatchStatus() != MatchStatus.OPEN) {
            throw new BadRequestException("Match request is not open");
        }

        JoinRequest joinRequest = joinRequestRepository.findById(joinId)
                .orElseThrow(() -> new ResourceNotFoundException("Join request not found with ID: " + joinId));

        if (!joinRequest.getMatchRequest().getMatchId().equals(matchId)) {
            throw new BadRequestException("Join request does not belong to this match");
        }

        if (joinRequest.getRequestStatus() != JoinRequestStatus.PENDING) {
            throw new BadRequestException("Join request is not in PENDING status");
        }

        if (match.getCurrentPlayers() >= match.getMaxPlayers()) {
            throw new BadRequestException("Match is already full");
        }

        joinRequest.setRequestStatus(JoinRequestStatus.APPROVED);
        joinRequestRepository.save(joinRequest);

        match.setCurrentPlayers(match.getCurrentPlayers() + 1);

        if (match.getCurrentPlayers().equals(match.getMaxPlayers())) {
            match.setMatchStatus(MatchStatus.FULL);
            log.info("Match ID: {} is now FULL. Auto-rejecting remaining pending requests.", matchId);
            
            List<JoinRequest> allRequests = joinRequestRepository.findAllByMatchRequestMatchId(matchId);
            for (JoinRequest req : allRequests) {
                if (req.getRequestStatus() == JoinRequestStatus.PENDING) {
                    req.setRequestStatus(JoinRequestStatus.REJECTED);
                    joinRequestRepository.save(req);
                }
            }
        }
        matchRequestRepository.save(match);
    }

    @Override
    @Transactional
    public void rejectJoinRequest(Integer matchId, Integer joinId, Integer hostUserId) {
        log.info("Host User ID {} rejecting Join ID: {} for Match ID: {}", hostUserId, joinId, matchId);

        MatchRequest match = matchRequestRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match request not found with ID: " + matchId));

        if (!match.getUser().getUserId().equals(hostUserId)) {
            throw new BadRequestException("Only the host of this match can reject join requests");
        }

        if (match.getMatchStatus() != MatchStatus.OPEN) {
            throw new BadRequestException("Match request is not open");
        }

        JoinRequest joinRequest = joinRequestRepository.findById(joinId)
                .orElseThrow(() -> new ResourceNotFoundException("Join request not found with ID: " + joinId));

        if (!joinRequest.getMatchRequest().getMatchId().equals(matchId)) {
            throw new BadRequestException("Join request does not belong to this match");
        }

        if (joinRequest.getRequestStatus() != JoinRequestStatus.PENDING) {
            throw new BadRequestException("Join request is not in PENDING status");
        }

        joinRequest.setRequestStatus(JoinRequestStatus.REJECTED);
        joinRequestRepository.save(joinRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchResponse> getMyCreatedMatches(Integer userId) {
        log.info("Retrieving matches created by User ID: {}", userId);
        List<MatchRequest> matches = matchRequestRepository.findAllByUserUserIdOrderByCreatedAtDesc(userId);
        return matches.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JoinRequestResponse> getMyJoinedRequests(String email) {
        log.info("Retrieving join requests sent by user email: {}", email);
        List<JoinRequest> requests = joinRequestRepository.findAllByUserEmailOrderByCreatedAtDesc(email);
        return requests.stream()
                .map(req -> JoinRequestResponse.builder()
                        .joinId(req.getJoinId())
                        .matchId(req.getMatchRequest().getMatchId())
                        .userId(req.getUser().getUserId())
                        .fullName(req.getUser().getFullName())
                        .email(req.getUser().getEmail())
                        .requestStatus(req.getRequestStatus())
                        .message(req.getMessage())
                        .createdAt(req.getCreatedAt())
                        .matchTitle(req.getMatchRequest().getTitle())
                        .stadiumName(req.getMatchRequest().getStadium().getStadiumName())
                        .sportName(req.getMatchRequest().getSportType().getSportName())
                        .playDate(req.getMatchRequest().getPlayDate())
                        .startTime(req.getMatchRequest().getStartTime())
                        .endTime(req.getMatchRequest().getEndTime())
                        .hostName(req.getMatchRequest().getUser().getFullName())
                        .hostEmail(req.getMatchRequest().getUser().getEmail())
                        .matchStatus(req.getMatchRequest().getMatchStatus())
                        .matchingType(req.getMatchRequest().getMatchingType())
                        .build())
                .collect(Collectors.toList());
    }

    private MatchResponse mapToResponse(MatchRequest match) {
        return MatchResponse.builder()
                .matchId(match.getMatchId())
                .hostName(match.getUser().getFullName())
                .hostUserId(match.getUser().getUserId())
                .stadiumName(match.getStadium().getStadiumName())
                .stadiumAddress(match.getStadium().getAddress())
                .sportName(match.getSportType().getSportName())
                .title(match.getTitle())
                .description(match.getDescription())
                .playDate(match.getPlayDate())
                .startTime(match.getStartTime())
                .endTime(match.getEndTime())
                .maxPlayers(match.getMaxPlayers())
                .currentPlayers(match.getCurrentPlayers())
                .skillLevel(match.getSkillLevel())
                .splitPrice(match.getSplitPrice())
                .pricePerPlayer(match.getPricePerPlayer())
                .matchStatus(match.getMatchStatus())
                .matchingType(match.getMatchingType())
                .createdAt(match.getCreatedAt())
                .build();
    }
}
