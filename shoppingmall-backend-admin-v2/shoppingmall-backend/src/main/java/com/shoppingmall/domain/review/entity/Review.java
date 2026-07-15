package com.shoppingmall.domain.review.entity;

import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int rating; // 별점 점수 (예: 1점 ~ 5점 제한)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 리뷰 상세 본문

    @Column(name = "image_url", length = 255)
    private String imageUrl; // 선택 사항: 첨부 포토 리뷰 이미지 링크

    @Builder
    public Review(User user, Product product, int rating, String content, String imageUrl) {
        this.user = user;
        this.product = product;
        this.rating = rating;
        this.content = content;
        this.imageUrl = imageUrl;
    }
}