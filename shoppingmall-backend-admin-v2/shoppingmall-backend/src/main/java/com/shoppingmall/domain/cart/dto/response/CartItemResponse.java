package com.shoppingmall.domain.cart.dto.response;

import com.shoppingmall.domain.cart.entity.CartItem;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CartItemResponse {

    private final Long cartItemId;
    private final Long productId;
    private final String productName;
    private final Long optionId;
    private final String optionName;
    private final int price;       // 옵션이 적용된 상품 최종 개당 가격
    private final int quantity;    // 유저가 지정한 수량
    private final int totalPrice;  // price * quantity 자동 합산 결과
    private final String thumbnailUrl; // 목록 썸네일 (없으면 null → 프론트가 회색 박스로 대체)

    @Builder
    public CartItemResponse(Long cartItemId, Long productId, String productName,
                            Long optionId, String optionName, int price, int quantity,
                            String thumbnailUrl) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.productName = productName;
        this.optionId = optionId;
        this.optionName = optionName;
        this.price = price;
        this.quantity = quantity;
        this.totalPrice = price * quantity;
        this.thumbnailUrl = thumbnailUrl;
    }

    public static CartItemResponse from(CartItem cartItem) {
        // 기존 엔티티 구조에서 상품 가격과 옵션 추가 금액을 더하여 최종 가액 파싱
        int basePrice = cartItem.getProduct().getPrice();

        // 💡 엔티티의 Integer 타입을 안전하게 int로 꺼내옵니다. (기본값 0 처리)
        Integer additionalPriceObj = cartItem.getProductOption().getAdditionalPrice();
        int optionAdditionalPrice = (additionalPriceObj == null) ? 0 : additionalPriceObj;

        return CartItemResponse.builder()
                .cartItemId(cartItem.getId())
                .productId(cartItem.getProduct().getId())
                .productName(cartItem.getProduct().getName())
                .optionId(cartItem.getProductOption().getId())
                .optionName(cartItem.getProductOption().getOptionName())
                .price(basePrice + optionAdditionalPrice)
                .quantity(cartItem.getQuantity())
                // 위에서 이미 getProduct() 를 쓰고 있어 프록시가 초기화된 상태라 추가 쿼리가 없다.
                .thumbnailUrl(cartItem.getProduct().getThumbnailUrl())
                .build();
    }
}