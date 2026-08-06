package com.shoppingmall.domain.activity.service;

import com.shoppingmall.domain.activity.dto.response.ProductLikeToggleResponse;
import com.shoppingmall.domain.activity.dto.response.WishlistItemResponse;
import com.shoppingmall.domain.activity.entity.ProductLike;
import com.shoppingmall.domain.activity.repository.ProductLikeRepository;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API 명세서 "일반 사용자 - 활동" 도메인 중 좋아요/위시리스트 담당.
 * ProductController 에 있던 TODO(좋아요 토글)를 여기서 구현한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductLikeService {

    private final ProductLikeRepository productLikeRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /** POST /api/v1/products/{productId}/like - 이미 좋아요한 상태면 취소, 아니면 등록 (토글) */
    @Transactional
    public ProductLikeToggleResponse toggleLike(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        return productLikeRepository.findByUserAndProduct(user, product)
                .map(existing -> {
                    // 취소는 판매중지 상품이어도 허용 (위시리스트 정리 가능해야 함)
                    productLikeRepository.delete(existing);
                    return new ProductLikeToggleResponse(false);
                })
                .orElseGet(() -> {
                    // 신규 좋아요 등록은 판매중지 상품에 대해 막는다.
                    if (!product.isOnSale()) {
                        throw new CustomException(ErrorCode.PRODUCT_NOT_ON_SALE);
                    }
                    productLikeRepository.save(ProductLike.builder().user(user).product(product).build());
                    return new ProductLikeToggleResponse(true);
                });
    }

    /** GET /api/v1/users/me/wishlist - 내 위시리스트(좋아요) 목록 조회 */
    public Page<WishlistItemResponse> getWishlist(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return productLikeRepository.findAllByUserOrderByCreatedAtDesc(user, pageable)
                .map(WishlistItemResponse::from);
    }
}
