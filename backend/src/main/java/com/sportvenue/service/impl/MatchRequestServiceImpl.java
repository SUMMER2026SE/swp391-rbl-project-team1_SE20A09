package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateMatchRequest;
import com.sportvenue.dto.response.MatchResponse;
import com.sportvenue.entity.enums.MatchStatus;
import com.sportvenue.entity.enums.SkillLevel;
import com.sportvenue.service.MatchRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock Service Implementation cho việc quản lý kèo ghép thể thao.
 * Giúp cả nhóm chạy thử nghiệm API độc lập mà không bị lỗi thiếu Bean Spring.
 * 
 * TODO(#39): Replace mock with real implementation — blocked on UC-CUS-10/11/12
 */
@Service
@Slf4j
public class MatchRequestServiceImpl implements MatchRequestService {

    @Override
    public MatchResponse createMatch(CreateMatchRequest request, Integer userId) {
        log.info("Mocking: Creating match request with title: '{}' for user ID: {}", request.getTitle(), userId);
        
        return MatchResponse.builder()
                .matchId(101)
                .hostName("Nguyễn Huy Xuân")
                .stadiumName("Sân Bóng Đá Thủ Đức")
                .stadiumAddress("123 Võ Văn Ngân, Thủ Đức, TP.HCM")
                .sportName("Football")
                .title(request.getTitle())
                .description(request.getDescription())
                .playDate(request.getPlayDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxPlayers(request.getMaxPlayers())
                .currentPlayers(1)
                .skillLevel(request.getSkillLevel())
                .splitPrice(request.getSplitPrice())
                .pricePerPlayer(request.getPricePerPlayer())
                .matchStatus(MatchStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public Page<MatchResponse> getActiveMatches(Pageable pageable) {
        log.info("Mocking: Retrieving active match requests with pagination: {}", pageable);
        
        List<MatchResponse> matches = new ArrayList<>();
        
        matches.add(MatchResponse.builder()
                .matchId(1)
                .hostName("Nguyễn Huy Xuân")
                .stadiumName("Sân Bóng Đá Thủ Đức")
                .stadiumAddress("123 Võ Văn Ngân, Thủ Đức, TP.HCM")
                .sportName("Football")
                .title("Tìm 3 đồng đội đá bóng 5vs5 tối mai")
                .description("Nhóm mình đang có 7 người, cần tìm thêm 3 bạn đá giao lưu vui vẻ. Trình độ trung bình, không đá xấu.")
                .playDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 30))
                .maxPlayers(10)
                .currentPlayers(8)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .splitPrice(true)
                .pricePerPlayer(BigDecimal.valueOf(45000))
                .matchStatus(MatchStatus.OPEN)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());

        matches.add(MatchResponse.builder()
                .matchId(2)
                .hostName("Lý Hào Chí Anh")
                .stadiumName("Sân Cầu Lông Quận 1")
                .stadiumAddress("45 Lê Lợi, Quận 1, TP.HCM")
                .sportName("Badminton")
                .title("Giao lưu cầu lông đôi nam nữ")
                .description("Cần ghép thêm 1 cặp hoặc 2 bạn lẻ vào đánh đôi. Sân trong nhà thoáng mát.")
                .playDate(LocalDate.now().plusDays(2))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .maxPlayers(4)
                .currentPlayers(2)
                .skillLevel(SkillLevel.ADVANCED)
                .splitPrice(false)
                .pricePerPlayer(BigDecimal.ZERO)
                .matchStatus(MatchStatus.OPEN)
                .createdAt(LocalDateTime.now().minusHours(5))
                .build());

        // Phân trang giả lập đơn giản
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), matches.size());
        List<MatchResponse> pageContent = new ArrayList<>();
        if (start < matches.size() && start <= end) {
            pageContent = matches.subList(start, end);
        }
        
        return new PageImpl<>(pageContent, pageable, matches.size());
    }

    @Override
    public void joinMatch(Integer matchId, Integer userId, String message) {
        log.info("Mocking: User ID {} requested to join Match ID: {} with message: '{}'", userId, matchId, message);
        // Thực hiện giả lập thành công
    }
}
