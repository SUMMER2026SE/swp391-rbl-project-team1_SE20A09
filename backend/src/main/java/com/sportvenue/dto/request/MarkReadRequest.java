package com.sportvenue.dto.request;

import com.sportvenue.entity.enums.NotificationType;
import lombok.Data;

/**
 * Request body để đánh dấu một admin notification là đã đọc bằng resourceId.
 * Dùng khi item chưa có notificationId (chưa từng được tạo trong bảng notifications).
 */
@Data
public class MarkReadRequest {
    /** Format: "OWNER-{id}", "STADIUM-{id}", "COMPLAINT-{id}" */
    private String resourceId;
    private String title;
    private String description;
    private NotificationType type;
    /** null nếu chưa có record, non-null nếu đã có (chỉ cần update). */
    private Long notificationId;
}
