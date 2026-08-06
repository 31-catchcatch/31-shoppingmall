-- ============================================================================
-- V9__general_user_api_backfill.sql
--
-- API 명세서 v9 "일반 사용자" 도메인 작성 필요 항목 구현에 따라 추가된 스키마.
-- ddl-auto: validate 설정이므로 이 스크립트를 운영/개발 DB에 수동으로 먼저 적용해야
-- 애플리케이션이 정상 기동한다.
-- ============================================================================

-- 1) 이벤트 배너 (GET /api/v1/banners)
CREATE TABLE IF NOT EXISTS banners (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(100) NOT NULL,
    image_url   VARCHAR(512) NOT NULL,
    link_url    VARCHAR(512),
    sort_order  INT NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    start_at    DATETIME NULL,
    end_at      DATETIME NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2) 브랜드 (GET /api/v1/products?brandId=)
CREATE TABLE IF NOT EXISTS brands (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    logo_url    VARCHAR(512),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NULL,
    CONSTRAINT uk_brand_name UNIQUE (name)
);

ALTER TABLE products
    ADD COLUMN brand_id BIGINT NULL AFTER category_id,
    ADD CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) REFERENCES brands(id);

-- 3) 상품 좋아요 / 위시리스트 (POST .../like, GET /api/v1/users/me/wishlist)
CREATE TABLE IF NOT EXISTS product_likes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_like_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_product_like_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_product_like_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- 4) 결제수단 (GET/POST/DELETE /api/v1/users/me/payments) - 기존 payments(PG 결제내역)와 별개 테이블
CREATE TABLE IF NOT EXISTS payment_methods (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    pg_provider         VARCHAR(50) NOT NULL,
    billing_key         VARCHAR(255) NOT NULL,
    alias               VARCHAR(50),
    masked_card_number  VARCHAR(30),
    is_default          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NULL,
    CONSTRAINT fk_payment_method_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 5) 보유 쿠폰 (GET /api/v1/users/me/coupons)
-- ⚠ 쿠폰을 사용자 지갑으로 "발급(claim)"하는 API/트리거가 아직 없어 이 테이블에는
--   당장 데이터가 쌓이지 않는다. 발급 정책이 확정되면 별도 서비스 로직 추가가 필요하다.
CREATE TABLE IF NOT EXISTS user_coupons (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    coupon_id   BIGINT NOT NULL,
    is_used     BOOLEAN NOT NULL DEFAULT FALSE,
    used_at     DATETIME NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_coupon_user_coupon UNIQUE (user_id, coupon_id),
    CONSTRAINT fk_user_coupon_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id)
);

-- 알림 목록조회/읽음처리, 배송/영수증/거래명세서 조회, 내 리뷰 목록 조회는
-- 기존 notifications / order_details / payments / reviews 테이블 컬럼을 그대로 사용하므로
-- 추가 DDL이 필요 없다.
