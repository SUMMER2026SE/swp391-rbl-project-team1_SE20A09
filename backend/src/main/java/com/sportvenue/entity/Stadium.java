package com.sportvenue.entity;

import com.sportvenue.entity.enums.StadiumStatus;
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
import java.util.ArrayList;
import java.util.List;

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

    /** Chủ sân — phải là Owner đã được Approved. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    /** Loại môn thể thao của sân. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_type_id", nullable = false)
    private SportType sportType;

    @Column(name = "stadium_name", nullable = false, length = 100)
    private String stadiumName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    /** Giá thuê mỗi giờ (VNĐ). */
    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    /** Sức chứa tối đa của sân (số người). */
    @Column(name = "capacity")
    private Integer capacity;

    /** Giờ mở cửa hàng ngày. */
    @Column(name = "open_time")
    private LocalTime openTime;

    /** Giờ đóng cửa hàng ngày. */
    @Column(name = "close_time")
    private LocalTime closeTime;

    /** Trạng thái hoạt động của sân. */
    @Column(name = "stadium_status", nullable = false, length = 20)
    @Builder.Default
    private StadiumStatus stadiumStatus = StadiumStatus.AVAILABLE;

    /** Điểm đánh giá trung bình — được cập nhật sau mỗi review. */
    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.valueOf(5.0);

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Danh sách ảnh của sân — cascade xóa khi xóa sân. */
    @OneToMany(mappedBy = "stadium", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StadiumImage> images = new ArrayList<>();

    /** Danh sách khung giờ của sân. */
    @OneToMany(mappedBy = "stadium", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TimeSlot> timeSlots = new ArrayList<>();

    /** Danh sách phụ kiện cho thuê kèm sân. */
    @OneToMany(mappedBy = "stadium", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Accessory> accessories = new ArrayList<>();
}
