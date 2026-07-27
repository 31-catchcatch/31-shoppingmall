package com.shoppingmall.domain.order.dto.response;

import lombok.Builder;
import lombok.Getter;

/** GET /api/v1/orders/checkout - 주문서 진입 시 기본값(수령인/주소/보유 포인트/배송비 정책) 미리 채워주는 응답 */
@Getter
public class CheckoutResponse {

    private final String defaultRecipientName;
    private final String defaultRecipientPhone;
    private final String defaultAddress;      // 기본 주소 (Address.baseAddress)
    private final String defaultAddressDetail; // 상세 주소 (Address.detailAddress)
    private final int availablePoint;

    /**
     * 배송비 정책. 프론트가 주문서에서 결제 예정 금액을 계산할 때 쓴다.
     *
     * 프론트에 빌드 스텝이 없어 상수를 갈아끼울 방법이 없으므로, 값이 어긋나지 않도록
     * 서버가 내려준다. (프론트에 같은 값이 하드코딩돼 있어 실제 청구액과 화면 금액이
     * 달랐던 문제를 없애기 위한 것)
     */
    private final int shippingFee;            // 무료배송 기준 미만일 때 부과되는 배송비
    private final int freeShippingThreshold;  // 상품 총액이 이 값 이상이면 배송비 0원

    @Builder
    public CheckoutResponse(String defaultRecipientName, String defaultRecipientPhone,
                             String defaultAddress, String defaultAddressDetail, int availablePoint,
                             int shippingFee, int freeShippingThreshold) {
        this.defaultRecipientName = defaultRecipientName;
        this.defaultRecipientPhone = defaultRecipientPhone;
        this.defaultAddress = defaultAddress;
        this.defaultAddressDetail = defaultAddressDetail;
        this.availablePoint = availablePoint;
        this.shippingFee = shippingFee;
        this.freeShippingThreshold = freeShippingThreshold;
    }
}
