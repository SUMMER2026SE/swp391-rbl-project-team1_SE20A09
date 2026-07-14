package com.sportvenue.service;

import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.exception.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class AccountStatusService {

    public AccountStatus applyAdminLockState(User user, Boolean enabled) {
        if (user.getAccountStatus() == AccountStatus.PENDING) {
            throw new BadRequestException("Không thể khóa/mở khóa tài khoản đang chờ xác thực email.");
        }

        AccountStatus newStatus = Boolean.TRUE.equals(enabled) ? AccountStatus.ACTIVE : AccountStatus.BLOCKED;
        user.setAccountStatus(newStatus);
        return newStatus;
    }
}
