package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.product.entity.Brand;
import com.shoppingmall.domain.product.entity.Category;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductImage;
import com.shoppingmall.domain.product.entity.ProductOption;
import com.shoppingmall.domain.product.entity.ProductStatus;
import com.shoppingmall.domain.product.repository.BrandRepository;
import com.shoppingmall.domain.product.repository.CategoryRepository;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.domain.seller.dto.request.ProductOptionRequest;
import com.shoppingmall.domain.seller.dto.request.SellerProductCreateRequest;
import com.shoppingmall.domain.seller.dto.request.SellerProductUpdateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerProductDetailResponse;
import com.shoppingmall.domain.seller.dto.response.SellerProductListResponse;
import com.shoppingmall.domain.seller.dto.response.SellerProductResponse;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import com.shoppingmall.global.validation.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
    private final BrandRepository brandRepository;
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
            String filter,
            int page,
            int size
    ) {
        SellerApplication sellerApplication =
                getApprovedSellerApplication(userId);
        Long sellerId = sellerApplication.getId();

        // 허용된 필터 값만 통과시키고, 그 외(null 포함)는 전체(ALL)로 처리한다.
        String normalizedFilter = switch (filter == null ? "ALL" : filter.toUpperCase()) {
            case "ON_SALE", "SUSPENDED", "SOLD_OUT" -> filter.toUpperCase();
            default -> "ALL";
        };

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(
                normalizedPage,
                normalizedSize
        );

        Page<Product> productPage =
                productRepository.findSellerProductsByFilter(
                        sellerId,
                        normalizedFilter,
                        pageable
                );

        // 상태별 개수는 페이지네이션과 무관하게 판매자 전체 상품 기준으로 집계한다.
        long total = productRepository.countByDeletedFalseAndSeller_Id(sellerId);
        long suspended = productRepository.countSuspendedBySeller(sellerId);
        long soldOut = productRepository.countSoldOutBySeller(sellerId);
        long onSale = total - suspended - soldOut; // 분류가 완전·배타적이므로 나머지가 판매중

        SellerProductListResponse.Counts counts =
                new SellerProductListResponse.Counts(total, onSale, suspended, soldOut);

        return SellerProductListResponse.from(productPage, counts);
    }

    /**
     * 상품 수정 화면 초기값 조회 (옵션·이미지 갤러리 포함)
     */
    public SellerProductDetailResponse getProductDetail(
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

        return SellerProductDetailResponse.from(product);
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

        Brand brand = brandRepository
                .findById(request.brandId())
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.BRAND_NOT_FOUND
                        )
                );

        Product product = Product.builder()
                .seller(sellerApplication)
                .category(category)
                .brand(brand)
                .name(request.productName())
                .price(request.price())
                .discountRate(request.discountRate())
                // [1-1 조치] 서식 태그가 필요한 필드라 차단 대신 화이트리스트 정제를 적용한다
                .description(HtmlSanitizer.clean(request.description()))
                .thumbnailUrl(request.thumbnailUrl())
                .build();

        for (ProductOptionRequest optionRequest : request.options()) {
            product.addOption(toOption(product, optionRequest));
        }
        for (int i = 0; i < request.imageUrls().size(); i++) {
            product.addImage(toImage(product, request.imageUrls().get(i), i));
        }

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

        Brand brand = brandRepository
                .findById(request.brandId())
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.BRAND_NOT_FOUND
                        )
                );

        product.update(
                category,
                brand,
                request.productName(),
                request.price(),
                request.discountRate(),
                HtmlSanitizer.clean(request.description()),   // [1-1 조치]
                request.thumbnailUrl()
        );

        List<ProductOption> newOptions = request.options().stream()
                .map(optionRequest -> toOption(product, optionRequest))
                .toList();
        product.replaceOptions(newOptions);

        List<ProductImage> newImages = new ArrayList<>();
        for (int i = 0; i < request.imageUrls().size(); i++) {
            newImages.add(toImage(product, request.imageUrls().get(i), i));
        }
        product.replaceImages(newImages);

        return SellerProductResponse.from(product);
    }

    private ProductOption toOption(Product product, ProductOptionRequest request) {
        return ProductOption.builder()
                .product(product)
                .optionName(request.optionName())
                .additionalPrice(request.additionalPrice())
                .stockQuantity(request.stockQuantity())
                .build();
    }

    private ProductImage toImage(Product product, String imageUrl, int sortOrder) {
        return ProductImage.builder()
                .product(product)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .build();
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
     * PATCH /api/v1/seller/products/{productId}/status
     * 판매자가 본인 상품을 판매중지(SUSPENDED) / 판매재개(ON_SALE) 한다.
     * 삭제된 상품은 상태를 바꿀 수 없다.
     */
    @Transactional
    public SellerProductResponse updateProductStatus(
            Long userId,
            Long productId,
            ProductStatus status
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

        if (status == ProductStatus.SUSPENDED) {
            product.suspend();
        } else {
            product.resume();
        }

        return SellerProductResponse.from(product);
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