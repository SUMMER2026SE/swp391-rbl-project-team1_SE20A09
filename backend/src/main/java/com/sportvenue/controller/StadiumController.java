package com.sportvenue.controller;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.StadiumSearchResponse;
import com.sportvenue.service.StadiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@RestController
@RequestMapping("/api/v1/guest/stadiums")
@RequiredArgsConstructor
public class StadiumController {

    private final StadiumService stadiumService;

    @GetMapping("/search")
    public ResponseEntity<Page<StadiumSearchResponse>> searchStadiums(
            @ModelAttribute StadiumSearchRequest request,
            Pageable pageable) {
        Page<StadiumSearchResponse> result = stadiumService.searchStadiums(request, pageable);
        return ResponseEntity.ok(result);
    }
}
