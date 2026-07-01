package com.sportvenue.entity;

import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.ComplexStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Entity đại diện cho Tổ hợp / Quần thể sân thể thao (L1).
 */
@Entity
@Table(name = "stadium_complexes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StadiumComplex implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complex_id")
    private Integer complexId;

    /** Chủ sở hữu tổ hợp — phải là Owner đã được duyệt. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "complex_status", nullable = false, length = 20)
    @Builder.Default
    private ComplexStatus complexStatus = ComplexStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "approved_status", nullable = false, length = 20)
    @Builder.Default
    private ApprovedStatus approvedStatus = ApprovedStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.valueOf(5.0);

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Danh sách các khu vực/sân lẻ thuộc tổ hợp này. */
    @OneToMany(mappedBy = "complex", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Stadium> stadiums = new LinkedHashSet<>();

    /** Album ảnh của tổ hợp. */
    @OneToMany(mappedBy = "complex", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<StadiumComplexImage> images = new LinkedHashSet<>();

    /** Danh sách tiện nghi thuộc cấp tổ hợp. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "complex_amenities",
            joinColumns = @JoinColumn(name = "complex_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @Builder.Default
    private Set<Amenity> amenities = new HashSet<>();

    /** Danh sách các môn thể thao tổ hợp này hỗ trợ. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "complex_sport_types",
            joinColumns = @JoinColumn(name = "complex_id"),
            inverseJoinColumns = @JoinColumn(name = "sport_type_id")
    )
    @Builder.Default
    private Set<SportType> sportTypes = new HashSet<>();
}
