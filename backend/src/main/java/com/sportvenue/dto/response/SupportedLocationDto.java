package com.sportvenue.dto.response;

import java.util.List;

/**
 * Danh sách tỉnh/thành + quận/huyện hỗ trợ tìm kiếm theo location — nguồn dữ liệu duy nhất là
 * {@link com.sportvenue.util.location.VietnamLocationReference}, dùng cho dropdown ở frontend.
 * Đặt tên khác LocationDTO (đã có sẵn, dùng cho kết quả Goong geocoding) để tránh nhầm lẫn.
 */
public record SupportedLocationDto(String province, List<String> districts) {
}
