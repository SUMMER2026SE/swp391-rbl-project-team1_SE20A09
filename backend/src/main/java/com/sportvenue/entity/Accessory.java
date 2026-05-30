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
 * Entity ánh xạ bảng accessories.
 * Phụ kiện cho thuê kèm theo sân (bóng, lưới, áo đồng phục,...).
 * Owner có thể thêm/sửa/xóa phụ kiện của sân mình.
 */
@Entity
@Table(name = "accessories", indexes = {
        @Index(name = "idx_accessories_stadium_id", columnList = "stadium_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Accessory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accessory_id")
    private Integer accessoryId;

    /** Sân sở hữu phụ kiện này. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    /** Tên phụ kiện (ví dụ: "Bóng đá số 5", "Lưới cầu lông"). */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Giá thuê mỗi đơn vị (VNĐ/lần). */
    @Column(name = "price_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerUnit;

    /** Số lượng hiện có trong kho. */
    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    /** false = hết hàng hoặc ngưng cho thuê, true = đang cho thuê. */
    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;
}
