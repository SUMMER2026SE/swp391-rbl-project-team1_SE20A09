package com.sportvenue.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity lưu danh sách ảnh của Tổ hợp / Quần thể sân thể thao.
 */
@Entity
@Table(name = "stadium_complex_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StadiumComplexImage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Integer imageId;

    /** Tổ hợp sở hữu ảnh này. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complex_id", nullable = false)
    private StadiumComplex complex;

    /** URL ảnh lưu trên Cloud Storage. */
    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
