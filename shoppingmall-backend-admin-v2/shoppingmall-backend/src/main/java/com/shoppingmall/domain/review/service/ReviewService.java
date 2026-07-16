package com.shoppingmall.domain.review.service;

import com.shoppingmall.domain.order.entity.DeliveryStatus;
import com.shoppingmall.domain.order.entity.OrderDetail;
import com.shoppingmall.domain.order.repository.OrderDetailRepository;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.domain.review.dto.request.ReviewCreateRequest;
import com.shoppingmall.domain.review.dto.request.ReviewUpdateRequest;
import com.shoppingmall.domain.review.dto.response.MyReviewResponse;
import com.shoppingmall.domain.review.dto.response.ReviewResponse;
import com.shoppingmall.domain.review.entity.Review;
import com.shoppingmall.domain.review.repository.ReviewRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;

    // 1. 리뷰 작성 기능
    @Transactional
    public void createReview(Long userId, ReviewCreateRequest request) {
        if (request.getProductId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT); // "대상 상품 ID는 필수입니다."
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 💡 [V6 가드 로직] 실제 구매자가 구매 확정을 지은 이력이 있는지 확인
        OrderDetail orderDetail = orderDetailRepository.findById(request.getOrderDetailId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

        // 보안 검증: 본인 주문 내역이 맞는지 대조
        if (!orderDetail.getOrder().getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 상태 검증: 배송상태가 CONFIRMED(구매 확정)인지 확인
        if (orderDetail.getDeliveryStatus() != DeliveryStatus.CONFIRMED) {
            throw new CustomException(ErrorCode.INVALID_INPUT); // "구매 확정이 완료된 상품만 리뷰 작성이 가능합니다."
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .build();

        reviewRepository.save(review);
    }

    // 2. 특정 상품의 리뷰 목록 조회 (페이징 제공)
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        Page<Review> reviews = reviewRepository.findAllByProductAndDeletedFalseOrderByCreatedAtDesc(product, pageable);
        return reviews.map(ReviewResponse::from);
    }

    // 3. 내가 작성한 리뷰 목록 조회 (마이페이지)
    public Page<MyReviewResponse> getMyReviews(Long userId, Pageable pageable) {
        return reviewRepository.findAllByUser_IdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable)
                .map(MyReviewResponse::from);
    }

    // 4. 리뷰 수정 - 본인 작성 리뷰만 가능 (프론트 my-reviews 화면 대응)
    @Transactional
    public void updateReview(Long userId, Long reviewId, ReviewUpdateRequest request) {
        Review review = reviewRepository.findByIdAndUser_IdAndDeletedFalse(reviewId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        review.update(request.getRating(), request.getContent(), request.getImageUrl());
    }

    // 5. 리뷰 삭제 (논리 삭제) - 본인 작성 리뷰만 가능
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findByIdAndUser_IdAndDeletedFalse(reviewId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        review.delete();
    }
}