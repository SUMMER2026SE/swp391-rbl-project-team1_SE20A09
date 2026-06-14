package com.sportvenue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO đại diện cho thông tin một kèo ghép trả về qua API.
 * Tuân thủ quy tắc không expose Entity trực tiếp ra ngoài Controller.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponse {

    private Integer matchId;
    private String hostName;
    private String hostEmail;
    private String stadiumName;
    private String stadiumAddress;
    private String sportName;
    private String title;
    private String description;
    private LocalDate playDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private String skillLevel;
    private Boolean splitPrice;
    private BigDecimal pricePerPlayer;
    private String matchStatus;
    private LocalDateTime createdAt;
}
