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
/** 옵션 내용을 갱신한다. 행을 지우지 않으므로 order_details 가 참조하는 id 가 유지된다. */
    public void update(Integer additionalPrice, Integer stockQuantity) {
        this.additionalPrice = additionalPrice == null ? 0 : additionalPrice;
        this.stockQuantity = stockQuantity == null ? 0 : stockQuantity;
    }

    /**
     * 판매자가 옵션을 제거했을 때 호출한다.
     * 물리 삭제하면 order_details.product_option_id 외래키 위반(SQL 1451)이 발생하고,
     * 주문 이력에서 "어떤 옵션을 샀는지"도 사라진다. 행은 남기고 비활성 표시만 한다.
     */
    public void softDelete() {
        this.deleted = true;
        this.stockQuantity = 0;
    }

    /** 제거했던 옵션명을 다시 등록했을 때 기존 행을 되살린다. */
    public void restore() {
        this.deleted = false;
    }
}
