package com.shoppingmall.domain.user.entity;

import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_addresses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 다대일(N:1) 관계로 회원 정보와 연동
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String addressName; // 예: "우리집", "회사"

    @Column(nullable = false, length = 100)
    private String recipientName; // 수령인 이름

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(nullable = false, length = 255)
    private String baseAddress; // 기본 주소

    @Column(nullable = false, length = 255)
    private String detailAddress; // 상세 주소

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress; // 기본 배송지 여부

    @Builder
    public Address(User user, String addressName, String recipientName,
                   String recipientPhone, String baseAddress, String detailAddress, boolean defaultAddress) {
        this.user = user;
        this.addressName = addressName;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
        this.defaultAddress = defaultAddress;
    }

    // 기본 배송지 변경 시 상태를 바꾸는 비즈니스 로직 메서드
    public void updateDefaultStatus(boolean isDefault) {
        this.defaultAddress = isDefault;
    }

    /** PUT /users/me/addresses/{addressId} - 배송지 정보 전체 수정 */
    public void update(String addressName, String recipientName, String recipientPhone,
                       String baseAddress, String detailAddress, boolean defaultAddress) {
        this.addressName = addressName;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.baseAddress = baseAddress;
        this.detailAddress = detailAddress;
        this.defaultAddress = defaultAddress;
    }
}