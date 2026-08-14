package com.shoppingmall.domain.user.dto.response;

import com.shoppingmall.domain.user.entity.Address;

/**
 * [5-2 조치] 배송지 조회 응답.
 *
 * <p>기존에는 컨트롤러가 {@code List<Address>} 엔티티를 그대로 반환해
 * Jackson 이 {@code Address.user} 를 따라가며 {@code User.password}(BCrypt 해시)까지
 * 직렬화했다. 화면에 필요한 필드만 담고 User 연관관계는 담지 않는다.
 *
 * <p><b>전화번호 마스킹에 대한 판단</b>: 본인 소유 배송지를 정보 주체 본인에게
 * 보여주는 응답이고, 배송지 수정 화면이 이 값을 그대로 채워 넣어야 하므로 마스킹하지 않는다.
 * 보고서가 지적한 것은 계정 비밀번호 해시이지 본인 연락처가 아니다.
 * 정책이 바뀌면 아래 maskPhone 을 사용하도록 from() 을 수정하면 된다.
 */
public record AddressResponse(
        Long id,
        String addressName,
        String recipientName,
        String recipientPhone,
        String baseAddress,
        String detailAddress,
        String zipCode,
        boolean defaultAddress
) {

    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getAddressName(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getBaseAddress(),
                address.getDetailAddress(),
                address.getZipCode(),
                address.isDefaultAddress()
        );
    }

    /** 정책상 목록에서 연락처를 가려야 할 경우 사용한다. 010-1234-5678 -> 010-****-5678 */
    public static String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 7) {
            return phone;
        }
        return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
    }
}
