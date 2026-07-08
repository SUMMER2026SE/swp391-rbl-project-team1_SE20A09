package com.sportvenue.repository;

import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Modifying
    void deleteAllByIsVerifiedFalseAndCreatedAtBefore(LocalDateTime threshold);

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndUserIdNot(String phoneNumber, Integer userId);

    /**
     * Tìm danh sách user theo role, hỗ trợ search (tên/email) và lọc theo accountStatus.
     * Dùng FETCH JOIN để tránh N+1 query trên bảng roles.
     * Note: Hibernate will not log warning HHH90003004 because role is @ManyToOne, 
     * but always verify integration tests logs to ensure it's not applied in memory.
     */
    @Query("""
        SELECT u FROM User u JOIN FETCH u.role r
        WHERE r.roleName = :roleName
          AND (:search IS NULL OR :search = ''
               OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:accountStatus IS NULL OR u.accountStatus = :accountStatus)
    """)
    @EntityGraph(attributePaths = {"role"})
    Page<User> findByRoleWithFilters(
            @Param("roleName") String roleName,
            @Param("search") String search,
            @Param("accountStatus") AccountStatus accountStatus,
            Pageable pageable
    );

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.roleName = :roleName")
    long countByRoleName(@Param("roleName") String roleName);

    /**
     * Search users by first/last name for starting new chat conversations.
     * Limited to 20 results, only returns ACTIVE users.
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.accountStatus = com.sportvenue.entity.enums.AccountStatus.ACTIVE
          AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY u.firstName, u.lastName
    """)
    List<User> searchByName(@Param("query") String query);

    /** Lấy danh sách tất cả Admin user — dùng để gửi notification cho admin. */
    @Query("SELECT u FROM User u WHERE u.role.roleName = 'Admin'")
    List<User> findAllAdmins();
}

