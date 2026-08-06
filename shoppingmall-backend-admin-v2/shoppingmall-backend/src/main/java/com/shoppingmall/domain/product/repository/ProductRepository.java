package com.shoppingmall.domain.product.repository;

import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductStatus;
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
              and p.status = com.shoppingmall.domain.product.entity.ProductStatus.ON_SALE
              and (:categoryId is null or p.category.id = :categoryId)
              and (:brandId is null or p.brand.id = :brandId)
              and (:keyword is null or p.name like concat('%', :keyword, '%'))
            """)
    Page<Product> search(@Param("categoryId") Long categoryId,
                          @Param("brandId") Long brandId,
                          @Param("keyword") String keyword,
                          Pageable pageable);

    /** "존재하는(삭제되지 않은) 상품" 조회. 판매중지 여부는 따지지 않는다. */
    Optional<Product> findByIdAndDeletedFalse(Long id);

    /**
     * "손님에게 판매 가능한 상품" 조회 (삭제되지 않고 판매중).
     * 공개 상세/주문/신규 문의·좋아요 등 손님 행위의 상품 조회에 사용한다.
     */
    Optional<Product> findByIdAndDeletedFalseAndStatus(Long id, ProductStatus status);

    /** GET /api/v1/search/autocomplete - 상품명 앞부분 일치 기준 상위 10개 추천 */
    @Query("""
            select p.name from Product p
            where p.deleted = false
              and p.status = com.shoppingmall.domain.product.entity.ProductStatus.ON_SALE
              and p.name like concat(:keyword, '%')
            order by p.name asc
            """)
    List<String> findTop10NamesStartingWith(@Param("keyword") String keyword, Pageable pageable);

    /**
     * GET /api/v1/seller/products - 판매자 본인 상품을 상태 필터로 최신순 조회.
     * Product.seller가 SellerApplication 타입이므로 seller.id(=SellerApplication.id) 기준.
     *
     * filter 값:
     *   ALL       - 전체(삭제 제외)
     *   SUSPENDED - 판매정지 (status=SUSPENDED)
     *   ON_SALE   - 판매중 (status=ON_SALE 이면서 품절이 아님)
     *   SOLD_OUT  - 품절 (status=ON_SALE 이면서 활성옵션이 있고 재고합=0)
     *
     * 품절 판정은 활성 옵션(deleted=false)의 재고합 기준이며, 응답 DTO의 soldOut 정의와 동일하다.
     */
    @Query("""
            select p from Product p
            where p.deleted = false
              and p.seller.id = :sellerId
              and (
                    :filter = 'ALL'
                 or (:filter = 'SUSPENDED'
                     and p.status = com.shoppingmall.domain.product.entity.ProductStatus.SUSPENDED)
                 or (:filter = 'ON_SALE'
                     and p.status = com.shoppingmall.domain.product.entity.ProductStatus.ON_SALE
                     and (
                          (select count(o) from ProductOption o where o.product = p and o.deleted = false) = 0
                       or (select coalesce(sum(o.stockQuantity), 0) from ProductOption o where o.product = p and o.deleted = false) > 0
                     ))
                 or (:filter = 'SOLD_OUT'
                     and p.status = com.shoppingmall.domain.product.entity.ProductStatus.ON_SALE
                     and (select count(o) from ProductOption o where o.product = p and o.deleted = false) > 0
                     and (select coalesce(sum(o.stockQuantity), 0) from ProductOption o where o.product = p and o.deleted = false) = 0)
              )
            order by p.createdAt desc
            """)
    Page<Product> findSellerProductsByFilter(@Param("sellerId") Long sellerApplicationId,
                                             @Param("filter") String filter,
                                             Pageable pageable);

    /** 판매자 본인 상품 전체 개수(삭제 제외). counts.total 및 onSale 산출용. */
    long countByDeletedFalseAndSeller_Id(Long sellerApplicationId);

    /** 판매정지(SUSPENDED) 개수. */
    @Query("""
            select count(p) from Product p
            where p.deleted = false
              and p.seller.id = :sellerId
              and p.status = com.shoppingmall.domain.product.entity.ProductStatus.SUSPENDED
            """)
    long countSuspendedBySeller(@Param("sellerId") Long sellerApplicationId);

    /** 품절(판매중이며 활성옵션이 있고 재고합=0) 개수. */
    @Query("""
            select count(p) from Product p
            where p.deleted = false
              and p.seller.id = :sellerId
              and p.status = com.shoppingmall.domain.product.entity.ProductStatus.ON_SALE
              and (select count(o) from ProductOption o where o.product = p and o.deleted = false) > 0
              and (select coalesce(sum(o.stockQuantity), 0) from ProductOption o where o.product = p and o.deleted = false) = 0
            """)
    long countSoldOutBySeller(@Param("sellerId") Long sellerApplicationId);
}
