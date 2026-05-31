package com.sportvenue.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO trả về thông tin khiếu nại của khách hàng.
 * Bao gồm phản hồi và trạng thái xử lý.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintResponse {

    private Integer complaintId;
    private Integer bookingId;

    /** Thông tin người khiếu nại. */
    private ComplainerInfo complainer;

    /** Thông tin sân liên quan. */
    private StadiumSummary stadium;

    /** Nội dung khiếu nại. */
    private String content;

    /** Trạng thái: OPEN, IN_PROGRESS, RESOLVED. */
    private String status;

    /** Phản hồi của Owner — null nếu chưa xử lý. */
    private String response;

    private LocalDateTime createdAt;

    /** Thông tin người gửi khiếu nại. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComplainerInfo {
        private Integer userId;
        private String fullName;
        private String email;
        private String phoneNumber;
    }

    /** Thông tin tóm tắt sân. */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StadiumSummary {
        private Integer stadiumId;
        private String stadiumName;
    }
}
