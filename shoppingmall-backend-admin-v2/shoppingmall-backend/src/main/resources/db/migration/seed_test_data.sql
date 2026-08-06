-- ============================================================================
-- seed_test_data.sql
--
-- Postman 컬렉션 테스트를 위한 최소 시드 데이터.
--
-- ⚠ 실행 순서 중요:
--   1) 먼저 Postman "0. 인증 → 회원가입"을 실행해서 testuser1 계정을 만드세요.
--   2) 그 다음 이 스크립트를 실행하세요. (아래 대부분의 INSERT가 testuser1의
--      user_id를 서브쿼리로 찾아서 참조하기 때문에 순서가 바뀌면 실패합니다)
--
-- 몇 번이고 다시 실행해도 되도록 기존 시드 데이터를 먼저 지우고 다시 넣는 방식입니다.
-- ============================================================================

USE shoppingmall;  -- ⚠ DB 이름이 다르면 이 줄만 본인 환경에 맞게 바꿔주세요.

-- MySQL Workbench의 안전모드(Safe Update Mode)는 서브쿼리를 쓴 DELETE/UPDATE를 막기 때문에
-- 이 세션에서만 잠깐 꺼둔다 (Preferences 설정을 바꾸거나 재접속할 필요 없음)
SET SQL_SAFE_UPDATES = 0;


-- ----------------------------------------------------------------------------
-- 0. 기존 시드 데이터 정리 (재실행 대비) - 하위 테이블부터 삭제
-- ----------------------------------------------------------------------------
DELETE FROM user_coupons WHERE coupon_id IN (SELECT id FROM coupons WHERE coupon_name = '[시드] 웰컴 10% 할인 쿠폰');
DELETE FROM coupons WHERE coupon_name = '[시드] 웰컴 10% 할인 쿠폰';
DELETE FROM coupon_requests WHERE coupon_name = '[시드] 웰컴 10% 할인 쿠폰';
DELETE FROM product_options WHERE product_id IN (SELECT id FROM products WHERE name IN ('[시드] 베이직 반팔 티셔츠', '[시드] 노브랜드 에코백'));
DELETE FROM product_images WHERE product_id IN (SELECT id FROM products WHERE name IN ('[시드] 베이직 반팔 티셔츠', '[시드] 노브랜드 에코백'));
DELETE FROM products WHERE name IN ('[시드] 베이직 반팔 티셔츠', '[시드] 노브랜드 에코백');
DELETE FROM banners WHERE title LIKE '[시드]%';
DELETE FROM notifications WHERE title = '[시드] 알림 테스트';
DELETE FROM brands WHERE name = '[시드] 캐치캐치';
DELETE FROM seller_applications WHERE business_name = '[시드] 캐치캐치 스토어';
DELETE FROM categories WHERE name = '[시드] 상의';

-- ----------------------------------------------------------------------------
-- 1. 카테고리
-- ----------------------------------------------------------------------------
INSERT INTO categories (name, created_at)
VALUES ('[시드] 상의', NOW());
SET @category_id = LAST_INSERT_ID();

-- ----------------------------------------------------------------------------
-- 2. 브랜드
-- ----------------------------------------------------------------------------
INSERT INTO brands (name, logo_url, is_active, created_at, updated_at)
VALUES ('[시드] 캐치캐치', 'https://example.com/brand-logo.png', TRUE, NOW(), NOW());
SET @brand_id = LAST_INSERT_ID();

-- ----------------------------------------------------------------------------
-- 3. 판매자 (product.seller_id 는 sellers가 아니라 seller_applications를 참조)
--    - 상품을 등록하려면 users 테이블에 판매자 계정이 하나 있어야 하므로 같이 생성한다.
--    - 비밀번호 해시는 'password1234' 의 BCrypt 값 (필요하면 이 계정으로 판매자 로그인도 가능)
-- ----------------------------------------------------------------------------
INSERT INTO users (username, password, name, email, phone_number, role, point, created_at, is_deleted)
SELECT '[시드]seller1', '$2b$10$56z2eikZCMvSKSGO7FoSA.EhKeeTmyS4vSBZWGG4z9T5dUkw6Tvd.',
       '시드판매자', 'seed-seller1@example.com', '01099998888', 'SELLER', 0, NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = '[시드]seller1');

SET @seller_user_id = (SELECT id FROM users WHERE username = '[시드]seller1');

INSERT INTO seller_applications (
    user_id, business_name, business_registration_number, representative_name,
    contact_number, business_address, business_registration_file_url,
    mail_order_report_file_url, status, created_at, reviewed_at
)
VALUES (
    @seller_user_id, '[시드] 캐치캐치 스토어', '1234567890', '시드대표',
    '01099998888', '서울시 강남구 테스트로 1', 'https://example.com/biz-reg.pdf',
    'https://example.com/mail-order-cert.pdf', 'APPROVED', NOW(), NOW()
);
SET @seller_application_id = LAST_INSERT_ID();

-- ----------------------------------------------------------------------------
-- 4. 상품 2개 (하나는 브랜드 있음 / 하나는 브랜드 없음 - brandId 필터 테스트용)
-- ----------------------------------------------------------------------------
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller_application_id, @category_id, @brand_id, '[시드] 베이직 반팔 티셔츠', 19900, 10,
        '테스트용 시드 상품입니다.', 'https://example.com/tshirt-thumb.jpg', NOW(), FALSE);
SET @product1_id = LAST_INSERT_ID();

INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller_application_id, @category_id, NULL, '[시드] 노브랜드 에코백', 9900, 0,
        '브랜드 미지정 테스트용 시드 상품입니다.', 'https://example.com/bag-thumb.jpg', NOW(), FALSE);
SET @product2_id = LAST_INSERT_ID();

-- 상품 옵션 (product1: 색상/사이즈 조합 2종, product2: 옵션 1종)
INSERT INTO product_options (product_id, option_name, additional_price, stock_quantity, is_deleted)
VALUES
    (@product1_id, 'Black / M', 0, 50, FALSE),
    (@product1_id, 'White / L', 1000, 30, FALSE),
    (@product2_id, '단일 옵션', 0, 100, FALSE);

-- 상품 이미지 (상세페이지 갤러리)
INSERT INTO product_images (product_id, image_url, sort_order, created_at)
VALUES
    (@product1_id, 'https://example.com/tshirt-detail-1.jpg', 0, NOW()),
    (@product1_id, 'https://example.com/tshirt-detail-2.jpg', 1, NOW()),
    (@product2_id, 'https://example.com/bag-detail-1.jpg', 0, NOW());

-- ----------------------------------------------------------------------------
-- 5. 이벤트 배너 2개 (하나는 상시노출, 하나는 기간제 노출)
-- ----------------------------------------------------------------------------
INSERT INTO banners (title, image_url, link_url, sort_order, is_active, start_at, end_at, created_at)
VALUES
    ('[시드] 여름 신상 프로모션', 'https://example.com/banner1.jpg', 'https://example.com/event/summer', 0, TRUE, NULL, NULL, NOW()),
    ('[시드] 기간한정 반값 세일', 'https://example.com/banner2.jpg', 'https://example.com/event/sale', 1, TRUE, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 7 DAY, NOW());

-- ----------------------------------------------------------------------------
-- 6. 알림 1건 (testuser1 앞으로) - GET /notifications, PATCH .../read 테스트용
-- ----------------------------------------------------------------------------
INSERT INTO notifications (user_id, type, title, content, is_read, created_at)
SELECT id, 'ORDER', '[시드] 알림 테스트', '테스트용 시드 알림입니다.', FALSE, NOW()
FROM users WHERE username = 'testuser1';

-- ----------------------------------------------------------------------------
-- 7. 쿠폰 발급 - "보유 쿠폰 목록 조회" 응답에 데이터가 뜨도록 강제로 지갑에 넣어준다.
--    (실서비스에는 쿠폰 발급/claim API가 아직 없어 이렇게 직접 넣어야 확인 가능)
-- ----------------------------------------------------------------------------
INSERT INTO coupon_requests (
    seller_id, coupon_name, discount_type, discount_value, minimum_order_amount,
    maximum_discount_amount, valid_from, valid_until, total_quantity, status, created_at, reviewed_at
)
VALUES (
    @seller_application_id, '[시드] 웰컴 10% 할인 쿠폰', 'PERCENTAGE', 10.00, 10000.00,
    5000.00, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY, 100, 'APPROVED', NOW(), NOW()
);
SET @coupon_request_id = LAST_INSERT_ID();

INSERT INTO coupons (
    coupon_request_id, seller_id, coupon_name, discount_type, discount_value,
    minimum_order_amount, maximum_discount_amount, valid_from, valid_until,
    total_quantity, issued_quantity, is_active, created_at, updated_at
)
VALUES (
    @coupon_request_id, @seller_application_id, '[시드] 웰컴 10% 할인 쿠폰', 'PERCENTAGE', 10.00,
    10000.00, 5000.00, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY,
    100, 1, TRUE, NOW(), NOW()
);
SET @coupon_id = LAST_INSERT_ID();

INSERT INTO user_coupons (user_id, coupon_id, is_used, created_at)
SELECT id, @coupon_id, FALSE, NOW()
FROM users WHERE username = 'testuser1';

-- ----------------------------------------------------------------------------
-- 완료 - 방금 만든 ID들을 확인해서 Postman 컬렉션 변수에 넣어주세요.
-- ----------------------------------------------------------------------------
SELECT
    @product1_id  AS product1_id_use_this_as_productId,
    @product2_id  AS product2_id_no_brand,
    @brand_id     AS brand_id_use_this_as_brandId,
    @category_id  AS category_id,
    (SELECT id FROM product_options WHERE product_id = @product1_id ORDER BY id LIMIT 1) AS product1_option_id_black_m,
    (SELECT id FROM product_options WHERE product_id = @product1_id ORDER BY id LIMIT 1 OFFSET 1) AS product1_option_id_white_l,
    (SELECT id FROM notifications WHERE title = '[시드] 알림 테스트' ORDER BY id DESC LIMIT 1) AS notification_id,
    @coupon_id AS coupon_id;

-- 이 세션에서만 꺼뒀던 안전모드를 다시 켜둔다 (다른 실수 방지용으로 켜두는 게 안전함)
SET SQL_SAFE_UPDATES = 1;
