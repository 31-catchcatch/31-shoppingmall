-- ============================================================================
-- V11__refund_logic.sql
--
-- 환불(클레임) 로직 보강.
-- ddl-auto: update 환경(로컬)에서는 자동 반영되므로 실행 불필요.
-- validate 환경(운영/스테이징)에만 수동 적용.
-- ============================================================================

-- 주문에 실제 적용된 쿠폰(UserCoupon)을 추적하기 위한 컬럼.
-- 기존에는 coupon_discount_amount(할인 금액)만 저장하고 있어, 주문 전체 환불 시
-- 어떤 쿠폰을 다시 사용 가능하게 되돌려야 하는지 알 수 없었다.
ALTER TABLE orders
    ADD COLUMN used_coupon_id BIGINT NULL,
    ADD CONSTRAINT fk_orders_used_coupon FOREIGN KEY (used_coupon_id) REFERENCES user_coupons(id);
