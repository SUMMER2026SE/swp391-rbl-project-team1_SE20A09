package com.sportvenue.service;

import com.sportvenue.dto.request.CreateSportTypeRequest;
import com.sportvenue.dto.response.SportTypeResponse;
import com.sportvenue.entity.SportType;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.mapper.SportTypeMapper;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.MatchRequestRepository;
import com.sportvenue.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SportTypeServiceImplTest {

    @Mock
    private SportTypeRepository sportTypeRepository;

    @Mock
    private SportTypeMapper sportTypeMapper;

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private MatchRequestRepository matchRequestRepository;

    @InjectMocks
    private SportTypeServiceImpl sportTypeService;

    @Test
    void createSportType_Success() {
        CreateSportTypeRequest request = CreateSportTypeRequest.builder()
                .sportName("Tennis")
                .nameEn("Tennis")
                .sportCode("TENNIS")
                .description("Tennis category")
                .isActive(true)
                .isFootballType(false)
                .build();

        SportType savedEntity = SportType.builder()
                .sportTypeId(1)
                .sportName(request.getSportName())
                .nameEn(request.getNameEn())
                .sportCode(request.getSportCode())
                .description(request.getDescription())
                .isActive(request.getIsActive())
                .isFootballType(request.getIsFootballType())
                .build();

        SportTypeResponse response = SportTypeResponse.builder()
                .sportTypeId(1)
                .sportName("Tennis")
                .sportCode("TENNIS")
                .build();

        when(sportTypeRepository.existsBySportName(request.getSportName())).thenReturn(false);
        when(sportTypeRepository.existsBySportCode(request.getSportCode())).thenReturn(false);
        when(sportTypeRepository.save(any(SportType.class))).thenReturn(savedEntity);
        when(sportTypeMapper.toResponse(any(SportType.class))).thenReturn(response);

        SportTypeResponse result = sportTypeService.createSportType(request);

        assertNotNull(result);
        assertEquals("Tennis", result.getSportName());
        assertEquals("TENNIS", result.getSportCode());
        verify(sportTypeRepository).save(any(SportType.class));
    }

    @Test
    void createSportType_DuplicateName_ThrowsException() {
        CreateSportTypeRequest request = CreateSportTypeRequest.builder()
                .sportName("Tennis")
                .sportCode("TENNIS")
                .build();

        when(sportTypeRepository.existsBySportName(request.getSportName())).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                sportTypeService.createSportType(request));

        assertTrue(ex.getMessage().contains("Tên loại môn thể thao đã tồn tại"));
        verify(sportTypeRepository, never()).save(any());
    }

    @Test
    void createSportType_DuplicateCode_ThrowsException() {
        CreateSportTypeRequest request = CreateSportTypeRequest.builder()
                .sportName("Tennis")
                .sportCode("TENNIS")
                .build();

        when(sportTypeRepository.existsBySportName(request.getSportName())).thenReturn(false);
        when(sportTypeRepository.existsBySportCode(request.getSportCode())).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                sportTypeService.createSportType(request));

        assertTrue(ex.getMessage().contains("Mã môn thể thao đã tồn tại"));
        verify(sportTypeRepository, never()).save(any());
    }

    @Test
    void deleteSportType_SportTypeNotFound_ThrowsException() {
        when(sportTypeRepository.findById(1)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                sportTypeService.deleteSportType(1));

        assertTrue(ex.getMessage().contains("Không tìm thấy loại môn thể thao với ID: 1"));
        verify(sportTypeRepository, never()).delete(any());
        verify(sportTypeRepository, never()).save(any());
    }

    @Test
    void deleteSportType_InUseInStadium_DeactivatesSportType() {
        SportType sportType = SportType.builder()
                .sportTypeId(1)
                .isActive(true)
                .build();

        when(sportTypeRepository.findById(1)).thenReturn(Optional.of(sportType));
        when(stadiumRepository.existsBySportTypeSportTypeId(1)).thenReturn(true);

        sportTypeService.deleteSportType(1);

        assertFalse(sportType.getIsActive());
        verify(sportTypeRepository).save(sportType);
        verify(sportTypeRepository, never()).delete(any());
    }

    @Test
    void deleteSportType_InUseInMatchRequest_DeactivatesSportType() {
        SportType sportType = SportType.builder()
                .sportTypeId(1)
                .isActive(true)
                .build();

        when(sportTypeRepository.findById(1)).thenReturn(Optional.of(sportType));
        when(stadiumRepository.existsBySportTypeSportTypeId(1)).thenReturn(false);
        when(matchRequestRepository.existsBySportTypeSportTypeId(1)).thenReturn(true);

        sportTypeService.deleteSportType(1);

        assertFalse(sportType.getIsActive());
        verify(sportTypeRepository).save(sportType);
        verify(sportTypeRepository, never()).delete(any());
    }

    @Test
    void deleteSportType_NotInUse_HardDeletesSportType() {
        SportType sportType = SportType.builder()
                .sportTypeId(1)
                .isActive(true)
                .build();

        when(sportTypeRepository.findById(1)).thenReturn(Optional.of(sportType));
        when(stadiumRepository.existsBySportTypeSportTypeId(1)).thenReturn(false);
        when(matchRequestRepository.existsBySportTypeSportTypeId(1)).thenReturn(false);

        sportTypeService.deleteSportType(1);

        verify(sportTypeRepository).delete(sportType);
        verify(sportTypeRepository, never()).save(any());
    }
}
