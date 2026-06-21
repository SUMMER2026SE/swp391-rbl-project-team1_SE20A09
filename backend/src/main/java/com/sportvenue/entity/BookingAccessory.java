package com.sportvenue.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * UC-CUS-01: Phụ kiện kèm theo một booking — snapshot giá tại thời điểm đặt.
 *
 * <p>{@code unitPrice} được lưu cứng (denormalized) để giá trong lịch sử booking
 * không bị ảnh hưởng khi Owner cập nhật giá accessory sau này.</p>
 */
@Entity
@Table(name = "booking_accessories", indexes = {
        @Index(name = "idx_booking_accessories_booking_id", columnList = "booking_id"),
        @Index(name = "idx_booking_accessories_accessory_id", columnList = "accessory_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingAccessory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Booking cha — không cascade delete để giữ lịch sử. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** ID phụ kiện (không FK cứng để tránh cascade lock với bảng accessories). */
    @Column(name = "accessory_id", nullable = false)
    private Integer accessoryId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Giá thuê một đơn vị TẠI THỜI ĐIỂM TẠO BOOKING (snapshot). */
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
}
