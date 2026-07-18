package com.shoppingmall.domain.review.entity;

import com.shoppingmall.domain.order.entity.OrderDetail;
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

    /**
     * 어떤 구매 건(주문상품)에 대한 리뷰인지. 이 값으로 "구매 건당 리뷰 1개" 중복을 막는다.
     * 기존(이 컬럼 도입 전) 리뷰는 NULL 일 수 있어 nullable 로 둔다. 신규 리뷰는 항상 채워진다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_detail_id")
    private OrderDetail orderDetail;

    @Column(nullable = false)
    private int rating; // 별점 점수 (예: 1점 ~ 5점 제한)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 리뷰 상세 본문

    @Column(name = "image_url", length = 255)
    private String imageUrl; // 선택 사항: 첨부 포토 리뷰 이미지 링크

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted; // 논리 삭제 플래그 (DELETE /reviews/{id} 대응)

    @Builder
    public Review(User user, Product product, OrderDetail orderDetail, int rating, String content, String imageUrl) {
        this.user = user;
        this.product = product;
        this.orderDetail = orderDetail;
        this.rating = rating;
        this.content = content;
        this.imageUrl = imageUrl;
        this.deleted = false;
    }

    /** PUT /api/v1/reviews/{reviewId} - 별점/내용/사진 수정 */
    public void update(int rating, String content, String imageUrl) {
        this.rating = rating;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    /** DELETE /api/v1/reviews/{reviewId} - 논리 삭제 */
    public void delete() {
        this.deleted = true;
    }
}