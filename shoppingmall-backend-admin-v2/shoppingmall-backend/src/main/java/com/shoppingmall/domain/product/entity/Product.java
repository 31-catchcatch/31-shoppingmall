package com.shoppingmall.domain.product.entity;

import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** DB 정의서 'products' 테이블 매핑. (image_url -> thumbnail_url 로 명칭 변경된 v2 반영) */
@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private SellerApplication seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** DB 정의서 v2 추가 컬럼 - 브랜드 미지정 상품도 있을 수 있어 Null 허용 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "discount_rate", nullable = false)
    private Integer discountRate;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    /**
     * 판매 상태. 삭제(is_deleted)와 독립. 손님 노출/주문 가능 여부는 ON_SALE 만 허용.
     * columnDefinition 을 varchar 로 명시해 로컬(ddl-auto)과 운영(마이그레이션 V12, VARCHAR)의
     * 컬럼 타입을 일치시킨다. (미명시 시 Hibernate 가 MySQL ENUM 으로 생성해 운영과 어긋남)
     *
     * DEFAULT 'ON_SALE' 도 함께 명시한다. 없으면 아래 두 경우가 깨진다.
     *   1) 기존 상품이 있는 DB 에 ddl-auto:update 로 이 컬럼이 추가될 때
     *      → MySQL 이 빈 문자열('')로 채워서 기존 상품이 목록에서 전부 사라진다.
     *   2) seed_catalog.sql / seed_test_data.sql 의 products INSERT 가 status 를 명시하지 않음
     *      → strict 모드에서 "Field 'status' doesn't have a default value" 로 실패한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'ON_SALE'")
    private ProductStatus status;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc")
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOption> options = new ArrayList<>();

    @Builder
    public Product(SellerApplication seller, Category category, Brand brand, String name, Integer price,
                   Integer discountRate, String description, String thumbnailUrl) {
        this.seller = seller;
        this.category = category;
        this.brand = brand;
        this.name = name;
        this.price = price;
        this.discountRate = discountRate == null ? 0 : discountRate;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.deleted = false;
        this.status = ProductStatus.ON_SALE;
    }

    public void addImage(ProductImage image) {
        images.add(image);
    }

    /** 상품 수정 시 이미지 목록을 통째로 교체한다 (orphanRemoval로 기존 이미지는 삭제됨). */
    public void replaceImages(List<ProductImage> newImages) {
        images.clear();
        images.addAll(newImages);
    }

    public void addOption(ProductOption option) {
        options.add(option);
    }

    /** 상품 수정 시 옵션 목록을 통째로 교체한다 (orphanRemoval로 기존 옵션은 삭제됨). */
    public void replaceOptions(List<ProductOption> newOptions) {
        options.clear();
        options.addAll(newOptions);
    }
    /**
     * 판매자가 상품 기본 정보를 수정한다.
     *
     * JPA 변경 감지를 사용하므로 Service에서 별도의 save()를
     * 호출하지 않아도 트랜잭션 종료 시 UPDATE 쿼리가 실행된다.
     */
    public void update(
            Category category,
            Brand brand,
            String name,
            Integer price,
            Integer discountRate,
            String description,
            String thumbnailUrl
    ) {
        this.category = category;
        this.brand = brand;
        this.name = name;
        this.price = price;
        this.discountRate =
                discountRate == null ? 0 : discountRate;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
    }
    public void softDelete() {
        this.deleted = true;
    }

    /** 판매자가 상품을 일시 판매중지한다. */
    public void suspend() {
        this.status = ProductStatus.SUSPENDED;
    }

    /** 판매중지한 상품을 다시 판매중으로 되돌린다. */
    public void resume() {
        this.status = ProductStatus.ON_SALE;
    }

    /** 손님에게 노출·주문 가능한 상태인지. (삭제되지 않았고 판매중) */
    public boolean isOnSale() {
        return !deleted && status == ProductStatus.ON_SALE;
    }

    /** price에 discountRate를 적용한 실 판매가. 프론트 목록/상세 카드에서 그대로 쓸 수 있게 서버에서 계산해서 내려준다. */
    public int getFinalPrice() {
        return price - (price * discountRate / 100);
    }
}
