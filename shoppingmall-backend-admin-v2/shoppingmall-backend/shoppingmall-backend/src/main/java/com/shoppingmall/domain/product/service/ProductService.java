package com.shoppingmall.domain.product.service;

import com.shoppingmall.domain.product.dto.response.ProductDetailResponse;
import com.shoppingmall.domain.product.dto.response.ProductListResponse;
import com.shoppingmall.domain.product.entity.ProductStatus;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * GET /api/v1/products - 메인/검색/카테고리 필터링 통합 조회.
     *
     * <p><b>[1-2 조치]</b> 기존에는 EntityManager 로 where 절과 limit/offset 을 문자열로 조립해
     * keyword 파라미터에 boolean-based SQL Injection 이 성립했다. 또한 count 쿼리의 예외를
     * try/catch 로 삼켜 주입 실패가 드러나지 않았고, 그것이 참/거짓 판별 채널이 되었다.
     * 이미 정의되어 있으나 사용되지 않던 파라미터 바인딩 JPQL(ProductRepository.search)로 교체한다.
     */
    @Transactional(readOnly = true)
    public Page<ProductListResponse> getProducts(Long categoryId, Long brandId,
                                                 String keyword, Pageable pageable) {

        String normalizedKeyword = (keyword == null || keyword.isBlank())
                ? null
                : escapeLike(keyword.trim());

        return productRepository
                .search(categoryId, brandId, normalizedKeyword, pageable)
                .map(ProductListResponse::from);
    }

    /**
     * [1-2 조치] LIKE 절의 와일드카드(%, _)를 리터럴로 처리한다.
     * SQL Injection 과는 별개 문제지만, '%%%' 같은 입력으로 전체 테이블을 훑는
     * 부하 유발(LIKE injection)을 막기 위해 함께 적용한다.
     * 이스케이프 문자는 JPQL 의 escape '!' 와 짝을 이룬다.
     */
    public static String escapeLike(String value) {
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        // 손님 상세: 판매중이 아닌 상품은 목록/검색과 동일하게 "없는 상품"으로 취급(404).
        return ProductDetailResponse.from(
                productRepository
                        .findByIdAndDeletedFalseAndStatus(productId, ProductStatus.ON_SALE)
                        .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND)));
    }
}
