package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateAccessoryRequest;
import com.sportvenue.dto.response.AccessoryResponse;
import com.sportvenue.entity.Accessory;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.mapper.AccessoryMapper;
import com.sportvenue.repository.AccessoryRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.AccessoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sportvenue.entity.enums.ApprovedStatus;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessoryServiceImpl implements AccessoryService {

    private final AccessoryRepository accessoryRepository;
    private final StadiumRepository stadiumRepository;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final AccessoryMapper accessoryMapper;

    @Override
    @Transactional
    public AccessoryResponse addAccessory(Integer stadiumId, CreateAccessoryRequest request, String ownerEmail) {
        log.info("Starting addAccessory for stadiumId: {} by user: {}", stadiumId, ownerEmail);

        // 1. Tìm thông tin User từ email đăng nhập
        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + ownerEmail));

        // 2. Tìm Owner profile từ userId
        Owner owner = ownerRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không có profile chủ sân (Owner)"));

        // Kiểm tra trạng thái phê duyệt của Owner
        if (owner.getApprovedStatus() != ApprovedStatus.APPROVED) {
            log.warn("Owner ID {} with email {} is not APPROVED (status: {}) but tried to add accessory",
                    owner.getOwnerId(), ownerEmail, owner.getApprovedStatus());
            throw new BadRequestException("Tài khoản chủ sân chưa được phê duyệt!");
        }

        // 3. Tìm thông tin sân cần thêm phụ kiện
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân thể thao với ID: " + stadiumId));

        // 4. Kiểm tra xem sân này có thuộc về Owner hiện tại không (Bảo mật chéo)
        if (!stadium.getOwner().getOwnerId().equals(owner.getOwnerId())) {
            log.warn("Security alert! Owner ID {} with email {} tried to add accessory to stadium ID {} owned by Owner ID {}",
                    owner.getOwnerId(), ownerEmail, stadiumId, stadium.getOwner().getOwnerId());
            throw new BadRequestException("Bạn không có quyền quản lý sân thể thao này!");
        }

        // 5. Tạo thực thể Accessory mới
        Accessory accessory = Accessory.builder()
                .stadium(stadium)
                .name(request.getName().trim())
                .pricePerUnit(request.getPricePerUnit())
                .quantity(request.getQuantity())
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .build();

        // 6. Lưu thực thể
        Accessory savedAccessory = accessoryRepository.save(accessory);
        log.info("Successfully added accessory ID {} to stadium ID {}", savedAccessory.getAccessoryId(), stadiumId);

        // 7. Chuyển đổi và trả về Response DTO
        return accessoryMapper.toResponse(savedAccessory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessoryResponse> getAccessoriesByStadium(Integer stadiumId, String ownerEmail) {
        log.info("Starting getAccessoriesByStadium for stadiumId: {} by user: {}", stadiumId, ownerEmail);

        // 1. Tìm thông tin User từ email đăng nhập
        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + ownerEmail));

        // 2. Tìm Owner profile từ userId
        Owner owner = ownerRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không có profile chủ sân (Owner)"));

        // 3. Tìm thông tin sân
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân thể thao với ID: " + stadiumId));

        // 4. Kiểm tra xem sân này có thuộc về Owner hiện tại không (Bảo mật chéo)
        if (!stadium.getOwner().getOwnerId().equals(owner.getOwnerId())) {
            log.warn("Security alert! Owner ID {} with email {} tried to access stadium ID {} owned by Owner ID {}",
                    owner.getOwnerId(), ownerEmail, stadiumId, stadium.getOwner().getOwnerId());
            throw new BadRequestException("Bạn không có quyền quản lý sân thể thao này!");
        }

        // 5. Lấy danh sách phụ kiện và map sang DTO
        return accessoryRepository.findByStadiumStadiumId(stadiumId).stream()
                .map(accessoryMapper::toResponse)
                .toList();
    }
}
