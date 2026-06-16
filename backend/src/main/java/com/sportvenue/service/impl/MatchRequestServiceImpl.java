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
import com.sportvenue.repository.MatchRequestRepository;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.MatchRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

        // 5. Kiểm tra trùng lịch kèo ghép khác của Host
        boolean hasOverlappingMatch = matchRequestRepository.existsOverlappingMatchRequest(
                userId,
                request.getPlayDate(),
                request.getStartTime(),
                request.getEndTime()
        );
        if (hasOverlappingMatch) {
            throw new BadRequestException("You already have another open or full match request overlapping with this time range");
        }

        // 6. Kiểm tra trùng lịch đặt sân thành công (Booking) của Host
        LocalDateTime startOfDay = request.getPlayDate().atStartOfDay();
        LocalDateTime endOfDay = request.getPlayDate().atTime(java.time.LocalTime.MAX);
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
                .pricePerPlayer(request.getPricePerPlayer())
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
        log.info("Retrieving active match requests with pagination: {}", pageable);
        Page<MatchRequest> matches = matchRequestRepository.findAllByMatchStatus(MatchStatus.OPEN, pageable);
        return matches.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void joinMatch(Integer matchId, Integer userId, String message) {
        log.info("Mocking: User ID {} requested to join Match ID: {} with message: '{}'", userId, matchId, message);
        // Sẽ được triển khai chi tiết ở UC-CUS-12
    }

    private MatchResponse mapToResponse(MatchRequest match) {
        return MatchResponse.builder()
                .matchId(match.getMatchId())
                .hostName(match.getUser().getFullName())
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
                .createdAt(match.getCreatedAt())
                .build();
    }
}
