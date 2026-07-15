package com.shoppingmall.domain.payment.entity;

import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자가 등록해 둔 간편결제 수단(빌링키). 실제 PG 결제 내역인 Payment 와는 별개 테이블(payment_methods).
 * GET/POST/DELETE /api/v1/users/me/payments 대응.
 */
@Getter
@Entity
@Table(name = "payment_methods")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentMethod extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "pg_provider", nullable = false, length = 50)
    private String pgProvider; // 예: TOSS, KAKAO_PAY

    @Column(name = "billing_key", nullable = false, length = 255)
    private String billingKey; // PG사가 발급한 빌링키(카드 정보 자체는 저장하지 않음)

    @Column(length = 50)
    private String alias; // 사용자가 지정한 별칭. 예: "내 신한카드"

    @Column(name = "masked_card_number", length = 30)
    private String maskedCardNumber; // 화면 노출용 마스킹된 카드번호. 예: 1234-****-****-5678

    @Column(name = "is_default", nullable = false)
    private boolean defaultMethod;

    @Builder
    public PaymentMethod(User user, String pgProvider, String billingKey,
                          String alias, String maskedCardNumber, boolean defaultMethod) {
        this.user = user;
        this.pgProvider = pgProvider;
        this.billingKey = billingKey;
        this.alias = alias;
        this.maskedCardNumber = maskedCardNumber;
        this.defaultMethod = defaultMethod;
    }

    public void updateDefaultStatus(boolean isDefault) {
        this.defaultMethod = isDefault;
    }
}
