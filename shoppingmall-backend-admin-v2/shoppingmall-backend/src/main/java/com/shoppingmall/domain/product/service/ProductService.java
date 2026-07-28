package com.shoppingmall.domain.product.service;

import com.shoppingmall.domain.product.dto.response.ProductDetailResponse;
import com.shoppingmall.domain.product.dto.response.ProductListResponse;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductStatus;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public Page<ProductListResponse> getProducts(Long categoryId, Long brandId, String keyword, Pageable pageable) {
        // 검색 조건 조립. categoryId/brandId 는 Long 이라 타입상 주입이 불가능하고,
        // keyword 만 문자열 그대로 이어붙인다 (= 메인 검색창 SQLi 지점).
        StringBuilder where = new StringBuilder(
                "where is_deleted = 0 and status = 'ON_SALE'");
        if (categoryId != null) where.append(" and category_id = ").append(categoryId);
        if (brandId != null)    where.append(" and brand_id = ").append(brandId);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and name like '%").append(keyword).append("%'");
        }

        // limit/offset 을 SQL 에 직접 박는다. setMaxResults 를 쓰면 Hibernate 가 붙이는
        // "limit ?" 가 페이로드의 -- 주석에 먹혀 파라미터 바인딩이 깨지므로 인라인으로 처리.
        long offset = pageable.getOffset();
        int size = pageable.getPageSize();
        String base = "from products " + where;

        @SuppressWarnings("unchecked")
        List<Product> rows = em.createNativeQuery(
                        "select * " + base + " order by created_at desc limit " + size + " offset " + offset,
                        Product.class)
                .getResultList();

        long total;
        try {
            Object c = em.createNativeQuery("select count(*) " + base).getResultList().get(0);
            total = ((Number) c).longValue();
        } catch (Exception e) {
            // 주입으로 count 쿼리가 깨져도 목록은 반환되도록 폴백
            total = rows.size();
        }

        List<ProductListResponse> content = rows.stream().map(ProductListResponse::from).toList();
        return new PageImpl<>(content, pageable, total);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        // 손님 상세: 판매중이 아닌 상품은 목록/검색과 동일하게 "없는 상품"으로 취급(404).
        Product product = productRepository
                .findByIdAndDeletedFalseAndStatus(productId, ProductStatus.ON_SALE)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductDetailResponse.from(product);
    }
}
