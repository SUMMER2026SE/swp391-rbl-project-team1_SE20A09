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
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.JoinRequestRepository;
import com.sportvenue.repository.MatchRequestRepository;
import com.sportvenue.repository.specification.MatchRequestSpecification;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.StadiumComplexRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.entity.JoinRequest;
import com.sportvenue.entity.enums.JoinRequestStatus;
import com.sportvenue.dto.response.JoinRequestResponse;
import com.sportvenue.service.MaintenanceScheduleService;
import com.sportvenue.service.MatchRequestService;
import com.sportvenue.service.ChatService;
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
import java.util.Set;
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
    private final ChatService chatService;
    private final StadiumComplexRepository stadiumComplexRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final MaintenanceScheduleService maintenanceScheduleService;

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

        // 2. Phân giải Complex ID và thiết lập các thông tin ưu tiên (hỗ trợ tương thích ngược)
        ResolvedTargetComplex resolved = resolveTargetComplex(request);
        final Integer targetComplexId = resolved.targetComplexId();
        final StadiumComplex complex = resolved.complex();
        final Stadium legacyStadium = resolved.legacyStadium();

        // 3. Kiểm tra trạng thái hoạt động của Complex (nếu có) hoặc Stadium legacy
        validateComplexOrStadiumAvailability(complex, legacyStadium);

        // 4 & 5. Nếu truyền preferredFacilityId / preferredCourtId, kiểm tra tính hợp lệ
        final Stadium preferredFacility = request.getPreferredFacilityId() != null
                ? validatePreferredFacility(request.getPreferredFacilityId(), targetComplexId)
                : resolved.preferredFacility();
        final Stadium preferredCourt = request.getPreferredCourtId() != null
                ? validatePreferredCourt(request.getPreferredCourtId(), targetComplexId, preferredFacility)
                : resolved.preferredCourt();

        // 5b. Kiểm tra bảo trì theo khung ngày (MaintenanceSchedule) cho playDate — tách khỏi bước 3
        // vì cần biết preferredCourt/preferredFacility (chính xác hơn complex chung chung) trước.
        // Trước PR này chỉ check complexStatus/stadiumStatus tĩnh, bỏ sót bảo trì có khung ngày.
        validateNotUnderMaintenance(complex, legacyStadium, preferredFacility, preferredCourt, request.getPlayDate());

        // 6. Kiểm tra Sport Type tồn tại
        SportType sportType = sportTypeRepository.findById(request.getSportTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Sport Type not found with ID: " + request.getSportTypeId()));

        // 7. Đảm bảo Tổ hợp (hoặc Sân/Khu vực ưu tiên) hỗ trợ đúng môn thể thao
        validateSportTypeSupport(complex, legacyStadium, preferredFacility, preferredCourt, sportType);

        // 8. Validate không tạo kèo với giờ đã qua
        LocalDateTime slotStart = LocalDateTime.of(request.getPlayDate(), request.getStartTime());
        if (!slotStart.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Cannot create matchmaking request for a past time");
        }

        // 9. Kiểm tra trùng lịch kèo ghép và Booking của Host
        validateOverlappingSchedule(userId, request);

        // 10. Kiểm tra conflict sân trống ở cấp Complex (chỉ áp dụng cho luồng mới chọn Complex)
        if (request.getComplexId() != null) {
            validateComplexCourtAvailability(request);
        }

        // 11. Tạo và lưu MatchRequest
        return createAndSaveMatch(request, user, complex, legacyStadium, preferredFacility, preferredCourt, sportType);
    }

    private ResolvedTargetComplex resolveTargetComplex(CreateMatchRequest request) {
        Integer targetComplexIdOpt = request.getComplexId();
        StadiumComplex resolvedComplex = null;
        Stadium resolvedPreferredFacility = null;
        Stadium resolvedPreferredCourt = null;
        Stadium resolvedLegacyStadium = null;

        if (targetComplexIdOpt == null) {
            if (request.getStadiumId() == null) {
                throw new BadRequestException("Either Complex ID or Stadium ID must be provided");
            }
            resolvedLegacyStadium = stadiumRepository.findByIdWithComplexAndParent(request.getStadiumId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stadium not found with ID: " + request.getStadiumId()));
            
            if (resolvedLegacyStadium.getNodeType() != StadiumNodeType.COURT) {
                throw new BadRequestException("The provided legacy Stadium ID must be of node type COURT");
            }
            
            resolvedComplex = resolvedLegacyStadium.getComplex();
            if (resolvedComplex != null) {
                targetComplexIdOpt = resolvedComplex.getComplexId();
                resolvedPreferredCourt = resolvedLegacyStadium;
                resolvedPreferredFacility = resolvedLegacyStadium.getParentStadium();
            }
        } else {
            final Integer finalComplexIdLookup = targetComplexIdOpt;
            resolvedComplex = stadiumComplexRepository.findById(finalComplexIdLookup)
                    .orElseThrow(() -> new ResourceNotFoundException("Complex not found with ID: " + finalComplexIdLookup));
        }

        return new ResolvedTargetComplex(targetComplexIdOpt, resolvedComplex, resolvedPreferredFacility, resolvedPreferredCourt, resolvedLegacyStadium);
    }

    private record ResolvedTargetComplex(
            Integer targetComplexId,
            StadiumComplex complex,
            Stadium preferredFacility,
            Stadium preferredCourt,
            Stadium legacyStadium) { }

    private MatchResponse createAndSaveMatch(
            CreateMatchRequest request,
            User user,
            StadiumComplex complex,
            Stadium legacyStadium,
            Stadium preferredFacility,
            Stadium preferredCourt,
            SportType sportType) {
        MatchRequest matchRequest = MatchRequest.builder()
                .user(user)
                .stadium(legacyStadium != null ? legacyStadium : preferredCourt)
                .complex(complex)
                .preferredFacility(preferredFacility)
                .preferredCourt(preferredCourt)
                .sportType(sportType)
                .title(request.getTitle())
                .description(request.getDescription())
                .playDate(request.getPlayDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxPlayers(request.getMaxPlayers())
                .currentPlayers(1)
                .skillLevel(request.getSkillLevel())
                .splitPrice(request.getSplitPrice())
                .pricePerPlayer(request.getPricePerPlayer() != null ? request.getPricePerPlayer() : BigDecimal.ZERO)
                .matchingType(request.getMatchingType())
                .matchStatus(MatchStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        MatchRequest savedMatch = matchRequestRepository.save(matchRequest);
        log.info("Successfully created match request with ID: {}", savedMatch.getMatchId());

        // Create empty group chat for the host immediately upon match creation
        chatService.createOrUpdateMatchGroupChat(savedMatch, user.getUserId());

        return mapToResponse(savedMatch);
    }

    private void validateComplexOrStadiumAvailability(StadiumComplex complex, Stadium legacyStadium) {
        if (complex != null) {
            if (complex.getComplexStatus() != ComplexStatus.AVAILABLE) {
                throw new BadRequestException("Complex is currently not available (CLOSED or under MAINTENANCE)");
            }
            if (complex.getApprovedStatus() != ApprovedStatus.APPROVED) {
                throw new BadRequestException("Complex is not approved yet");
            }
        } else if (legacyStadium != null) {
            if (legacyStadium.getStadiumStatus() != StadiumStatus.AVAILABLE) {
                throw new BadRequestException("Stadium is currently not available (e.g. CLOSED or under MAINTENANCE)");
            }
            if (legacyStadium.getApprovedStatus() != ApprovedStatus.APPROVED) {
                throw new BadRequestException("Stadium is not approved yet");
            }
        }
    }

    /**
     * {@link #validateComplexOrStadiumAvailability} chỉ check cờ tĩnh (stadiumStatus/complexStatus —
     * bảo trì vô thời hạn). Bảo trì có khung ngày ({@link com.sportvenue.entity.MaintenanceSchedule})
     * cố tình KHÔNG đổi 2 cờ đó, nên cần check riêng qua {@code MaintenanceScheduleService} — dùng
     * target cụ thể nhất đã biết (Court > Facility > Stadium legacy > Complex chung chung).
     */
    private void validateNotUnderMaintenance(StadiumComplex complex, Stadium legacyStadium,
                                              Stadium preferredFacility, Stadium preferredCourt, LocalDate playDate) {
        if (preferredCourt != null) {
            if (maintenanceScheduleService.isStadiumUnderMaintenance(preferredCourt, playDate)) {
                throw new BadRequestException("Selected court has a scheduled maintenance window on " + playDate);
            }
        } else if (preferredFacility != null) {
            if (maintenanceScheduleService.isStadiumUnderMaintenance(preferredFacility, playDate)) {
                throw new BadRequestException("Selected facility has a scheduled maintenance window on " + playDate);
            }
        } else if (legacyStadium != null) {
            if (maintenanceScheduleService.isStadiumUnderMaintenance(legacyStadium, playDate)) {
                throw new BadRequestException("Selected stadium has a scheduled maintenance window on " + playDate);
            }
        } else if (complex != null) {
            if (maintenanceScheduleService.isComplexUnderMaintenance(complex, playDate)) {
                throw new BadRequestException("Selected complex has a scheduled maintenance window on " + playDate);
            }
        }
    }

    private Stadium validatePreferredFacility(Integer preferredFacilityId, Integer targetComplexId) {
        if (preferredFacilityId == null) {
            return null;
        }
        Stadium facility = stadiumRepository.findById(preferredFacilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Preferred Facility not found with ID: " + preferredFacilityId));
        if (facility.getNodeType() != StadiumNodeType.FACILITY) {
            throw new BadRequestException("Preferred Facility ID must refer to a FACILITY node type");
        }
        if (facility.getComplex() == null || !facility.getComplex().getComplexId().equals(targetComplexId)) {
            throw new BadRequestException("Preferred Facility does not belong to the selected Complex");
        }
        return facility;
    }

    private Stadium validatePreferredCourt(Integer preferredCourtId, Integer targetComplexId, Stadium preferredFacility) {
        if (preferredCourtId == null) {
            return null;
        }
        Stadium court = stadiumRepository.findById(preferredCourtId)
                .orElseThrow(() -> new ResourceNotFoundException("Preferred Court not found with ID: " + preferredCourtId));
        if (court.getNodeType() != StadiumNodeType.COURT) {
            throw new BadRequestException("Preferred Court ID must refer to a COURT node type");
        }
        if (court.getComplex() == null || !court.getComplex().getComplexId().equals(targetComplexId)) {
            throw new BadRequestException("Preferred Court does not belong to the selected Complex");
        }
        if (preferredFacility != null && (court.getParentStadium() == null || !court.getParentStadium().getStadiumId().equals(preferredFacility.getStadiumId()))) {
            throw new BadRequestException("Preferred Court does not belong to the selected Facility");
        }
        return court;
    }

    private void validateSportTypeSupport(StadiumComplex complex, Stadium legacyStadium, Stadium preferredFacility, Stadium preferredCourt, SportType sportType) {
        if (complex != null) {
            if (preferredCourt != null) {
                Stadium facilityOfCourt = preferredCourt.getParentStadium();
                SportType courtSportType = preferredCourt.getSportType() != null ? preferredCourt.getSportType() :
                        (facilityOfCourt != null ? facilityOfCourt.getSportType() : null);
                if (courtSportType == null || !courtSportType.getSportTypeId().equals(sportType.getSportTypeId())) {
                    throw new BadRequestException("Preferred Court does not support the sport type: " + sportType.getSportName());
                }
            } else if (preferredFacility != null) {
                if (preferredFacility.getSportType() == null || !preferredFacility.getSportType().getSportTypeId().equals(sportType.getSportTypeId())) {
                    throw new BadRequestException("Preferred Facility does not support the sport type: " + sportType.getSportName());
                }
            } else {
                boolean complexSupportsSport = complex.getSportTypes().stream()
                        .anyMatch(st -> st.getSportTypeId().equals(sportType.getSportTypeId()));
                if (!complexSupportsSport) {
                    throw new BadRequestException("Selected Complex does not support the sport type: " + sportType.getSportName());
                }
            }
        } else {
            if (!legacyStadium.getSportType().getSportTypeId().equals(sportType.getSportTypeId())) {
                throw new BadRequestException("The selected stadium does not support the sport type: " + sportType.getSportName());
            }
        }
    }

    private void validateComplexCourtAvailability(CreateMatchRequest request) {
        List<Stadium> candidateCourts;
        if (request.getPreferredCourtId() != null) {
            Stadium court = stadiumRepository.findById(request.getPreferredCourtId())
                    .orElseThrow(() -> new ResourceNotFoundException("Court not found"));
            candidateCourts = List.of(court);
        } else if (request.getPreferredFacilityId() != null) {
            Stadium facility = stadiumRepository.findById(request.getPreferredFacilityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Facility not found"));
            candidateCourts = stadiumRepository.findCourtsByFacilityId(request.getPreferredFacilityId());
        } else {
            candidateCourts = stadiumRepository.findCourtsByComplexId(request.getComplexId());
        }

        if (candidateCourts.isEmpty()) {
            throw new BadRequestException("No courts found in the selected range");
        }

        List<Integer> courtIds = candidateCourts.stream()
                .map(Stadium::getStadiumId)
                .collect(Collectors.toList());

        List<TimeSlot> allSlots = timeSlotRepository.findOverlappingSlotsByStadiumIds(
                courtIds, request.getStartTime(), request.getEndTime());

        List<BookingStatus> activeStatuses = Arrays.asList(
                BookingStatus.PENDING_PAYMENT, BookingStatus.PENDING, BookingStatus.CONFIRMED);
        Set<String> bookedKeys = bookingRepository.findActiveBookingKeysByStadiumIds(
                courtIds, request.getPlayDate(), activeStatuses)
                .stream()
                .map(row -> row[0] + "-" + row[1])
                .collect(Collectors.toSet());

        boolean hasAvailableCourt = allSlots.stream()
                .filter(slot -> slot.getSlotStatus() == com.sportvenue.entity.enums.SlotStatus.AVAILABLE)
                .anyMatch(slot -> !bookedKeys.contains(
                        slot.getStadium().getStadiumId() + "-" + slot.getSlotId()));

        if (!hasAvailableCourt) {
            throw new BadRequestException("No available court in the selected complex/facility during the requested time slot");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResponse getMatch(Integer matchId) {
        log.info("Retrieving match detail for ID: {}", matchId);
        MatchRequest match = matchRequestRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match request not found with ID: " + matchId));
        return mapToResponse(match);
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
    @Transactional(readOnly = true)
    public Page<MatchResponse> getActiveMatches(Pageable pageable, String location) {
        return getActiveMatches(pageable, location, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MatchResponse> getActiveMatches(Pageable pageable, String location, Integer sportTypeId) {
        log.info("Retrieving active match requests with pagination, location={}, sportTypeId={} and future dates: {}",
                location, sportTypeId, pageable);
        LocalDate nowDate = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        Page<MatchRequest> matches = matchRequestRepository.findAll(
                MatchRequestSpecification.withDynamicFilter(
                        Arrays.asList(MatchStatus.OPEN, MatchStatus.FULL),
                        nowDate,
                        nowTime,
                        location,
                        sportTypeId),
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

        validateNoScheduleConflicts(userId, match);

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

    private void validateNoScheduleConflicts(Integer userId, MatchRequest match) {
        boolean hasOverlappingMatch = matchRequestRepository.existsOverlappingMatchRequest(
                userId, match.getPlayDate(), match.getStartTime(), match.getEndTime());
        if (hasOverlappingMatch) {
            throw new BadRequestException("You already have an active or joined match request overlapping with this time range");
        }

        boolean hasApprovedOverlap = joinRequestRepository.existsApprovedOverlappingJoinRequest(
                userId, match.getPlayDate(), match.getStartTime(), match.getEndTime());
        if (hasApprovedOverlap) {
            throw new BadRequestException("You are already approved in another match overlapping with this time range");
        }

        LocalDateTime startOfDay = match.getPlayDate().atStartOfDay();
        LocalDateTime endOfDay = match.getPlayDate().atTime(LocalTime.MAX);
        boolean hasOverlappingBooking = bookingRepository.existsOverlappingBooking(
                userId, startOfDay, endOfDay, match.getStartTime(), match.getEndTime());
        if (hasOverlappingBooking) {
            throw new BadRequestException("You have an active booking overlapping with this match request time range");
        }
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
                        .stadiumName(req.getMatchRequest().getStadium() != null ? req.getMatchRequest().getStadium().getStadiumName() :
                                (req.getMatchRequest().getPreferredCourt() != null ? req.getMatchRequest().getPreferredCourt().getStadiumName() :
                                        (req.getMatchRequest().getPreferredFacility() != null ? req.getMatchRequest().getPreferredFacility().getStadiumName() :
                                                (req.getMatchRequest().getComplex() != null ? req.getMatchRequest().getComplex().getName() : null))))
                        .sportName(req.getMatchRequest().getSportType().getSportName())
                        .playDate(req.getMatchRequest().getPlayDate())
                        .startTime(req.getMatchRequest().getStartTime())
                        .endTime(req.getMatchRequest().getEndTime())
                        .hostName(req.getMatchRequest().getUser().getFullName())
                        .hostEmail(req.getMatchRequest().getUser().getEmail())
                        .hostUserId(req.getMatchRequest().getUser().getUserId())
                        .matchStatus(req.getMatchRequest().getMatchStatus())
                        .matchingType(req.getMatchRequest().getMatchingType())
                        .cancelReason(req.getMatchRequest().getCancelReason())
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
        joinRequestRepository.saveAndFlush(joinRequest);

        // Atomic increment để tránh race condition khi approve đồng thời
        int updated = matchRequestRepository.incrementCurrentPlayers(matchId);
        if (updated == 0) {
            throw new BadRequestException("Match is already full");
        }

        // Reload để lấy currentPlayers mới nhất sau atomic update
        MatchRequest updatedMatch = matchRequestRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match request not found"));

        if (updatedMatch.getCurrentPlayers().equals(updatedMatch.getMaxPlayers())) {
            matchRequestRepository.updateStatusAndReason(matchId, MatchStatus.FULL, updatedMatch.getCancelReason());
            log.info("Match ID: {} is now FULL. Auto-rejecting remaining pending requests.", matchId);
            joinRequestRepository.bulkUpdateStatus(
                    matchId,
                    JoinRequestStatus.REJECTED,
                    Arrays.asList(JoinRequestStatus.PENDING)
            );
        }

        // Create or update group chat
        chatService.createOrUpdateMatchGroupChat(updatedMatch, joinRequest.getUser().getUserId());
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
    public Page<MatchResponse> getMyCreatedMatches(Integer userId, Pageable pageable) {
        log.info("Retrieving matches created by User ID: {}", userId);
        return matchRequestRepository.findAllByUserUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JoinRequestResponse> getMyJoinedRequests(String email, Pageable pageable) {
        log.info("Retrieving join requests sent by user email: {}", email);
        return joinRequestRepository.findAllByUserEmailOrderByCreatedAtDesc(email, pageable)
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
                        .stadiumName(req.getMatchRequest().getStadium() != null ? req.getMatchRequest().getStadium().getStadiumName() :
                                (req.getMatchRequest().getPreferredCourt() != null ? req.getMatchRequest().getPreferredCourt().getStadiumName() :
                                        (req.getMatchRequest().getPreferredFacility() != null ? req.getMatchRequest().getPreferredFacility().getStadiumName() :
                                                (req.getMatchRequest().getComplex() != null ? req.getMatchRequest().getComplex().getName() : null))))
                        .sportName(req.getMatchRequest().getSportType().getSportName())
                        .playDate(req.getMatchRequest().getPlayDate())
                        .startTime(req.getMatchRequest().getStartTime())
                        .endTime(req.getMatchRequest().getEndTime())
                        .hostName(req.getMatchRequest().getUser().getFullName())
                        .hostEmail(req.getMatchRequest().getUser().getEmail())
                        .hostUserId(req.getMatchRequest().getUser().getUserId())
                        .matchStatus(req.getMatchRequest().getMatchStatus())
                        .matchingType(req.getMatchRequest().getMatchingType())
                        .cancelReason(req.getMatchRequest().getCancelReason())
                        .build());
    }

    @Override
    @Transactional
    public void cancelMatch(Integer matchId, Integer userId, String reason) {
        log.info("User ID {} requesting to cancel Match ID: {} with reason: {}", userId, matchId, reason);

        MatchRequest match = matchRequestRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match request not found with ID: " + matchId));

        // 1. Kiểm tra quyền sở hữu (Phải là Host)
        if (!match.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Only the host of this match can cancel it");
        }

        // 2. Kiểm tra trạng thái hợp lệ để hủy
        if (match.getMatchStatus() != MatchStatus.OPEN && match.getMatchStatus() != MatchStatus.FULL) {
            throw new BadRequestException("Match request cannot be cancelled in its current status: " + match.getMatchStatus());
        }

        // 3. Kiểm tra thời gian (Kèo chưa diễn ra)
        LocalDate nowDate = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        if (match.getPlayDate().isBefore(nowDate) || 
            (match.getPlayDate().isEqual(nowDate) && match.getStartTime().isBefore(nowTime))) {
            throw new BadRequestException("Cannot cancel a match that has already started or finished");
        }

        // 4. Cập nhật trạng thái kèo sang CANCELLED và lý do hủy
        String finalReason = (reason == null || reason.trim().isEmpty()) ? "Chủ nhà không cung cấp lý do cụ thể" : reason.trim();
        matchRequestRepository.updateStatusAndReason(matchId, MatchStatus.CANCELLED, finalReason);

        // 5. Bulk update các JoinRequest liên quan sang CANCELLED
        int affectedRows = joinRequestRepository.bulkUpdateStatus(
                matchId,
                JoinRequestStatus.CANCELLED,
                Arrays.asList(JoinRequestStatus.PENDING, JoinRequestStatus.APPROVED)
        );

        log.info("Successfully cancelled Match ID: {}. Affected {} join requests.", matchId, affectedRows);
    }

    private MatchResponse mapToResponse(MatchRequest match) {
        return MatchResponse.builder()
                .matchId(match.getMatchId())
                .hostName(match.getUser().getFullName())
                .hostUserId(match.getUser().getUserId())
                .stadiumName(match.getStadium() != null ? match.getStadium().getStadiumName() :
                        (match.getPreferredCourt() != null ? match.getPreferredCourt().getStadiumName() :
                                (match.getPreferredFacility() != null ? match.getPreferredFacility().getStadiumName() :
                                        (match.getComplex() != null ? match.getComplex().getName() : null))))
                .stadiumAddress(match.getStadium() != null && match.getStadium().getAddress() != null ? match.getStadium().getAddress() :
                        (match.getComplex() != null ? match.getComplex().getAddress() : null))
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
                .cancelReason(match.getCancelReason())
                .createdAt(match.getCreatedAt())
                .build();
    }
}
