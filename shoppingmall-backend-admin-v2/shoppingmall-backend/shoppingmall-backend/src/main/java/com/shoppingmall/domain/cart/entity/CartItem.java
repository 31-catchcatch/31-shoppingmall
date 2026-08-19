package com.shoppingmall.domain.cart.entity;

import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductOption;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "cart_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", nullable = false)
    private ProductOption productOption;

    @Column(nullable = false)
    private int quantity;

    @Builder
    public CartItem(User user, Product product, ProductOption productOption, int quantity) {
        this.user = user;
        this.product = product;
        this.productOption = productOption;
        this.quantity = quantity;
    }

    // 장바구니 내 동일 상품/옵션이 존재할 때 수량을 늘려주는 더티 체킹 비즈니스 메서드
    public void addQuantity(int count) {
        this.quantity += count;
    }

    // 장바구니 내부에서 사용자가 직접 수량을 조정할 때의 업데이트 메서드
    public void updateQuantity(int quantity) {
        this.quantity = quantity;
    }
}