-- ============================================================================
-- V13__review_order_detail.sql
--
-- 리뷰 중복 방지: 리뷰가 어떤 구매 건(주문상품)에 대한 것인지 추적하기 위한 컬럼.
-- "구매 건(order_detail)당 리뷰 1개" 규칙의 기준이 된다.
-- ddl-auto: update 환경(로컬)에서는 자동 반영되므로 실행 불필요.
-- validate 환경(운영/스테이징)에만 수동 적용.
-- ============================================================================

-- 기존 리뷰는 이 컬럼 도입 전 데이터라 NULL 을 허용한다. 신규 리뷰는 항상 채워진다.
ALTER TABLE reviews
    ADD COLUMN order_detail_id BIGINT NULL,
    ADD CONSTRAINT fk_reviews_order_detail
        FOREIGN KEY (order_detail_id) REFERENCES order_details(id);
