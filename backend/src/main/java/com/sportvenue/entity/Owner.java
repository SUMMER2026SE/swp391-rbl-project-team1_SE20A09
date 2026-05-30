package com.sportvenue.entity;

import com.sportvenue.entity.enums.ApprovedStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng owners.
 * Lưu thông tin profile kinh doanh của chủ sân (tách biệt với bảng users).
 */
@Entity
@Table(name = "owners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Owner implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "owner_id")
    private Integer ownerId;

    /** Liên kết 1-1 với User — một user chỉ có một owner profile. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "business_name", length = 100)
    private String businessName;

    @Column(name = "tax_code", length = 30)
    private String taxCode;

    @Column(name = "business_address", columnDefinition = "TEXT")
    private String businessAddress;

    /** Trạng thái phê duyệt chủ sân từ Admin. */
    @Enumerated(EnumType.STRING)
    @Column(name = "approved_status", nullable = false, length = 20)
    @Builder.Default
    private ApprovedStatus approvedStatus = ApprovedStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
