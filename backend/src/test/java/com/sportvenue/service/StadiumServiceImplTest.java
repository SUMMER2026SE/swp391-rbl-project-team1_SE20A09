package com.sportvenue.service;

import com.sportvenue.config.FileStorageProperties;
import com.sportvenue.dto.request.CreateStadiumRequest;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.mapper.StadiumMapper;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

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

@ExtendWith(MockitoExtension.class)
class StadiumServiceImplTest {

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private SportTypeRepository sportTypeRepository;

    @Mock
    private StadiumMapper stadiumMapper;

    private StadiumServiceImpl stadiumService;

    @BeforeEach
    void setUp() {
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        fileStorageProperties.setBaseUrl("http://localhost:8080");
        stadiumService = new StadiumServiceImpl(
                stadiumRepository,
                ownerRepository,
                sportTypeRepository,
                stadiumMapper,
                fileStorageProperties);
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

        assertThrows(ResponseStatusException.class, () -> stadiumService.createStadium(request, 1));
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
}
