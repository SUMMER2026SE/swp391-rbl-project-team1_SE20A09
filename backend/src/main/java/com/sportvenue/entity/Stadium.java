package com.sportvenue.entity;

import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.FootballFieldType;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.entity.enums.StadiumNodeType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
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
import java.time.LocalTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;

/**
 * Entity ánh xạ bảng stadiums.
 * Đại diện cho một sân thể thao trong hệ thống.
 */
@Entity
@Table(name = "stadiums", indexes = {
        @Index(name = "idx_stadiums_owner_id", columnList = "owner_id"),
        @Index(name = "idx_stadiums_sport_type", columnList = "sport_type_id"),
        @Index(name = "idx_stadiums_status", columnList = "stadium_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stadium implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stadium_id")
    private Integer stadiumId;

    /** Chủ sân — có thể null đối với Court/Facility mới (resolve qua Complex). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = true)
    private Owner owner;

    /** Loại môn thể thao của sân (null đối với Court để kế thừa từ Facility). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_type_id", nullable = true)
    private SportType sportType;

    @Column(name = "stadium_name", nullable = false, length = 100)
    private String stadiumName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Địa chỉ của sân (null đối với Court/Facility để kế thừa từ Complex). */
    @Column(name = "address", nullable = true, columnDefinition = "TEXT")
    private String address;

    /** Giờ mở cửa hàng ngày. */
    @Column(name = "open_time")
    private LocalTime openTime;

    /** Giờ đóng cửa hàng ngày. */
    @Column(name = "close_time")
    private LocalTime closeTime;

    /** Giá thuê cơ bản mỗi giờ. */
    @Column(name = "price_per_hour", precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    /** Trạng thái hoạt động của sân. */
    @Enumerated(EnumType.STRING)
    @Column(name = "stadium_status", nullable = false, length = 20)
    @Builder.Default
    private StadiumStatus stadiumStatus = StadiumStatus.AVAILABLE;

    /** Trạng thái duyệt của sân (null đối với Court/Facility mới). */
    @Enumerated(EnumType.STRING)
    @Column(name = "approved_status", nullable = true, length = 20)
    private ApprovedStatus approvedStatus;

    /** Điểm đánh giá trung bình — được cập nhật sau mỗi review. */
    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.valueOf(5.0);

    /** Tổng số lượt đánh giá — được cập nhật sau mỗi review. */
    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    /** Phân loại node (FACILITY / COURT). */
    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 20)
    @Builder.Default
    private StadiumNodeType nodeType = StadiumNodeType.COURT;

    /** Loại sân bóng đá (Sân 5, Sân 7...). */
    @Enumerated(EnumType.STRING)
    @Column(name = "football_field_type", length = 20)
    private FootballFieldType footballFieldType;

    /** Tổ hợp sở hữu sân này (bắt buộc cho cả FACILITY và COURT). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complex_id")
    private StadiumComplex complex;

    /** Sân cha (chỉ dành cho COURT, trỏ về FACILITY). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_stadium_id")
    private Stadium parentStadium;

    /** Danh sách các sân con (chỉ dành cho FACILITY, chứa các COURT). */
    @OneToMany(mappedBy = "parentStadium", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Stadium> childCourts = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "admin_suspended", nullable = false)
    @Builder.Default
    private Boolean adminSuspended = false;

    @Column(name = "admin_suspended_reason", columnDefinition = "TEXT")
    private String adminSuspendedReason;

    @Column(name = "admin_suspended_at")
    private LocalDateTime adminSuspendedAt;

    /** Danh sách ảnh của sân — cascade xóa khi xóa sân. */
    @OneToMany(mappedBy = "stadium", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<StadiumImage> images = new LinkedHashSet<>();

    /** Danh sách khung giờ của sân. */
    @OneToMany(mappedBy = "stadium", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TimeSlot> timeSlots = new LinkedHashSet<>();

    /** Danh sách phụ kiện cho thuê kèm sân. */
    @OneToMany(mappedBy = "stadium", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Accessory> accessories = new LinkedHashSet<>();

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "stadium_amenities",
            joinColumns = @JoinColumn(name = "stadium_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @Builder.Default
    private Set<Amenity> amenities = new HashSet<>();

    /**
     * Lấy Owner sở hữu sân này theo cấu trúc cây (Court -> Facility -> Complex -> Owner).
     */
    public Owner resolveOwner() {
        if (this.nodeType == StadiumNodeType.FACILITY && this.complex != null) {
            return this.complex.getOwner();
        } else if (this.nodeType == StadiumNodeType.COURT && this.parentStadium != null) {
            StadiumComplex parentComplex = this.parentStadium.getComplex();
            if (parentComplex != null) {
                return parentComplex.getOwner();
            }
            return this.parentStadium.getOwner(); // Fallback if complex is missing
        }
        return this.owner; // Fallback cho dữ liệu cũ
    }
}
