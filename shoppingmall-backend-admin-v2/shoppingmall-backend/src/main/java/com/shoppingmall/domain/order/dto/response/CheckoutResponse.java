package com.shoppingmall.domain.order.dto.response;

import lombok.Builder;
import lombok.Getter;

/** GET /api/v1/orders/checkout - 주문서 진입 시 기본값(수령인/주소/보유 포인트) 미리 채워주는 응답 */
@Getter
public class CheckoutResponse {

    private final String defaultRecipientName;
    private final String defaultRecipientPhone;
    private final String defaultAddress;      // 기본 주소 (Address.baseAddress)
    private final String defaultAddressDetail; // 상세 주소 (Address.detailAddress)
    private final int availablePoint;

    @Builder
    public CheckoutResponse(String defaultRecipientName, String defaultRecipientPhone,
                             String defaultAddress, String defaultAddressDetail, int availablePoint) {
        this.defaultRecipientName = defaultRecipientName;
        this.defaultRecipientPhone = defaultRecipientPhone;
        this.defaultAddress = defaultAddress;
        this.defaultAddressDetail = defaultAddressDetail;
        this.availablePoint = availablePoint;
    }
}
