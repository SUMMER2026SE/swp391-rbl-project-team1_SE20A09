package com.sportvenue.service;

import com.sportvenue.dto.request.CreateMatchRequest;
import com.sportvenue.dto.response.MatchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service Interface cho việc quản lý kèo ghép thể thao (Matchmaking).
 * Tuân thủ quy tắc lập trình hướng giao diện (Entity -> Repository -> Service Interface -> ServiceImpl -> Controller).
 */
public interface MatchRequestService {

    /**
     * Tạo một kèo ghép mới.
     *
     * @param request thông tin kèo ghép từ client
     * @param userId ID của người tạo kèo (Host)
     * @return MatchResponse thông tin kèo ghép vừa tạo
     */
    MatchResponse createMatch(CreateMatchRequest request, Integer userId);

    /**
     * Lấy danh sách các kèo ghép đang mở (OPEN) có phân trang.
     *
     * @param pageable thông tin phân trang
     * @return Page<MatchResponse> danh sách kèo ghép phân trang
     */
    Page<MatchResponse> getActiveMatches(Pageable pageable);

    /**
     * Gửi yêu cầu xin tham gia vào một kèo ghép.
     *
     * @param matchId ID của kèo ghép muốn tham gia
     * @param userId ID của người xin tham gia
     * @param message Lời nhắn đính kèm
     */
    void joinMatch(Integer matchId, Integer userId, String message);
}
