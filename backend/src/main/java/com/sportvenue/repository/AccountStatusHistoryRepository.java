package com.sportvenue.repository;

import com.sportvenue.entity.AccountStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountStatusHistoryRepository extends JpaRepository<AccountStatusHistory, Long> {
}
