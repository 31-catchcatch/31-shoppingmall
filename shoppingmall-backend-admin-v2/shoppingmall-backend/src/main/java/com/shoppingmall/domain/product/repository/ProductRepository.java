package com.shoppingmall.domain.product.repository;

import com.shoppingmall.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * GET /api/v1/products - 메인/검색/카테고리 필터링 통합 조회.
     * keyword, categoryId 둘 다 선택적 파라미터라 null 이면 조건을 무시하도록 작성.
     */
    @Query("""
            select p from Product p
            where p.deleted = false
              and (:categoryId is null or p.category.id = :categoryId)
              and (:brandId is null or p.brand.id = :brandId)
              and (:keyword is null or p.name like concat('%', :keyword, '%'))
            """)
    Page<Product> search(@Param("categoryId") Long categoryId,
                          @Param("brandId") Long brandId,
                          @Param("keyword") String keyword,
                          Pageable pageable);

    Optional<Product> findByIdAndDeletedFalse(Long id);

    /** GET /api/v1/search/autocomplete - 상품명 앞부분 일치 기준 상위 10개 추천 */
    @Query("""
            select p.name from Product p
            where p.deleted = false and p.name like concat(:keyword, '%')
            order by p.name asc
            """)
    List<String> findTop10NamesStartingWith(@Param("keyword") String keyword, Pageable pageable);

    /**
     * GET /api/v1/seller/products - 특정 판매자가 등록한 삭제되지 않은 상품을 최신순으로 조회.
     * Product.seller가 SellerApplication 타입이므로 seller.id(=SellerApplication.id) 기준으로 조회한다.
     */
    Page<Product> findAllBySeller_IdAndDeletedFalseOrderByCreatedAtDesc(Long sellerApplicationId, Pageable pageable);
}
