package com.shoppingmall.domain.product.entity;

import com.shoppingmall.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 브랜드 즐겨찾기(좋아요). POST /api/v1/brands/{brandId}/like (토글) 대응.
 * product_likes 와 동일한 패턴 - (user_id, brand_id) UNIQUE.
 */
@Getter
@Entity
@Table(
        name = "brand_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_brand_like_user_brand",
                columnNames = {"user_id", "brand_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public BrandLike(User user, Brand brand) {
        this.user = user;
        this.brand = brand;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
