package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateStadiumRequest;
import com.sportvenue.dto.request.UpdateStadiumRequest;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.StadiumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stadiums")
@RequiredArgsConstructor
@Tag(name = "Stadium", description = "Endpoints for managing sport venues (stadiums)")
@Slf4j
public class StadiumController {

    private final StadiumService stadiumService;

    @PostMapping
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Add new stadium", description = "Allows an owner to add a new sport venue. Requires ROLE_Owner.")
    public ResponseEntity<StadiumResponse> createStadium(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateStadiumRequest request) {
        log.info("Received request to create stadium: {} from owner: {}", request.getStadiumName(), userPrincipal.getUsername());
        StadiumResponse response = stadiumService.createStadium(request, userPrincipal.getUser().getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Get my stadiums", description = "Returns all stadiums owned by the authenticated owner.")
    public ResponseEntity<List<StadiumResponse>> getMyStadiums(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<StadiumResponse> stadiums = stadiumService.getMyStadiums(userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(stadiums);
    }

    @PutMapping("/{stadiumId}")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Update stadium", description = "Allows an owner to update their stadium. Ownership is verified.")
    public ResponseEntity<StadiumResponse> updateStadium(
            @PathVariable Integer stadiumId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateStadiumRequest request) {
        log.info("Received request to update stadium ID: {} from owner: {}", stadiumId, userPrincipal.getUsername());
        StadiumResponse response = stadiumService.updateStadium(stadiumId, request, userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(response);
    }
}
