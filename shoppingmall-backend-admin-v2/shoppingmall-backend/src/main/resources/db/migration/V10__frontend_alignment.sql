-- ============================================================================
-- V10__frontend_alignment.sql
--
-- 프론트엔드(B 항목 + E 경로 협의) 대응 백엔드 추가분에 대한 스키마.
-- ddl-auto: update 환경(로컬)에서는 자동 반영되므로 실행 불필요.
-- validate 환경(운영/스테이징)에만 수동 적용.
-- ============================================================================

-- 1) 브랜드 즐겨찾기 (POST /api/v1/brands/{brandId}/like)
CREATE TABLE IF NOT EXISTS brand_likes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    brand_id    BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_brand_like_user_brand UNIQUE (user_id, brand_id),
    CONSTRAINT fk_brand_like_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_brand_like_brand FOREIGN KEY (brand_id) REFERENCES brands(id)
);

-- 2) 리뷰 논리 삭제 (DELETE /api/v1/reviews/{reviewId})
ALTER TABLE reviews
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- GET /brands, PUT /reviews/{id}, POST /products/{id}/reviews,
-- POST /auth/find-username, POST /auth/reset-password 는
-- 기존 테이블(brands, reviews, users)을 그대로 사용하므로 추가 DDL 없음.

-- 3) 고객센터 1:1 문의 (POST/GET /api/v1/customer-center/inquiries)
CREATE TABLE IF NOT EXISTS customer_inquiries (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    category      VARCHAR(30) NOT NULL,
    order_number  VARCHAR(50) NULL,
    title         VARCHAR(200) NOT NULL,
    content       TEXT NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    answer        TEXT NULL,
    answered_at   DATETIME NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NULL,
    CONSTRAINT fk_inquiry_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 배송지 수정 완성, 주문 쿠폰 할인 반영, 쿠폰 발급(claim), 판매자 로그아웃,
-- GET/PUT /seller/me 는 기존 테이블을 그대로 사용하므로 추가 DDL 없음.
