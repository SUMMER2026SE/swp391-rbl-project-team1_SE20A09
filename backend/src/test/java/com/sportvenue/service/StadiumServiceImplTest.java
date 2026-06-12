package com.sportvenue.service;

import com.sportvenue.config.FileStorageProperties;
import com.sportvenue.dto.request.CreateStadiumRequest;
import com.sportvenue.dto.request.UpdateStadiumRequest;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.entity.Amenity;
import com.sportvenue.exception.AppException;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ErrorCode;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.mapper.StadiumMapper;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.AmenityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class StadiumServiceImplTest {

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private SportTypeRepository sportTypeRepository;

    @Mock
    private BookingRepository bookingRepository;
    private AmenityRepository amenityRepository;

    @Mock
    private StadiumMapper stadiumMapper;

    @Mock
    private NotificationService notificationService;

    private StadiumServiceImpl stadiumService;

    @BeforeEach
    void setUp() {
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        fileStorageProperties.setBaseUrl("http://localhost:8080");
        stadiumService = new StadiumServiceImpl(
                stadiumRepository,
                ownerRepository,
                sportTypeRepository,
                bookingRepository,
                amenityRepository,
                stadiumMapper,
                fileStorageProperties,
                notificationService);
    }

    @Test
    void createStadiumSavesTrimmedVenueWithImages() {
        CreateStadiumRequest request = validRequest();
        Owner owner = approvedOwner();
        SportType sportType = SportType.builder().sportTypeId(1).sportName("Football").build();
        StadiumResponse expectedResponse = StadiumResponse.builder().stadiumId(10).build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(sportTypeRepository.findById(1)).thenReturn(Optional.of(sportType));
        when(stadiumMapper.toEntity(request)).thenAnswer(invocation -> Stadium.builder()
                .stadiumName(request.getStadiumName())
                .address(request.getAddress())
                .description(request.getDescription())
                .build());
        when(stadiumRepository.save(any(Stadium.class))).thenAnswer(invocation -> {
            Stadium stadium = invocation.getArgument(0);
            stadium.setStadiumId(10);
            return stadium;
        });
        when(stadiumMapper.toResponse(any(Stadium.class))).thenReturn(expectedResponse);

        StadiumResponse response = stadiumService.createStadium(request, 1);

        assertEquals(expectedResponse, response);
        ArgumentCaptor<Stadium> stadiumCaptor = ArgumentCaptor.forClass(Stadium.class);
        verify(stadiumRepository).save(stadiumCaptor.capture());
        Stadium savedStadium = stadiumCaptor.getValue();
        assertEquals("Stadium A", savedStadium.getStadiumName());
        assertEquals("123 Main Street", savedStadium.getAddress());
        assertEquals(1, savedStadium.getImages().size());
        assertEquals(savedStadium, savedStadium.getImages().iterator().next().getStadium());
    }

    @Test
    void createStadiumRejectsExternalImageUrl() {
        CreateStadiumRequest request = validRequest();
        request.setImageUrls(List.of("https://example.com/image.jpg"));

        assertThrows(BadRequestException.class, () -> stadiumService.createStadium(request, 1));
        verify(stadiumRepository, never()).save(any(Stadium.class));
    }

    @Test
    void createStadiumRejectsUnapprovedOwner() {
        CreateStadiumRequest request = validRequest();
        Owner owner = approvedOwner();
        owner.setApprovedStatus(ApprovedStatus.PENDING);

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));

        AppException ex = assertThrows(AppException.class, () -> stadiumService.createStadium(request, 1));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        verify(stadiumRepository, never()).save(any(Stadium.class));
    }

    @Test
    void createStadiumRejectsBlankImageUrls() {
        CreateStadiumRequest request = validRequest();
        request.setImageUrls(List.of("http://localhost:8080/api/v1/files/stadiums/ ", "  "));

        assertThrows(BadRequestException.class, () -> stadiumService.createStadium(request, 1));
        verify(stadiumRepository, never()).save(any(Stadium.class));
    }

    @Test
    void createStadiumRejectsEmptyImageUrls() {
        CreateStadiumRequest request = validRequest();
        request.setImageUrls(List.of());

        assertThrows(BadRequestException.class, () -> stadiumService.createStadium(request, 1));
        verify(stadiumRepository, never()).save(any(Stadium.class));
    }

    @Test
    void createStadiumRejectsTooManyImages() {
        CreateStadiumRequest request = validRequest();
        request.setImageUrls(java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> "http://localhost:8080/api/v1/files/stadiums/image" + i + ".jpg")
                .toList());

        assertThrows(BadRequestException.class, () -> stadiumService.createStadium(request, 1));
        verify(stadiumRepository, never()).save(any(Stadium.class));
    }

    @Test
    void createStadiumRejectsInvalidOpenCloseTime() {
        CreateStadiumRequest request = validRequest();
        request.setOpenTime(LocalTime.of(22, 0));
        request.setCloseTime(LocalTime.of(6, 0));

        assertThrows(BadRequestException.class, () -> stadiumService.createStadium(request, 1));
        verify(stadiumRepository, never()).save(any(Stadium.class));
    }

    @Test
    void createStadiumRejectsPathTraversalInImageUrl() {
        CreateStadiumRequest request = validRequest();
        request.setImageUrls(List.of("http://localhost:8080/api/v1/files/stadiums/../../../etc/passwd"));

        assertThrows(BadRequestException.class, () -> stadiumService.createStadium(request, 1));
        verify(stadiumRepository, never()).save(any(Stadium.class));
    }

    private static CreateStadiumRequest validRequest() {
        return CreateStadiumRequest.builder()
                .stadiumName(" Stadium A ")
                .address(" 123 Main Street ")
                .sportTypeId(1)
                .pricePerHour(BigDecimal.valueOf(100000))
                .latitude(BigDecimal.valueOf(10.762622))
                .longitude(BigDecimal.valueOf(106.660172))
                .description(" Description ")
                .openTime(LocalTime.of(6, 0))
                .closeTime(LocalTime.of(22, 0))
                .imageUrls(List.of("http://localhost:8080/api/v1/files/stadiums/image.jpg"))
                .build();
    }

    private static Owner approvedOwner() {
        return Owner.builder()
                .ownerId(5)
                .user(User.builder().userId(1).build())
                .approvedStatus(ApprovedStatus.APPROVED)
                .build();
    }

    @Test
    void updateStadiumSuccessfullyUpdatesAllFields() {
        UpdateStadiumRequest request = validUpdateRequest();
        Owner owner = approvedOwner();
        SportType oldSportType = SportType.builder().sportTypeId(1).sportName("Football").build();
        SportType newSportType = SportType.builder().sportTypeId(2).sportName("Basketball").build();
        Stadium stadium = Stadium.builder()
                .stadiumId(10)
                .owner(owner)
                .sportType(oldSportType)
                .stadiumName("Old Name")
                .address("Old Address")
                .build();
        StadiumResponse expectedResponse = StadiumResponse.builder().stadiumId(10).build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(sportTypeRepository.findById(2)).thenReturn(Optional.of(newSportType));
        when(stadiumRepository.save(any(Stadium.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stadiumMapper.toResponse(any(Stadium.class))).thenReturn(expectedResponse);

        StadiumResponse response = stadiumService.updateStadium(10, request, 1);

        assertEquals(expectedResponse, response);
        ArgumentCaptor<Stadium> captor = ArgumentCaptor.forClass(Stadium.class);
        verify(stadiumRepository).save(captor.capture());
        Stadium updated = captor.getValue();
        assertEquals("Updated Stadium", updated.getStadiumName());
        assertEquals("456 New Street", updated.getAddress());
        assertEquals(LocalTime.of(7, 0), updated.getOpenTime());
        assertEquals(LocalTime.of(23, 0), updated.getCloseTime());
        assertEquals(newSportType, updated.getSportType());
    }

    @Test
    void updateStadiumRejectsOwnershipViolation() {
        UpdateStadiumRequest request = validUpdateRequest();
        Owner owner = approvedOwner();
        Owner differentOwner = Owner.builder().ownerId(99).build();
        Stadium stadium = Stadium.builder()
                .stadiumId(10)
                .owner(differentOwner)
                .build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));

        AppException ex = assertThrows(AppException.class, () -> stadiumService.updateStadium(10, request, 1));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        verify(stadiumRepository, never()).save(any(Stadium.class));
    }

    @Test
    void updateStadiumRejectsInvalidCloseTime() {
        UpdateStadiumRequest request = validUpdateRequest();
        request.setOpenTime(LocalTime.of(20, 0));
        request.setCloseTime(LocalTime.of(8, 0));

        assertThrows(BadRequestException.class, () -> stadiumService.updateStadium(10, request, 1));
        verify(stadiumRepository, never()).save(any(Stadium.class));
    }

    @Test
    void updateStadiumRejectsNonExistentStadium() {
        UpdateStadiumRequest request = validUpdateRequest();
        Owner owner = approvedOwner();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(stadiumRepository.findById(10)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> stadiumService.updateStadium(10, request, 1));
        verify(stadiumRepository, never()).save(any(Stadium.class));
    }

    @Test
    void updateStadiumResetsApprovedStatusToPendingWhenNameChanges() {
        UpdateStadiumRequest request = validUpdateRequest();
        request.setStadiumName("A Completely New Name");
        Owner owner = approvedOwner();
        SportType sportType = SportType.builder().sportTypeId(2).sportName("Basketball").build();
        Stadium stadium = Stadium.builder()
                .stadiumId(10)
                .owner(owner)
                .sportType(sportType)
                .stadiumName("Old Name")
                .approvedStatus(ApprovedStatus.APPROVED)
                .build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(sportTypeRepository.findById(2)).thenReturn(Optional.of(sportType));
        when(stadiumRepository.save(any(Stadium.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stadiumMapper.toResponse(any(Stadium.class))).thenReturn(StadiumResponse.builder().build());

        stadiumService.updateStadium(10, request, 1);

        ArgumentCaptor<Stadium> captor = ArgumentCaptor.forClass(Stadium.class);
        verify(stadiumRepository).save(captor.capture());
        assertEquals(ApprovedStatus.PENDING, captor.getValue().getApprovedStatus());
    }

    @Test
    void updateStadiumDoesNotResetApprovedStatusWhenNameDoesNotChange() {
        UpdateStadiumRequest request = validUpdateRequest();
        request.setStadiumName("Old Name");
        Owner owner = approvedOwner();
        SportType sportType = SportType.builder().sportTypeId(2).sportName("Basketball").build();
        Stadium stadium = Stadium.builder()
                .stadiumId(10)
                .owner(owner)
                .sportType(sportType)
                .stadiumName("Old Name")
                .approvedStatus(ApprovedStatus.APPROVED)
                .build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(sportTypeRepository.findById(2)).thenReturn(Optional.of(sportType));
        when(stadiumRepository.save(any(Stadium.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stadiumMapper.toResponse(any(Stadium.class))).thenReturn(StadiumResponse.builder().build());

        stadiumService.updateStadium(10, request, 1);

        ArgumentCaptor<Stadium> captor = ArgumentCaptor.forClass(Stadium.class);
        verify(stadiumRepository).save(captor.capture());
        assertEquals(ApprovedStatus.APPROVED, captor.getValue().getApprovedStatus());
    }

    @Test
    void updateStadiumSyncsAmenitiesSuccessfully() {
        UpdateStadiumRequest request = validUpdateRequest();
        request.setAmenityIds(List.of(1, 2));
        Owner owner = approvedOwner();
        SportType sportType = SportType.builder().sportTypeId(2).sportName("Basketball").build();
        Stadium stadium = Stadium.builder()
                .stadiumId(10)
                .owner(owner)
                .sportType(sportType)
                .stadiumName("Old Name")
                .amenities(new java.util.HashSet<>())
                .build();

        List<Amenity> mockAmenities = List.of(
                Amenity.builder().amenityId(1).name("Wifi").build(),
                Amenity.builder().amenityId(2).name("Parking").build()
        );

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(sportTypeRepository.findById(2)).thenReturn(Optional.of(sportType));
        when(amenityRepository.findAllById(List.of(1, 2))).thenReturn(mockAmenities);
        when(stadiumRepository.save(any(Stadium.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stadiumMapper.toResponse(any(Stadium.class))).thenReturn(StadiumResponse.builder().build());

        stadiumService.updateStadium(10, request, 1);

        ArgumentCaptor<Stadium> captor = ArgumentCaptor.forClass(Stadium.class);
        verify(stadiumRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getAmenities().size());
    }

    @Test
    void updateStadiumUpdatesStadiumStatusSuccessfully() {
        UpdateStadiumRequest request = validUpdateRequest();
        request.setStadiumStatus(StadiumStatus.MAINTENANCE);
        Owner owner = approvedOwner();
        SportType sportType = SportType.builder().sportTypeId(2).sportName("Basketball").build();
        Stadium stadium = Stadium.builder()
                .stadiumId(10)
                .owner(owner)
                .sportType(sportType)
                .stadiumName("Old Name")
                .stadiumStatus(StadiumStatus.AVAILABLE)
                .build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(stadium));
        when(sportTypeRepository.findById(2)).thenReturn(Optional.of(sportType));
        when(stadiumRepository.save(any(Stadium.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stadiumMapper.toResponse(any(Stadium.class))).thenReturn(StadiumResponse.builder().build());

        stadiumService.updateStadium(10, request, 1);

        ArgumentCaptor<Stadium> captor = ArgumentCaptor.forClass(Stadium.class);
        verify(stadiumRepository).save(captor.capture());
        assertEquals(StadiumStatus.MAINTENANCE, captor.getValue().getStadiumStatus());
    }

    private static UpdateStadiumRequest validUpdateRequest() {
        return UpdateStadiumRequest.builder()
                .stadiumName(" Updated Stadium ")
                .address(" 456 New Street ")
                .sportTypeId(2)
                .pricePerHour(BigDecimal.valueOf(150000))
                .capacity(20)
                .latitude(BigDecimal.valueOf(10.762622))
                .longitude(BigDecimal.valueOf(106.660172))
                .description("Updated description")
                .openTime(LocalTime.of(7, 0))
                .closeTime(LocalTime.of(23, 0))
                .build();
    }
}
