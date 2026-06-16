package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateMatchRequest;
import com.sportvenue.dto.response.MatchResponse;
import com.sportvenue.dto.response.MessageResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.MatchRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.sportvenue.dto.response.JoinRequestResponse;

/**
 * Controller xử lý các yêu cầu liên quan đến tính năng Ghép kèo thể thao (Matchmaking).
 * Được thiết kế làm mẫu chuẩn RESTful API cho cả nhóm tham khảo.
 */
@Validated
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
            @RequestParam(required = false, defaultValue = "") @Size(max = 500, message = "Message must not exceed 500 characters") String message) {

        log.info("REST request by User: {} to join Match ID: {}", userPrincipal.getUsername(), matchId);
        
        matchRequestService.joinMatch(matchId, userPrincipal.getUser().getUserId(), message);
        
        return ResponseEntity.ok(new MessageResponse("Gửi yêu cầu tham gia kèo thành công và đang chờ duyệt."));
    }

    @GetMapping("/{matchId}/participants")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Lấy danh sách các yêu cầu tham gia kèo (Host)",
            description = "Cho phép chủ kèo xem toàn bộ các yêu cầu xin tham gia kèm theo lời nhắn."
    )
    public ResponseEntity<List<JoinRequestResponse>> getJoinRequests(
            @PathVariable Integer matchId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request by Host: {} to get join requests for Match ID: {}", userPrincipal.getUsername(), matchId);
        List<JoinRequestResponse> responses =
                matchRequestService.getJoinRequestsForMatch(matchId, userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{matchId}/participants/{participantId}/approve")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Phê duyệt yêu cầu tham gia kèo (Host)",
            description = "Cho phép chủ kèo phê duyệt yêu cầu xin tham gia của người chơi hoặc đội khác."
    )
    public ResponseEntity<MessageResponse> approveJoinRequest(
            @PathVariable Integer matchId,
            @PathVariable Integer participantId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request by Host: {} to approve Join ID: {} for Match ID: {}", userPrincipal.getUsername(), participantId, matchId);
        matchRequestService.approveJoinRequest(matchId, participantId, userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(new MessageResponse("Phê duyệt yêu cầu tham gia thành công."));
    }

    @PutMapping("/{matchId}/participants/{participantId}/reject")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Từ chối yêu cầu tham gia kèo (Host)",
            description = "Cho phép chủ kèo từ chối yêu cầu xin tham gia của người chơi hoặc đội khác."
    )
    public ResponseEntity<MessageResponse> rejectJoinRequest(
            @PathVariable Integer matchId,
            @PathVariable Integer participantId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request by Host: {} to reject Join ID: {} for Match ID: {}", userPrincipal.getUsername(), participantId, matchId);
        matchRequestService.rejectJoinRequest(matchId, participantId, userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(new MessageResponse("Từ chối yêu cầu tham gia thành công."));
    }

    @GetMapping("/my-created")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Lấy danh sách kèo tôi đã tạo (Host sidebar)",
            description = "Trả về tất cả kèo ghép mà người dùng hiện tại đã tạo ra, dùng cho sidebar 'Kèo của tôi'."
    )
    public ResponseEntity<List<MatchResponse>> getMyCreatedMatches(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request by User: {} to get their created matches", userPrincipal.getUsername());
        List<MatchResponse> responses = matchRequestService.getMyCreatedMatches(userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/my-joined")
    @PreAuthorize("hasRole('Customer')")
    @Operation(
            summary = "Lấy danh sách đơn đăng ký tham gia kèo của tôi (Guest sidebar)",
            description = "Trả về tất cả các đơn đăng ký xin tham gia kèo mà người dùng hiện tại đã gửi."
    )
    public ResponseEntity<List<JoinRequestResponse>> getMyJoinedRequests(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("REST request by User: {} to get their join requests", userPrincipal.getUsername());
        List<JoinRequestResponse> responses = matchRequestService.getMyJoinedRequests(userPrincipal.getUsername());
        return ResponseEntity.ok(responses);
    }
}
