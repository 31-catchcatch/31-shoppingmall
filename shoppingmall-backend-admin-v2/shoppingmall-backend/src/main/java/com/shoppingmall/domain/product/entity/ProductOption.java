package com.shoppingmall.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** DB 정의서 'product_options' 테이블. 화면정의서 34/35번(사이즈/수량 선택) 대응. */
@Getter
@Entity
@Table(name = "product_options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Column(name = "additional_price", nullable = false)
    private Integer additionalPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Builder
    public ProductOption(Product product, String optionName, Integer additionalPrice, Integer stockQuantity) {
        this.product = product;
        this.optionName = optionName;
        this.additionalPrice = additionalPrice == null ? 0 : additionalPrice;
        this.stockQuantity = stockQuantity == null ? 0 : stockQuantity;
        this.deleted = false;
    }

    public boolean isSoldOut() {
        return stockQuantity == 0;
    }
}
