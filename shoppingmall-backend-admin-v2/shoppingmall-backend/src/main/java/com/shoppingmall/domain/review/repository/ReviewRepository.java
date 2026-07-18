package com.shoppingmall.domain.review.repository;

import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 상품에 적재된 리뷰를 페이징하여 최신순으로 정렬 조회 (삭제된 리뷰 제외)
    Page<Review> findAllByProductAndDeletedFalseOrderByCreatedAtDesc(Product product, Pageable pageable);

    // GET /api/v1/users/me/reviews - 내가 작성한 리뷰 목록 (최신순, 삭제 제외)
    Page<Review> findAllByUser_IdAndDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // PUT/DELETE /api/v1/reviews/{reviewId} - 본인 소유 검증 겸 단건 조회
    java.util.Optional<Review> findByIdAndUser_IdAndDeletedFalse(Long reviewId, Long userId);

    // 같은 구매 건(주문상품)에 이미 리뷰가 있는지 - 구매 건당 리뷰 1개 중복 방지
    boolean existsByOrderDetail_IdAndDeletedFalse(Long orderDetailId);
}