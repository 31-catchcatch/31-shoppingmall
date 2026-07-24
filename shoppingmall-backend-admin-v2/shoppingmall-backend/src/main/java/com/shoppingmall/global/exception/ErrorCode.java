package com.shoppingmall.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 도메인이 늘어날 때마다 이 enum에 항목만 추가하면 된다.
 * (인증/상품 예시 + 앞으로 붙을 도메인들이 공통으로 참조)
 */
@Getter
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON-001", "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-002", "서버 내부 오류가 발생했습니다."),

    // Auth / User
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "AUTH-001", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH-002", "이미 사용 중인 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH-003", "아이디 또는 비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-004", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-005", "만료된 토큰입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH-006", "존재하지 않는 사용자입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH-007", "접근 권한이 없습니다."),
    WRONG_LOGIN_ENDPOINT(HttpStatus.BAD_REQUEST, "AUTH-008", "잘못된 로그인 경로입니다. 알맞은 로그인 API를 사용해주세요."),
    DUPLICATE_BUSINESS_NUMBER(HttpStatus.CONFLICT, "AUTH-009", "이미 등록된 사업자등록번호입니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH-010", "인증번호가 일치하지 않습니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH-011", "인증번호가 만료되었습니다. 다시 요청해주세요."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "AUTH-012", "현재 비밀번호가 일치하지 않습니다."),

    // User Address (배송지 주소록)
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "ADDRESS-001", "존재하지 않거나 삭제된 배송지 주소입니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT-001", "존재하지 않는 상품입니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT-002", "존재하지 않는 카테고리입니다."),
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT-003", "존재하지 않는 브랜드입니다."),
    PRODUCT_NOT_ON_SALE(HttpStatus.CONFLICT, "PRODUCT-004", "현재 판매중지된 상품입니다."),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "PRODUCT-005", "재고가 부족합니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW-001", "이미 리뷰를 작성한 주문입니다."),

    // ===== Seller (sell 추가) =====
    SELLER_NOT_FOUND(HttpStatus.NOT_FOUND, "SELLER-001", "판매자를 찾을 수 없습니다."),
    SELLER_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "SELLER-002", "입점 신청서를 찾을 수 없습니다."),
    SELLER_APPLICATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "SELLER-003", "이미 진행 중이거나 승인된 입점 신청이 있습니다."),
    BUSINESS_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "SELLER-004", "이미 사용 중인 사업자등록번호입니다."),
    SELLER_NOT_APPROVED(HttpStatus.FORBIDDEN, "SELLER-005", "승인된 판매자가 아닙니다."),
    SELLER_SUSPENDED(HttpStatus.FORBIDDEN, "SELLER-006", "판매자 계정이 정지되었습니다."),
    PRODUCT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "SELLER-007", "본인 상품만 수정할 수 있습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "SELLER-008", "주문 정보를 찾을 수 없습니다."),
    CLAIM_NOT_FOUND(HttpStatus.NOT_FOUND, "SELLER-009", "클레임 정보를 찾을 수 없습니다."),
    QNA_NOT_FOUND(HttpStatus.NOT_FOUND, "SELLER-010", "Q&A를 찾을 수 없습니다."),
    COUPON_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "SELLER-011", "쿠폰 요청 정보를 찾을 수 없습니다."),
    INVALID_SELLER_STATUS(HttpStatus.BAD_REQUEST, "SELLER-012", "판매자 상태가 올바르지 않습니다."),
    QNA_ACCESS_DENIED(HttpStatus.FORBIDDEN, "SELLER-013", "해당 문의에 대한 판매자 권한이 없습니다."),
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "INQUIRY-001", "존재하지 않는 문의입니다."),
    // sell 원본에서 INVALID_SEARCH_PERIOD 가 SELLER-013과 중복 사용되고 있어서 SELLER-014로 재배치
    INVALID_SEARCH_PERIOD(HttpStatus.BAD_REQUEST, "SELLER-014", "조회 시작일은 종료일보다 이후일 수 없습니다."),

    // ===== Order (sell 추가) =====
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "ORDER-001", "현재 주문 상태에서는 배송 처리를 할 수 없습니다."),

    // ===== Claim (sell 추가) =====
    INVALID_CLAIM_STATUS(HttpStatus.BAD_REQUEST, "CLAIM-001", "현재 상태에서는 클레임을 신청하거나 변경할 수 없습니다."),
    // sell 원본에서 CLAIM_ALREADY_EXISTS 가 CLAIM-001과 중복 사용되고 있어서 CLAIM-002로 재배치
    CLAIM_ALREADY_EXISTS(HttpStatus.CONFLICT, "CLAIM-002", "이미 해당 주문에 대한 클레임이 존재합니다."),

    // ===== Coupon (sell 추가) =====
    COUPON_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "COUPON-001", "동일한 이름으로 처리 중인 쿠폰 요청이 있습니다."),
    INVALID_COUPON_PERIOD(HttpStatus.BAD_REQUEST, "COUPON-002", "쿠폰 종료일은 시작일보다 이후여야 합니다."),
    INVALID_COUPON_DISCOUNT(HttpStatus.BAD_REQUEST, "COUPON-003", "쿠폰 할인 유형 또는 할인 값이 올바르지 않습니다."),

    // ===== Refund (sell 추가) =====
    REFUND_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "REFUND-001", "현재 클레임 유형 또는 상태에서는 환불할 수 없습니다."),
    REFUND_PROCESS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "REFUND-002", "환불 처리 중 오류가 발생했습니다."),

    // ===== Notification =====
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION-001", "존재하지 않는 알림입니다."),

    // ===== Review =====
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW-001", "존재하지 않거나 접근 권한이 없는 리뷰입니다."),

    // ===== Coupon (사용자) =====
    COUPON_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "COUPON-101", "사용할 수 없는 쿠폰입니다."),
    COUPON_MINIMUM_NOT_MET(HttpStatus.BAD_REQUEST, "COUPON-102", "쿠폰 적용 최소 주문 금액을 충족하지 않습니다."),
    COUPON_ALREADY_CLAIMED(HttpStatus.CONFLICT, "COUPON-103", "이미 발급받은 쿠폰입니다."),
    COUPON_SOLD_OUT(HttpStatus.CONFLICT, "COUPON-104", "쿠폰 발급 수량이 모두 소진되었습니다."),

    // ===== Payment (결제수단) =====
    PAYMENT_METHOD_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT-001", "존재하지 않거나 접근 권한이 없는 결제수단입니다."),

    // ===== Settlement =====
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "SETTLEMENT-001", "존재하지 않는 정산 내역입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
