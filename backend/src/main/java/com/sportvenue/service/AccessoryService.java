package com.sportvenue.service;

import com.sportvenue.dto.request.CreateAccessoryRequest;
import com.sportvenue.dto.response.AccessoryResponse;

public interface AccessoryService {
    
    /**
     * Thêm phụ kiện cho thuê mới cho sân thể thao.
     *
     * @param stadiumId ID của sân
     * @param request thông tin phụ kiện cần tạo
     * @param ownerEmail email của Owner hiện tại để xác thực quyền sở hữu sân
     * @return DTO chứa thông tin phụ kiện đã được lưu thành công
     */
    AccessoryResponse addAccessory(Integer stadiumId, CreateAccessoryRequest request, String ownerEmail);

    /**
     * Lấy danh sách phụ kiện của một sân thể thao.
     *
     * @param stadiumId ID của sân
     * @param ownerEmail email của Owner để kiểm tra quyền sở hữu sân
     * @return danh sách DTO của các phụ kiện thuộc sân
     */
    java.util.List<AccessoryResponse> getAccessoriesByStadium(Integer stadiumId, String ownerEmail);
}
