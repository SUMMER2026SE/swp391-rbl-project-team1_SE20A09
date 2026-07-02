package com.sportvenue.service;

import com.sportvenue.dto.request.CreateStadiumRequest;
import com.sportvenue.dto.request.UpdateStadiumRequest;
import com.sportvenue.dto.request.CreateFacilityRequest;
import com.sportvenue.dto.request.CreateCourtRequest;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.config.FileStorageProperties;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.repository.StadiumComplexRepository;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumImage;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.exception.AppException;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ErrorCode;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.mapper.StadiumMapper;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.AmenityRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StadiumServiceImpl implements StadiumService {

    private final StadiumRepository stadiumRepository;
    private final OwnerRepository ownerRepository;
    private final MaintenanceScheduleService maintenanceScheduleService;
    private final SportTypeRepository sportTypeRepository;
    private final BookingRepository bookingRepository;
    private final AmenityRepository amenityRepository;
    private final StadiumMapper stadiumMapper;
    private final FileStorageProperties fileStorageProperties;
    private final NotificationService notificationService;
    private final com.sportvenue.repository.UserRepository userRepository;
    private final Environment env;
    private final StadiumComplexRepository stadiumComplexRepository;

    @Override
    @Transactional
    public StadiumResponse createStadium(CreateStadiumRequest request, Integer userId) {
        normalizeRequest(request);
        log.info("Creating stadium for user: {}", userId);

        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        if (owner.getApprovedStatus() != ApprovedStatus.APPROVED) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        SportType sportType = sportTypeRepository.findById(request.getSportTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Sport type not found with ID: " + request.getSportTypeId()));

        Stadium stadium = stadiumMapper.toEntity(request);
        stadium.setOwner(owner);
        stadium.setSportType(sportType);
        stadium.setStadiumStatus(StadiumStatus.AVAILABLE);
        stadium.setApprovedStatus(ApprovedStatus.PENDING);
        // Phải set FACILITY (không phải COURT) vì sân này là top-level,
        // không có parent. Trigger check_stadium_parent_node_type() yêu cầu
        // COURT phải có parent_stadium_id — FACILITY thì không cần.
        stadium.setNodeType(StadiumNodeType.FACILITY);

        // Tự động tạo StadiumComplex wrapper để thỏa mãn ràng buộc cấu trúc cây
        // (FACILITY bắt buộc phải thuộc về 1 Complex).
        StadiumComplex autoComplex = StadiumComplex.builder()
                .owner(owner)
                .name(request.getStadiumName())
                .address(request.getAddress())
                .latitude(request.getLatitude() != null ? request.getLatitude().doubleValue() : null)
                .longitude(request.getLongitude() != null ? request.getLongitude().doubleValue() : null)
                .sportTypes(java.util.Set.of(sportType))
                .build();
        StadiumComplex savedComplex = stadiumComplexRepository.save(autoComplex);
        stadium.setComplex(savedComplex);

        List<StadiumImage> images = request.getImageUrls().stream()
                .map(url -> StadiumImage.builder()
                        .stadium(stadium)
                        .imageUrl(url)
                        .build())
                .toList();
        stadium.getImages().addAll(images);

        Stadium savedStadium = stadiumRepository.save(stadium);
        log.info("Successfully created stadium with ID: {}", savedStadium.getStadiumId());

        // Thông báo cho tất cả Admin: có sân mới chờ duyệt
        String ownerName = owner.getUser() != null ? owner.getUser().getFullName() : "N/A";
        String resourceId = "STADIUM-" + savedStadium.getStadiumId();
        userRepository.findAllAdmins().forEach(admin ->
            notificationService.createNotification(
                admin.getUserId(),
                "Sân mới chờ duyệt",
                "\"" + savedStadium.getStadiumName() + "\" của " + ownerName + " đang chờ phê duyệt",
                com.sportvenue.entity.enums.NotificationType.STADIUM_APPROVAL,
                resourceId
            )
        );

        return stadiumMapper.toResponse(savedStadium);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<StadiumResponse> getMyStadiums(Integer userId, String search, Integer sportTypeId, String status) {
        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        org.springframework.data.jpa.domain.Specification<Stadium> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            
            // Must belong to this owner
            predicates.add(cb.equal(root.get("owner").get("ownerId"), owner.getOwnerId()));

            // Exclude CLOSED (deleted) stadiums
            predicates.add(cb.notEqual(root.get("stadiumStatus"), StadiumStatus.CLOSED));
            
            
            // Keyword search on name
            if (org.springframework.util.StringUtils.hasText(search)) {
                predicates.add(cb.like(cb.lower(root.get("stadiumName")), "%" + search.trim().toLowerCase() + "%"));
            }
            
            // Filter by sport type
            if (sportTypeId != null) {
                predicates.add(cb.equal(root.get("sportType").get("sportTypeId"), sportTypeId));
            }
            
            // Filter by status (AVAILABLE, MAINTENANCE)
            if (org.springframework.util.StringUtils.hasText(status)) {
                try {
                    StadiumStatus statusEnum = StadiumStatus.valueOf(status.toUpperCase());
                    predicates.add(cb.equal(root.get("stadiumStatus"), statusEnum));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid status enum
                }
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        LocalDate today = LocalDate.now();
        List<Stadium> stadiums = stadiumRepository.findAll(spec);
        // Batch — tối đa 2 query bất kể danh sách dài bao nhiêu, thay vì gọi isStadiumUnderMaintenance
        // (1-2 query/lần) lặp lại cho từng sân.
        java.util.Map<Integer, Boolean> maintenanceByStadiumId = maintenanceScheduleService.isUnderMaintenanceToday(stadiums, today);
        return stadiums.stream()
                .map(stadium -> {
                    StadiumResponse response = stadiumMapper.toResponse(stadium);
                    response.setUnderMaintenanceToday(maintenanceByStadiumId.getOrDefault(stadium.getStadiumId(), false));
                    return response;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StadiumResponse getStadiumByIdAndOwner(Integer stadiumId, Integer userId) {
        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Stadium not found with ID: " + stadiumId));

        Owner resolvedOwner = stadium.resolveOwner();
        if (resolvedOwner == null || !resolvedOwner.getOwnerId().equals(owner.getOwnerId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        StadiumResponse response = stadiumMapper.toResponse(stadium);
        response.setUnderMaintenanceToday(maintenanceScheduleService.isStadiumUnderMaintenance(stadium, LocalDate.now()));
        return response;
    }

    @Override
    @Transactional
    public StadiumResponse updateStadium(Integer stadiumId, UpdateStadiumRequest request, Integer userId) {
        normalizeUpdateRequest(request);
        log.info("Updating stadium ID: {} for user: {}", stadiumId, userId);

        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Stadium not found with ID: " + stadiumId));

        Owner resolvedOwner = stadium.resolveOwner();
        if (resolvedOwner == null || !resolvedOwner.getOwnerId().equals(owner.getOwnerId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        SportType sportType = sportTypeRepository.findById(request.getSportTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Sport type not found with ID: " + request.getSportTypeId()));

        boolean nameChanged = !stadium.getStadiumName().equals(request.getStadiumName());
        boolean imagesChanged = false;

        java.util.List<String> currentImageUrls = stadium.getImages().stream()
                .map(StadiumImage::getImageUrl)
                .toList();
        
        java.util.List<String> newImageUrls = request.getImageUrls();
        if (newImageUrls != null) {
            newImageUrls = newImageUrls.stream()
                    .map(this::trimToNull)
                    .filter(url -> url != null)
                    .toList();
            validateStadiumImageUrls(newImageUrls);

            imagesChanged = !new java.util.HashSet<>(currentImageUrls).equals(new java.util.HashSet<>(newImageUrls));
            
            // Clear and reload images
            stadium.getImages().clear();
            for (String url : newImageUrls) {
                stadium.getImages().add(StadiumImage.builder()
                        .stadium(stadium)
                        .imageUrl(url)
                        .build());
            }
        }

        if (nameChanged || imagesChanged) {
            stadium.setApprovedStatus(ApprovedStatus.PENDING);
        }

        stadium.setStadiumName(request.getStadiumName());
        stadium.setAddress(request.getAddress());
        stadium.setDescription(request.getDescription());
        stadium.setSportType(sportType);
        stadium.setOpenTime(request.getOpenTime());
        stadium.setCloseTime(request.getCloseTime());
        stadium.setPricePerHour(request.getPricePerHour());
        stadium.setLatitude(request.getLatitude().doubleValue());
        stadium.setLongitude(request.getLongitude().doubleValue());

        // Sync amenities if provided in the update request
        if (request.getAmenityIds() != null) {
            java.util.List<com.sportvenue.entity.Amenity> amenitiesList = amenityRepository.findAllById(request.getAmenityIds());
            if (amenitiesList.size() != request.getAmenityIds().size()) {
                throw new ResourceNotFoundException("Some amenities were not found");
            }
            stadium.getAmenities().clear();
            stadium.getAmenities().addAll(amenitiesList);
        }

        // Update stadiumStatus safely if provided
        if (request.getStadiumStatus() != null) {
            stadium.setStadiumStatus(request.getStadiumStatus());
        }

        Stadium updatedStadium = stadiumRepository.save(stadium);
        log.info("Successfully updated stadium with ID: {}", updatedStadium.getStadiumId());

        return stadiumMapper.toResponse(updatedStadium);
    }

    @Override
    @Transactional
    public StadiumResponse approveStadium(Integer stadiumId) {
        log.info("Approving stadium with ID: {}", stadiumId);
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Stadium not found with ID: " + stadiumId));
        stadium.setApprovedStatus(ApprovedStatus.APPROVED);
        Stadium saved = stadiumRepository.save(stadium);
        return stadiumMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public StadiumResponse rejectStadium(Integer stadiumId) {
        log.info("Rejecting stadium with ID: {}", stadiumId);
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Stadium not found with ID: " + stadiumId));
        stadium.setApprovedStatus(ApprovedStatus.REJECTED);
        Stadium saved = stadiumRepository.save(stadium);
        return stadiumMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<StadiumResponse> getAllStadiums(String approvedStatus) {
        log.info("Admin fetching all stadiums with approvedStatus filter: {}", approvedStatus);
        java.util.List<Stadium> stadiums;
        if (org.springframework.util.StringUtils.hasText(approvedStatus)) {
            try {
                ApprovedStatus statusEnum = ApprovedStatus.valueOf(approvedStatus.toUpperCase());
                stadiums = stadiumRepository.findByApprovedStatus(statusEnum);
            } catch (IllegalArgumentException e) {
                stadiums = stadiumRepository.findAllWithDetails();
            }
        } else {
            stadiums = stadiumRepository.findAllWithDetails();
        }
        return stadiums.stream()
                .map(stadiumMapper::toResponse)
                .toList();
    }

    private void normalizeRequest(CreateStadiumRequest request) {
        request.setStadiumName(trimToNull(request.getStadiumName()));
        request.setAddress(trimToNull(request.getAddress()));
        request.setDescription(trimToNull(request.getDescription()));
        if (request.getOpenTime() != null && request.getCloseTime() != null
                && !request.getCloseTime().isAfter(request.getOpenTime())) {
            throw new BadRequestException("Close time must be after open time");
        }
        if (request.getImageUrls() == null || request.getImageUrls().isEmpty()) {
            throw new BadRequestException("At least one stadium image is required");
        }
        if (request.getImageUrls().size() > 10) {
            throw new BadRequestException("Cannot upload more than 10 images");
        }
        request.setImageUrls(request.getImageUrls().stream()
                .map(this::trimToNull)
                .toList());
        if (request.getImageUrls().stream().anyMatch(url -> url == null)) {
            throw new BadRequestException("Image URL cannot be blank");
        }
        validateStadiumImageUrls(request.getImageUrls());
    }

    private void validateStadiumImageUrls(List<String> imageUrls) {
        String baseUrl = fileStorageProperties.getBaseUrl();
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String allowedPrefix = normalizedBaseUrl + "/api/v1/files/stadiums/";
        boolean isDevOrTest = env.acceptsProfiles(Profiles.of("dev", "test"));

        boolean hasInvalidUrl = imageUrls.stream()
                .anyMatch(url -> {
                    // Allow uploaded local stadium images
                    if (url.startsWith(allowedPrefix) && !url.contains("..") && url.length() > allowedPrefix.length()) {
                        return false;
                    }
                    // Allow external web URLs (http/https) only for mockups & seeding compatibility in dev/test profiles
                    if (isDevOrTest && (url.startsWith("http://") || url.startsWith("https://")) && !url.contains("..")) {
                        return false;
                    }
                    return true;
                });
        if (hasInvalidUrl) {
            throw new BadRequestException("Image URLs must be uploaded through the stadium image endpoint");
        }
    }

    private void normalizeUpdateRequest(UpdateStadiumRequest request) {
        request.setStadiumName(trimToNull(request.getStadiumName()));
        request.setAddress(trimToNull(request.getAddress()));
        request.setDescription(trimToNull(request.getDescription()));
        if (request.getOpenTime() != null && request.getCloseTime() != null
                && !request.getCloseTime().isAfter(request.getOpenTime())) {
            throw new BadRequestException("Close time must be after open time");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional
    public void suspendStadium(Integer stadiumId, Integer userId) {
        log.info("Owner {} requesting suspend for stadium {}", userId, stadiumId);
        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Stadium not found with ID: " + stadiumId));

        Owner resolvedOwner = stadium.resolveOwner();
        if (resolvedOwner == null || !resolvedOwner.getOwnerId().equals(owner.getOwnerId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        stadium.setStadiumStatus(StadiumStatus.MAINTENANCE);
        stadiumRepository.save(stadium);
        log.info("Stadium {} successfully suspended by owner {}", stadiumId, userId);
    }

    @Override
    @Transactional
    public void activateStadium(Integer stadiumId, Integer userId) {
        log.info("Owner {} requesting activation for stadium {}", userId, stadiumId);
        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Stadium not found with ID: " + stadiumId));

        Owner resolvedOwner = stadium.resolveOwner();
        if (resolvedOwner == null || !resolvedOwner.getOwnerId().equals(owner.getOwnerId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        stadium.setStadiumStatus(StadiumStatus.AVAILABLE);
        stadiumRepository.save(stadium);
        log.info("Stadium {} successfully activated by owner {}", stadiumId, userId);
    }

    @Override
    @Transactional
    public void deleteStadium(Integer stadiumId, Integer userId) {
        log.info("Owner {} requesting delete for stadium {}", userId, stadiumId);
        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Stadium not found with ID: " + stadiumId));

        Owner resolvedOwner = stadium.resolveOwner();
        if (resolvedOwner == null || !resolvedOwner.getOwnerId().equals(owner.getOwnerId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        stadium.setStadiumStatus(StadiumStatus.CLOSED);
        stadium.setDeletedAt(LocalDateTime.now());
        stadiumRepository.save(stadium);

        if (stadium.getNodeType() == StadiumNodeType.FACILITY) {
            List<Stadium> childCourts = stadiumRepository.findCourtsByFacilityId(stadiumId);
            for (Stadium court : childCourts) {
                court.setStadiumStatus(StadiumStatus.CLOSED);
                court.setDeletedAt(LocalDateTime.now());
                stadiumRepository.save(court);
                cancelFutureBookingsForCourt(court);
            }
        }

        cancelFutureBookingsForCourt(stadium);
        log.info("Stadium {} successfully soft-deleted by owner {}", stadiumId, userId);
    }

    private void cancelFutureBookingsForCourt(Stadium stadium) {
        List<Booking> futureBookings = bookingRepository.findFutureBookingsByStadiumId(stadium.getStadiumId(), java.time.LocalDate.now(), java.time.LocalTime.now());
        log.info("Found {} future bookings to cancel for stadium {}", futureBookings.size(), stadium.getStadiumId());
        for (Booking booking : futureBookings) {
            booking.setBookingStatus(BookingStatus.CANCELLED);
            booking.setNote("Sân bị đóng cửa vĩnh viễn bởi chủ sân.");
            if (booking.getSlot() != null) {
                booking.getSlot().setSlotStatus(com.sportvenue.entity.enums.SlotStatus.AVAILABLE);
            }
            if (booking.getPaymentStatus() == com.sportvenue.entity.enums.PaymentStatus.PAID) {
                booking.setPaymentStatus(com.sportvenue.entity.enums.PaymentStatus.REFUNDED);
            }
            bookingRepository.save(booking);

            notificationService.createNotification(
                    booking.getUser().getUserId(),
                    "Đơn đặt sân bị hủy",
                    String.format("Đơn đặt sân %s của bạn đã bị hủy do sân ngừng hoạt động vĩnh viễn. Vui lòng liên hệ để được hoàn tiền nếu có.", stadium.getStadiumName()),
                    com.sportvenue.entity.enums.NotificationType.BOOKING,
                    booking.getBookingId().toString()
            );
        }
    }

    @Override
    @Transactional
    public StadiumResponse createFacility(CreateFacilityRequest request, Integer userId) {
        log.info("Creating Facility under Complex ID: {} by user ID: {}", request.getComplexId(), userId);

        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        if (owner.getApprovedStatus() != ApprovedStatus.APPROVED) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        StadiumComplex complex = stadiumComplexRepository.findById(request.getComplexId())
                .orElseThrow(() -> new ResourceNotFoundException("Complex not found with ID: " + request.getComplexId()));

        if (!complex.getOwner().getOwnerId().equals(owner.getOwnerId())) {
            throw new BadRequestException("You do not own this complex");
        }

        SportType sportType = sportTypeRepository.findById(request.getSportTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Sport type not found with ID: " + request.getSportTypeId()));

        // Kiểm tra xem Complex có hỗ trợ môn thể thao này không
        boolean supportsSport = complex.getSportTypes().stream()
                .anyMatch(st -> st.getSportTypeId().equals(sportType.getSportTypeId()));
        if (!supportsSport) {
            throw new BadRequestException("Complex does not support this sport type: " + sportType.getSportName());
        }

        Stadium facility = Stadium.builder()
                .complex(complex)
                .owner(owner)
                .sportType(sportType)
                .stadiumName(request.getStadiumName().trim())
                .description(request.getDescription())
                .openTime(request.getOpenTime())
                .closeTime(request.getCloseTime())
                .nodeType(StadiumNodeType.FACILITY)
                .stadiumStatus(StadiumStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .build();

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            List<StadiumImage> images = request.getImageUrls().stream()
                    .map(url -> StadiumImage.builder()
                            .stadium(facility)
                            .imageUrl(url)
                            .build())
                    .toList();
            facility.getImages().addAll(images);
        }

        Stadium savedFacility = stadiumRepository.save(facility);
        log.info("Successfully created Facility with ID: {}", savedFacility.getStadiumId());

        return stadiumMapper.toResponse(savedFacility);
    }

    @Override
    @Transactional
    public StadiumResponse createCourt(CreateCourtRequest request, Integer userId) {
        log.info("Creating Court under Facility ID: {} by user ID: {}", request.getParentStadiumId(), userId);

        Owner owner = ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner profile not found for user ID: " + userId));

        if (owner.getApprovedStatus() != ApprovedStatus.APPROVED) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Stadium parent = stadiumRepository.findById(request.getParentStadiumId())
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + request.getParentStadiumId()));

        if (parent.getNodeType() != StadiumNodeType.FACILITY) {
            throw new BadRequestException("Parent node is not a FACILITY");
        }

        StadiumComplex complex = parent.getComplex();
        if (complex == null) {
            throw new BadRequestException("Facility does not belong to any Complex");
        }

        if (!complex.getOwner().getOwnerId().equals(owner.getOwnerId())) {
            throw new BadRequestException("You do not own this complex");
        }

        Stadium court = Stadium.builder()
                .complex(complex)
                .parentStadium(parent)
                .owner(owner)
                .sportType(parent.getSportType()) // Kế thừa sportType từ Facility
                .stadiumName(request.getStadiumName().trim())
                .description(request.getDescription())
                .pricePerHour(request.getPricePerHour())
                .openTime(parent.getOpenTime()) // Kế thừa giờ đóng/mở cửa từ Facility
                .closeTime(parent.getCloseTime())
                .nodeType(StadiumNodeType.COURT)
                .stadiumStatus(StadiumStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .build();

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            List<StadiumImage> images = request.getImageUrls().stream()
                    .map(url -> StadiumImage.builder()
                            .stadium(court)
                            .imageUrl(url)
                            .build())
                    .toList();
            court.getImages().addAll(images);
        }

        Stadium savedCourt = stadiumRepository.save(court);
        log.info("Successfully created Court with ID: {}", savedCourt.getStadiumId());

        return stadiumMapper.toResponse(savedCourt);
    }
}
