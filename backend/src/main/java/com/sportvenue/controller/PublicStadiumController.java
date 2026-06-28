package com.sportvenue.controller;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.StadiumDetailResponse;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.service.PublicStadiumService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/public/stadiums")
@RequiredArgsConstructor
public class PublicStadiumController {

    private final PublicStadiumService publicStadiumService;

    @GetMapping
    public ResponseEntity<PageResponse<StadiumResponse>> searchStadiums(@Valid @ModelAttribute StadiumSearchRequest request) {
        PageResponse<StadiumResponse> response = publicStadiumService.searchStadiums(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StadiumDetailResponse> getStadiumDetail(
            @PathVariable @Positive(message = "ID sân phải là số dương") Integer id) {
        StadiumDetailResponse response = publicStadiumService.getStadiumDetail(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<PageResponse<StadiumDetailResponse.ReviewDto>> getStadiumReviews(
            @PathVariable @Positive(message = "ID sân phải là số dương") Integer id,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page không được nhỏ hơn 0") Integer page,
            @RequestParam(defaultValue = "5") @Min(value = 1, message = "size tối thiểu là 1") @Max(value = 50, message = "size tối đa là 50") Integer size) {
        PageResponse<StadiumDetailResponse.ReviewDto> response =
                publicStadiumService.getStadiumReviews(id, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/complex-ref")
    public ResponseEntity<com.sportvenue.dto.response.ComplexRefResponse> getComplexRef(
            @PathVariable @Positive(message = "ID sân phải là số dương") Integer id) {
        com.sportvenue.dto.response.ComplexRefResponse response = publicStadiumService.getComplexRef(id);
        return ResponseEntity.ok(response);
    }
}
