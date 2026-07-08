package com.sportvenue.controller;

import com.sportvenue.dto.request.StadiumComplexSearchRequest;
import com.sportvenue.dto.response.FacilityResponse;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.PublicComplexDetailResponse;
import com.sportvenue.dto.response.StadiumDetailResponse;
import com.sportvenue.service.PublicComplexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/public/complexes")
@RequiredArgsConstructor
@Tag(name = "PublicComplex", description = "Public endpoints for sport complexes")
@Slf4j
public class PublicComplexController {

    private final PublicComplexService publicComplexService;

    @GetMapping
    @Operation(summary = "Search complexes", description = "Allows searching and filtering complexes, with distance sorting and pagination.")
    public ResponseEntity<PageResponse<PublicComplexDetailResponse>> searchComplexes(
            @Valid @ModelAttribute StadiumComplexSearchRequest request) {
        log.info("Public API call to search complexes with query: {}", request);
        PageResponse<PublicComplexDetailResponse> response = publicComplexService.searchComplexes(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get public complex detail by ID", description = "Returns details of a specific complex, public access.")
    public ResponseEntity<PublicComplexDetailResponse> getPublicComplexById(
            @PathVariable @Positive(message = "Complex ID must be positive") Integer id) {
        log.info("Public API call to get complex detail for ID: {}", id);
        PublicComplexDetailResponse response = publicComplexService.getPublicComplexById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/facilities")
    @Operation(summary = "Get facilities under a complex", description = "Returns the list of facilities (L2) under a specific complex.")
    public ResponseEntity<List<FacilityResponse>> getFacilitiesByComplexId(
            @PathVariable @Positive(message = "Complex ID must be positive") Integer id) {
        log.info("Public API call to get facilities for complex ID: {}", id);
        List<FacilityResponse> response = publicComplexService.getFacilitiesByComplexId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/reviews")
    @Operation(summary = "Get reviews for a complex", description = "Returns reviews aggregated from all courts under this complex, public access.")
    public ResponseEntity<PageResponse<StadiumDetailResponse.ReviewDto>> getComplexReviews(
            @PathVariable @Positive(message = "Complex ID must be positive") Integer id,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page không được nhỏ hơn 0") Integer page,
            @RequestParam(defaultValue = "5") @Min(value = 1, message = "size tối thiểu là 1") @Max(value = 50, message = "size tối đa là 50") Integer size) {
        log.info("Public API call to get reviews for complex ID: {}", id);
        PageResponse<StadiumDetailResponse.ReviewDto> response =
                publicComplexService.getComplexReviews(id, page, size);
        return ResponseEntity.ok(response);
    }
}
