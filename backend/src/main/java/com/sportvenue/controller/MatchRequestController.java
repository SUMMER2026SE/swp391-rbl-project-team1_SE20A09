package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateMatchRequest;
import com.sportvenue.dto.response.MatchResponse;
import com.sportvenue.dto.response.MessageResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.MatchRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller xử lý các yêu cầu liên quan đến tính năng Ghép kèo thể thao (Matchmaking).
 * Được thiết kế làm mẫu chuẩn RESTful API cho cả nhóm tham khảo.
 */
@RestController
@RequestMapping("/api/v1/matchmaking")
@RequiredArgsConstructor
@Tag(name = "Matchmaking", description = "Quản lý tính năng ghép kèo chơi thể thao (USP)")
@Slf4j
public class MatchRequestController {

    private final MatchRequestService matchRequestService;

    @PostMapping
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Tạo kèo ghép mới (Customer)",
            description = "Cho phép khách hàng đã đăng nhập tạo một yêu cầu ghép đội chơi thể thao mới."
    )
    public ResponseEntity<MatchResponse> createMatch(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateMatchRequest request) {

        log.info("REST request to create match: '{}' by User: {}", request.getTitle(), userPrincipal.getUsername());
        
        MatchResponse response = matchRequestService.createMatch(request, userPrincipal.getUser().getUserId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Lấy danh sách các kèo ghép đang mở (Phân trang)",
            description = "Trả về toàn bộ các kèo ghép có trạng thái là OPEN kèm theo phân trang."
    )
    public ResponseEntity<Page<MatchResponse>> getActiveMatches(Pageable pageable) {
        log.info("REST request to get all active match requests with pagination: {}", pageable);
        
        Page<MatchResponse> responses = matchRequestService.getActiveMatches(pageable);
        
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{matchId}/join")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Gửi yêu cầu xin tham gia kèo ghép (Customer)",
            description = "Cho phép khách hàng khác đăng ký xin tham gia vào kèo ghép của chủ kèo."
    )
    public ResponseEntity<MessageResponse> joinMatch(
            @PathVariable Integer matchId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false, defaultValue = "") String message) {

        log.info("REST request by User: {} to join Match ID: {}", userPrincipal.getUsername(), matchId);
        
        matchRequestService.joinMatch(matchId, userPrincipal.getUser().getUserId(), message);
        
        return ResponseEntity.ok(new MessageResponse("Gửi yêu cầu tham gia kèo thành công và đang chờ duyệt."));
    }
}
