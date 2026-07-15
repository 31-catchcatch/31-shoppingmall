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

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc")
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOption> options = new ArrayList<>();

    @Builder
    public Product(SellerApplication seller, Category category, String name, Integer price,
                   Integer discountRate, String description, String thumbnailUrl) {
        this.seller = seller;
        this.category = category;
        this.name = name;
        this.price = price;
        this.discountRate = discountRate == null ? 0 : discountRate;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.deleted = false;
    }

    public void addImage(ProductImage image) {
        images.add(image);
    }

    public void addOption(ProductOption option) {
        options.add(option);
    }
    /**
     * 판매자가 상품 기본 정보를 수정한다.
     *
     * JPA 변경 감지를 사용하므로 Service에서 별도의 save()를
     * 호출하지 않아도 트랜잭션 종료 시 UPDATE 쿼리가 실행된다.
     */
    public void update(
            Category category,
            String name,
            Integer price,
            Integer discountRate,
            String description,
            String thumbnailUrl
    ) {
        this.category = category;
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

    /** price에 discountRate를 적용한 실 판매가. 프론트 목록/상세 카드에서 그대로 쓸 수 있게 서버에서 계산해서 내려준다. */
    public int getFinalPrice() {
        return price - (price * discountRate / 100);
    }
}
