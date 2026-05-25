package com.sportvenue.controller;

import com.sportvenue.dto.UserProfileResponse;
import com.sportvenue.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.sportvenue.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User profile management")
public class UserController {

    private final UserService userService;

    /**
     * Retrieves the profile of the current logged-in user.
     *
     * @param userPrincipal authenticated user principal
     * @return 200 OK with UserProfileResponse DTO
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Requires Bearer JWT Token in Header")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userService.getMyProfile(userPrincipal.getUsername()));
    }
}
