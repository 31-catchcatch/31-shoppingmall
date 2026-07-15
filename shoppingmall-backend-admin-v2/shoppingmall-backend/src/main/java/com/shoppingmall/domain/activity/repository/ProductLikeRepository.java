package com.shoppingmall.domain.activity.repository;

import com.shoppingmall.domain.activity.entity.ProductLike;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductLikeRepository extends JpaRepository<ProductLike, Long> {

    Optional<ProductLike> findByUserAndProduct(User user, Product product);

    boolean existsByUserAndProduct(User user, Product product);

    /** GET /api/v1/users/me/wishlist - 내가 좋아요한 상품 목록 (최근 등록순) */
    Page<ProductLike> findAllByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByProduct(Product product);
}
