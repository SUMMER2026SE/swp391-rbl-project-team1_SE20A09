package com.sportvenue.service;

import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.entity.enums.ComplexStatus;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.exception.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class StadiumValidationService {

    /**
     * Xác thực xem một sân lẻ (Court) có khả dụng để thực hiện các thao tác đặt sân hay không.
     * Kiểm tra trạng thái hoạt động động từ cấp Court -> Facility -> Complex.
     */
    public void validateCourtAvailableForBooking(Stadium court) {
        if (court == null) {
            throw new BadRequestException("Sân thể thao không tồn tại");
        }

        // 1. Kiểm tra node type
        if (court.getNodeType() != StadiumNodeType.COURT) {
            throw new BadRequestException("Chỉ sân lẻ (COURT) mới có thể thực hiện đặt sân");
        }

        // 2. Kiểm tra trạng thái của Sân lẻ (Court)
        if (court.getStadiumStatus() != StadiumStatus.AVAILABLE) {
            throw new BadRequestException("Sân lẻ hiện tại không khả dụng (ví dụ: đang bảo trì hoặc tạm đóng)");
        }

        // 3. Kiểm tra trạng thái của Khu vực/Môn học cha (Facility)
        Stadium facility = court.getParentStadium();
        if (facility == null) {
            throw new BadRequestException("Sân lẻ chưa được phân vào khu vực (Facility) hợp lệ");
        }
        if (facility.getStadiumStatus() != StadiumStatus.AVAILABLE) {
            throw new BadRequestException("Khu vực sân của môn thể thao này hiện tại không khả dụng");
        }

        // 4. Kiểm tra trạng thái và kiểm duyệt của Tổ hợp (Complex)
        StadiumComplex complex = court.getComplex();
        if (complex == null) {
            throw new BadRequestException("Sân thể thao chưa thuộc tổ hợp (Complex) hợp lệ");
        }
        if (complex.getComplexStatus() != ComplexStatus.AVAILABLE) {
            throw new BadRequestException("Tổ hợp sân thể thao hiện tại không khả dụng (tạm đóng)");
        }
        if (complex.getApprovedStatus() != ApprovedStatus.APPROVED) {
            throw new BadRequestException("Tổ hợp sân thể thao chưa được ban quản trị phê duyệt");
        }
    }
}
