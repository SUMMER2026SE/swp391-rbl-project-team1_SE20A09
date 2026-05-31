package com.sportvenue.controller;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.service.PublicStadiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/stadiums")
@RequiredArgsConstructor
public class PublicStadiumController {

    private final PublicStadiumService publicStadiumService;

    @GetMapping
    public ResponseEntity<PageResponse<StadiumResponse>> searchStadiums(@ModelAttribute StadiumSearchRequest request) {
        PageResponse<StadiumResponse> response = publicStadiumService.searchStadiums(request);
        return ResponseEntity.ok(response);
    }
}
