package com.sportvenue.service;

import com.sportvenue.entity.AccountStatusHistory;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.repository.AccountStatusHistoryRepository;
import com.sportvenue.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountStatusHistoryService {

    private final AccountStatusHistoryRepository accountStatusHistoryRepository;
    private final UserRepository userRepository;

    public void recordStatusChange(
            User user,
            Integer changedByUserId,
            AccountStatus previousStatus,
            AccountStatus newStatus,
            String reason) {
        User changedBy = changedByUserId == null
                ? null
                : userRepository.findById(changedByUserId).orElse(null);

        accountStatusHistoryRepository.save(AccountStatusHistory.builder()
                .user(user)
                .changedBy(changedBy)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reason(trimToNull(reason))
                .build());
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
