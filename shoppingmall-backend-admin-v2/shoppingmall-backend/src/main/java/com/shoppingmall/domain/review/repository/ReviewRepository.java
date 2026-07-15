package com.shoppingmall.domain.review.repository;

import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 상품에 적재된 리뷰를 페이징하여 최신순으로 정렬 조회
    Page<Review> findAllByProductOrderByCreatedAtDesc(Product product, Pageable pageable);
}