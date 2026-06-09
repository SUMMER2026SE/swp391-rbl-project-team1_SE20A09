package com.sportvenue.controller;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.StadiumDetailResponse;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.service.PublicStadiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

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
    public ResponseEntity<StadiumDetailResponse> getStadiumDetail(@PathVariable Integer id) {
        StadiumDetailResponse response = publicStadiumService.getStadiumDetail(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<PageResponse<StadiumDetailResponse.ReviewDto>> getStadiumReviews(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        PageResponse<StadiumDetailResponse.ReviewDto> response =
                publicStadiumService.getStadiumReviews(id, page, size);
        return ResponseEntity.ok(response);
    }
}
