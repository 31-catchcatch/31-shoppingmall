package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.product.entity.Category;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.repository.CategoryRepository;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.domain.seller.dto.request.SellerProductCreateRequest;
import com.shoppingmall.domain.seller.dto.request.SellerProductUpdateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerProductListResponse;
import com.shoppingmall.domain.seller.dto.response.SellerProductResponse;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 판매자 상품 관리 서비스
 *
 * 담당 API
 * GET    /api/v1/seller/products
 * POST   /api/v1/seller/products
 * PUT    /api/v1/seller/products/{productId}
 * DELETE /api/v1/seller/products/{productId}
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SellerApplicationRepository sellerApplicationRepository;

    /**
     * 판매자가 등록한 상품 목록 조회
     *
     * 1. 승인된 판매자 신청 정보 조회
     * 2. 삭제되지 않은 상품 목록 조회
     * 3. 응답 DTO로 변환
     */
    public SellerProductListResponse getMyProducts(
            Long userId,
            int page,
            int size
    ) {
        SellerApplication sellerApplication =
                getApprovedSellerApplication(userId);

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(
                normalizedPage,
                normalizedSize
        );

        Page<Product> productPage =
                productRepository
                        .findAllBySeller_IdAndDeletedFalseOrderByCreatedAtDesc(
                                sellerApplication.getId(),
                                pageable
                        );

        return SellerProductListResponse.from(productPage);
    }

    /**
     * 신규 상품 등록
     *
     * 1. 승인된 판매자 확인
     * 2. 카테고리 조회
     * 3. Product 엔티티 생성
     * 4. DB 저장
     * 5. 응답 DTO 반환
     */
    @Transactional
    public SellerProductResponse createProduct(
            Long userId,
            SellerProductCreateRequest request
    ) {
        SellerApplication sellerApplication =
                getApprovedSellerApplication(userId);

        Category category = categoryRepository
                .findById(request.categoryId())
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.CATEGORY_NOT_FOUND
                        )
                );

        Product product = Product.builder()
                .seller(sellerApplication)
                .category(category)
                .name(request.productName())
                .price(request.price())
                .discountRate(request.discountRate())
                .description(request.description())
                .thumbnailUrl(request.thumbnailUrl())
                .build();

        Product savedProduct =
                productRepository.save(product);

        return SellerProductResponse.from(savedProduct);
    }

    /**
     * 상품 정보 수정
     *
     * 1. 승인된 판매자 확인
     * 2. 상품 조회
     * 3. 상품 소유권 확인
     * 4. 카테고리 조회
     * 5. 엔티티 값 변경
     * 6. 응답 DTO 반환
     */
    @Transactional
    public SellerProductResponse updateProduct(
            Long userId,
            Long productId,
            SellerProductUpdateRequest request
    ) {
        SellerApplication sellerApplication =
                getApprovedSellerApplication(userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.PRODUCT_NOT_FOUND
                        )
                );

        validateProductOwnership(
                product,
                sellerApplication
        );

        if (product.isDeleted()) {
            throw new CustomException(
                    ErrorCode.PRODUCT_NOT_FOUND
            );
        }

        Category category = categoryRepository
                .findById(request.categoryId())
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.CATEGORY_NOT_FOUND
                        )
                );

        product.update(
                category,
                request.productName(),
                request.price(),
                request.discountRate(),
                request.description(),
                request.thumbnailUrl()
        );

        return SellerProductResponse.from(product);
    }

    /**
     * 상품 논리 삭제
     *
     * 실제 DELETE 쿼리를 실행하지 않고
     * products.is_deleted 값을 true로 변경한다.
     */
    @Transactional
    public void deleteProduct(
            Long userId,
            Long productId
    ) {
        SellerApplication sellerApplication =
                getApprovedSellerApplication(userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.PRODUCT_NOT_FOUND
                        )
                );

        validateProductOwnership(
                product,
                sellerApplication
        );

        if (product.isDeleted()) {
            throw new CustomException(
                    ErrorCode.PRODUCT_NOT_FOUND
            );
        }

        product.softDelete();
    }

    /**
     * 사용자의 가장 최근 입점 신청서를 조회하고
     * 승인 상태인지 확인한다.
     */
    private SellerApplication getApprovedSellerApplication(
            Long userId
    ) {
        SellerApplication application =
                sellerApplicationRepository
                        .findFirstByUser_IdOrderByCreatedAtDesc(userId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.SELLER_NOT_APPROVED
                                )
                        );

        if (application.getStatus()
                != SellerApplicationStatus.APPROVED) {
            throw new CustomException(
                    ErrorCode.SELLER_NOT_APPROVED
            );
        }

        return application;
    }

    /**
     * 현재 로그인한 판매자가 등록한 상품인지 확인한다.
     */
    private void validateProductOwnership(
            Product product,
            SellerApplication sellerApplication
    ) {
        Long productSellerId =
                product.getSeller().getId();

        Long currentSellerApplicationId =
                sellerApplication.getId();

        if (!productSellerId.equals(
                currentSellerApplicationId
        )) {
            throw new CustomException(
                    ErrorCode.PRODUCT_ACCESS_DENIED
            );
        }
    }
}