package com.shoppingmall.domain.activity.entity;

import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DB 정의서 'product_likes' 테이블 매핑.
 * POST /api/v1/products/{productId}/like (토글), GET /api/v1/users/me/wishlist 에서 사용.
 */
@Getter
@Entity
@Table(
        name = "product_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_like_user_product",
                columnNames = {"user_id", "product_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ProductLike(User user, Product product) {
        this.user = user;
        this.product = product;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
