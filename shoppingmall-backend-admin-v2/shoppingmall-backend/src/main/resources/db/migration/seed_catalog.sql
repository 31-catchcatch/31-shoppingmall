-- ============================================================================
-- seed_catalog.sql
--
-- 홈페이지(메인/목록/상세/검색/브랜드/위시리스트) 화면 확인용 카탈로그 시드.
-- 프론트 하드코딩 더미를 걷어내고 실제 API/DB 연동을 검증하기 위한 상품 데이터.
--
-- ⚠ 기존 seed_test_data.sql 과는 별개 파일이며 서로 건드리지 않는다.
--   - seed_test_data.sql : Postman 테스트용, '[시드]' 접두어
--   - seed_catalog.sql   : 화면 확인용,   '[카탈로그]' 접두어
--
-- 몇 번이고 다시 실행해도 되도록 기존 카탈로그 데이터를 먼저 지우고 다시 넣는다.
--
-- 실행 방법: MySQL Workbench 등에서 이 파일을 통째로 실행.
--   (testuser1 계정이 있으면 리뷰까지 들어가고, 없으면 리뷰만 건너뛴다)
-- ============================================================================

USE shoppingmall;  -- ⚠ DB 이름이 다르면 이 줄만 본인 환경에 맞게 바꿔주세요.

SET SQL_SAFE_UPDATES = 0;

-- ----------------------------------------------------------------------------
-- 0. 기존 카탈로그 데이터 정리 (재실행 대비) - 하위 테이블부터 삭제
--    상품은 이름이 아니라 '카탈로그 전용 판매자' 기준으로 지운다
--    (상품명이 진짜처럼 보여야 하므로 이름에 접두어를 안 붙이기 때문)
-- ----------------------------------------------------------------------------
SET @catalog_seller_app_id = (
    SELECT sa.id FROM seller_applications sa
    JOIN users u ON u.id = sa.user_id
    WHERE u.username = 'catalog_seller'
    LIMIT 1
);

-- 장바구니가 카탈로그 상품/옵션을 참조하고 있으면 FK 때문에 옵션·상품 삭제가 막히므로 먼저 정리
DELETE FROM cart_items       WHERE product_id IN (SELECT id FROM products WHERE seller_id = @catalog_seller_app_id);
DELETE FROM product_likes    WHERE product_id IN (SELECT id FROM products WHERE seller_id = @catalog_seller_app_id);
DELETE FROM reviews          WHERE product_id IN (SELECT id FROM products WHERE seller_id = @catalog_seller_app_id);
DELETE FROM product_options  WHERE product_id IN (SELECT id FROM products WHERE seller_id = @catalog_seller_app_id);
DELETE FROM product_images   WHERE product_id IN (SELECT id FROM products WHERE seller_id = @catalog_seller_app_id);
DELETE FROM products         WHERE seller_id = @catalog_seller_app_id;
DELETE FROM banners          WHERE title LIKE '[카탈로그]%';
DELETE FROM brands           WHERE name IN ('캐치베이직', '무드로우', '온더코너', '레이어드', '폴리시');
DELETE FROM categories       WHERE name IN ('아우터','상의','셔츠','니트','팬츠','데님','신발','가방','액세서리');
DELETE FROM seller_applications WHERE business_name = '[카탈로그] 캐치캐치 공식스토어';
DELETE FROM users            WHERE username = 'catalog_seller';

-- ----------------------------------------------------------------------------
-- 1. 카테고리 9개 - 전부 최상위(parent_id NULL, 평면 구조)
--    ⚠ 이름이 프론트 js/catalog.js 의 SLUG_TO_NAME 값과 글자 단위로 일치해야 한다.
--      (헤더 nav 의 ?cat=outer 슬러그를 categoryId 로 변환하는 계약)
-- ----------------------------------------------------------------------------
INSERT INTO categories (name, created_at) VALUES
    ('아우터',   NOW()),
    ('상의',     NOW()),
    ('셔츠',     NOW()),
    ('니트',     NOW()),
    ('팬츠',     NOW()),
    ('데님',     NOW()),
    ('신발',     NOW()),
    ('가방',     NOW()),
    ('액세서리', NOW());

SET @cat_outer = (SELECT id FROM categories WHERE name = '아우터'   LIMIT 1);
SET @cat_top   = (SELECT id FROM categories WHERE name = '상의'     LIMIT 1);
SET @cat_shirt = (SELECT id FROM categories WHERE name = '셔츠'     LIMIT 1);
SET @cat_knit  = (SELECT id FROM categories WHERE name = '니트'     LIMIT 1);
SET @cat_pants = (SELECT id FROM categories WHERE name = '팬츠'     LIMIT 1);
SET @cat_denim = (SELECT id FROM categories WHERE name = '데님'     LIMIT 1);
SET @cat_shoes = (SELECT id FROM categories WHERE name = '신발'     LIMIT 1);
SET @cat_bag   = (SELECT id FROM categories WHERE name = '가방'     LIMIT 1);
SET @cat_acc   = (SELECT id FROM categories WHERE name = '액세서리' LIMIT 1);

-- ----------------------------------------------------------------------------
-- 2. 브랜드 5개 (프론트 brand.js 기존 이름 재사용)
--    로고/상품 이미지는 백엔드 업로드 경로 /uploads/products/cat*.jpg 를 쓴다.
--    (SecurityConfig 에 /uploads/** permitAll 이 추가되어 비로그인도 조회 가능)
-- ----------------------------------------------------------------------------
INSERT INTO brands (name, logo_url, is_active, created_at, updated_at) VALUES
    ('캐치베이직', '/uploads/products/cat.jpg',  TRUE, NOW(), NOW()),
    ('무드로우',   '/uploads/products/cat2.jpg', TRUE, NOW(), NOW()),
    ('온더코너',   '/uploads/products/cat3.jpg', TRUE, NOW(), NOW()),
    ('레이어드',   '/uploads/products/cat.jpg',  TRUE, NOW(), NOW()),
    ('폴리시',     '/uploads/products/cat2.jpg', TRUE, NOW(), NOW());

SET @brand_basic  = (SELECT id FROM brands WHERE name = '캐치베이직' LIMIT 1);
SET @brand_mood   = (SELECT id FROM brands WHERE name = '무드로우'   LIMIT 1);
SET @brand_corner = (SELECT id FROM brands WHERE name = '온더코너'   LIMIT 1);
SET @brand_layer  = (SELECT id FROM brands WHERE name = '레이어드'   LIMIT 1);
SET @brand_policy = (SELECT id FROM brands WHERE name = '폴리시'     LIMIT 1);

-- ----------------------------------------------------------------------------
-- 3. 카탈로그 전용 판매자
--    products.seller_id 는 sellers 가 아니라 seller_applications.id 를 참조한다.
--    비밀번호 해시는 'password1234' 의 BCrypt 값 (seed_test_data.sql 과 동일).
-- ----------------------------------------------------------------------------
INSERT INTO users (username, password, name, email, phone_number, role, point, created_at, is_deleted)
SELECT 'catalog_seller', '$2b$10$56z2eikZCMvSKSGO7FoSA.EhKeeTmyS4vSBZWGG4z9T5dUkw6Tvd.',
       '카탈로그판매자', 'catalog-seller@example.com', '01077776666', 'SELLER', 0, NOW(), FALSE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'catalog_seller');

SET @seller_user_id = (SELECT id FROM users WHERE username = 'catalog_seller');

INSERT INTO seller_applications (
    user_id, business_name, business_registration_number, representative_name,
    contact_number, business_address, business_registration_file_url,
    mail_order_report_file_url, status, created_at, reviewed_at
)
VALUES (
    @seller_user_id, '[카탈로그] 캐치캐치 공식스토어', '2208801234', '카탈로그대표',
    '01077776666', '서울시 성동구 연무장길 1', '/uploads/dummy-license.png',
    '/uploads/dummy-mailorder.png', 'APPROVED', NOW(), NOW()
);
SET @seller = LAST_INSERT_ID();

-- ----------------------------------------------------------------------------
-- 4. 상품 48개 (카테고리 9종 × 5~6개, 브랜드 순환)
--
--    설계 의도 (검증을 유도하는 시드):
--    - created_at 을 NOW() - INTERVAL n HOUR 로 전부 다르게 → 최신순 정렬 안정성
--      (동률이 있으면 페이징에서 상품이 중복/누락됨). n 은 아래에서 48→1 로 부여
--    - discount_rate 를 0/10/20/30/50 순환 → finalPrice 계산 및 할인 표기 검증
--    - thumbnail_url 을 cat/cat2/cat3 순환, 단 일부는 NULL → 플레이스홀더 검증
--    - brand_id 일부 NULL → 브랜드명 없는 카드 폴백 검증
--
--    편의를 위해 세션 변수 @h(=시간 오프셋)를 하나 두고 INSERT 마다 1씩 줄인다.
--    (첫 상품이 가장 오래됨=48시간 전, 마지막 상품이 가장 최신=1시간 전)
-- ----------------------------------------------------------------------------
SET @h = 49;   -- 각 INSERT 직전에 SET @h = @h - 1 로 감소시켜 사용

-- ===== 아우터 (5개) =====
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_outer, @brand_basic, '오버핏 울 블렌드 코트', 189000, 20, '겨울용 오버핏 코트. 부드러운 울 혼방 소재.', '/uploads/products/cat.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_outer, @brand_mood, '숏 패딩 점퍼', 129000, 30, '경량 충전재 숏 패딩. 데일리로 좋은 기본 아이템.', '/uploads/products/cat2.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_outer, @brand_corner, '싱글 트렌치 코트', 159000, 0, '봄가을 트렌치 코트. 클래식한 베이지 컬러.', '/uploads/products/cat3.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_outer, NULL, '베이직 후드 집업', 59000, 10, '무지 후드 집업. 데일리 레이어링용.', NULL, NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_outer, @brand_layer, '레더 라이더 자켓', 219000, 50, '소가죽 라이더 자켓. 시즌 오프 특가.', '/uploads/products/cat.jpg', NOW() - INTERVAL @h HOUR, FALSE);

-- ===== 상의 (5개) =====
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_top, @brand_basic, '베이직 반팔 티셔츠', 19000, 0, '데일리 무지 반팔티. 사계절 기본템.', '/uploads/products/cat2.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_top, @brand_mood, '오버핏 롱슬리브', 29000, 10, '루즈핏 긴팔 티셔츠. 부드러운 코튼.', '/uploads/products/cat3.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_top, @brand_corner, '스트라이프 맨투맨', 39000, 20, '배색 스트라이프 맨투맨. 캐주얼한 무드.', '/uploads/products/cat.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_top, NULL, '피그먼트 무지 티', 24000, 0, '워싱 가공 피그먼트 반팔. 빈티지한 컬러감.', NULL, NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_top, @brand_policy, '크롭 슬리브리스', 22000, 30, '여름용 크롭 나시. 데일리 코디.', '/uploads/products/cat2.jpg', NOW() - INTERVAL @h HOUR, FALSE);

-- ===== 셔츠 (5개) =====
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_shirt, @brand_basic, '옥스포드 베이직 셔츠', 45000, 10, '기본 옥스포드 셔츠. 오피스룩에 좋은 아이템.', '/uploads/products/cat3.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_shirt, @brand_mood, '오버핏 체크 셔츠', 49000, 20, '루즈핏 체크 패턴 셔츠. 레이어드용.', '/uploads/products/cat.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_shirt, @brand_corner, '린넨 하프 셔츠', 42000, 30, '여름용 린넨 반팔 셔츠. 시원한 착용감.', '/uploads/products/cat2.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_shirt, @brand_layer, '스트라이프 데일리 셔츠', 47000, 0, '잔잔한 스트라이프 셔츠. 데일리 코디.', '/uploads/products/cat3.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_shirt, @brand_policy, '코듀로이 셔츠 자켓', 62000, 50, '두꺼운 코듀로이 셔켓. 간절기 아우터 겸용.', '/uploads/products/cat.jpg', NOW() - INTERVAL @h HOUR, FALSE);

-- ===== 니트 (5개) =====
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_knit, @brand_basic, '라운드넥 베이직 니트', 39000, 10, '기본 라운드넥 스웨터. 부드러운 촉감.', '/uploads/products/cat2.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_knit, @brand_mood, '케이블 꽈배기 니트', 49000, 20, '꽈배기 패턴 니트. 도톰한 겨울용.', '/uploads/products/cat3.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_knit, NULL, '브이넥 슬림 니트', 35000, 0, '슬림핏 브이넥 니트. 이너로 좋은 아이템.', NULL, NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_knit, @brand_layer, '오버핏 터틀넥 니트', 45000, 30, '루즈핏 목폴라 니트. 따뜻한 겨울 필수템.', '/uploads/products/cat.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_knit, @brand_policy, '가디건 니트 자켓', 55000, 50, '롱 가디건. 레이어링용 시즌오프 특가.', '/uploads/products/cat2.jpg', NOW() - INTERVAL @h HOUR, FALSE);

-- ===== 팬츠 (5개) =====
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_pants, @brand_basic, '베이직 슬랙스', 42000, 10, '기본 정장 슬랙스. 오피스룩 필수템.', '/uploads/products/cat3.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_pants, @brand_mood, '와이드 코튼 팬츠', 45000, 20, '와이드핏 면바지. 편안한 데일리 팬츠.', '/uploads/products/cat.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_pants, @brand_corner, '조거 트레이닝 팬츠', 35000, 0, '밴딩 조거 팬츠. 편한 트레이닝 룩.', '/uploads/products/cat2.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_pants, @brand_layer, '치노 스트레이트 팬츠', 39000, 30, '스트레이트핏 치노 팬츠. 깔끔한 캐주얼.', '/uploads/products/cat3.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_pants, @brand_policy, '카고 워크 팬츠', 49000, 50, '카고 포켓 워크 팬츠. 스트릿 무드.', '/uploads/products/cat.jpg', NOW() - INTERVAL @h HOUR, FALSE);

-- ===== 데님 (5개) =====
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_denim, @brand_basic, '레귤러핏 청바지', 49000, 10, '기본 레귤러핏 데님. 사계절 데일리.', '/uploads/products/cat2.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_denim, @brand_mood, '와이드 데님 팬츠', 52000, 20, '와이드핏 청바지. 트렌디한 실루엣.', '/uploads/products/cat3.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_denim, NULL, '워싱 스키니 진', 45000, 0, '슬림 스키니 데님. 다리 라인 강조.', NULL, NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_denim, @brand_layer, '데님 트러커 자켓', 69000, 30, '기본 청자켓. 레이어드 아우터.', '/uploads/products/cat.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_denim, @brand_policy, '크롭 데님 스커트', 38000, 50, '하이웨스트 데님 스커트. 시즌오프 특가.', '/uploads/products/cat2.jpg', NOW() - INTERVAL @h HOUR, FALSE);

-- ===== 신발 (5개) =====
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_shoes, @brand_basic, '캔버스 스니커즈', 59000, 10, '데일리 캔버스 운동화. 어디에나 잘 어울림.', '/uploads/products/cat3.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_shoes, @brand_mood, '청키 러닝화', 89000, 20, '볼륨감 있는 청키 스니커즈. 편한 착화감.', '/uploads/products/cat.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_shoes, @brand_corner, '레더 로퍼', 99000, 0, '소가죽 로퍼. 세미 정장룩에 어울림.', '/uploads/products/cat2.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_shoes, @brand_layer, '첼시 앵클 부츠', 119000, 30, '사이드 고어 첼시 부츠. 가을겨울 필수템.', '/uploads/products/cat3.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_shoes, @brand_policy, '스웨이드 더비 슈즈', 109000, 50, '스웨이드 더비. 시즌오프 반값 특가.', '/uploads/products/cat.jpg', NOW() - INTERVAL @h HOUR, FALSE);

-- ===== 가방 (5개) =====
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_bag, @brand_basic, '데일리 에코백', 15000, 0, '기본 코튼 에코백. 가벼운 데일리백.', '/uploads/products/cat2.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_bag, @brand_mood, '미니 크로스백', 45000, 10, '컴팩트 크로스백. 데일리 필수 아이템.', '/uploads/products/cat3.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_bag, @brand_corner, '레더 토트백', 89000, 20, '소가죽 토트백. 오피스룩에 잘 어울림.', '/uploads/products/cat.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_bag, NULL, '캔버스 백팩', 55000, 30, '데일리 백팩. 넉넉한 수납.', NULL, NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_bag, @brand_policy, '나일론 숄더백', 39000, 50, '경량 나일론 숄더백. 시즌오프 특가.', '/uploads/products/cat2.jpg', NOW() - INTERVAL @h HOUR, FALSE);

-- ===== 액세서리 (8개, 마지막이 가장 최신) =====
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_acc, @brand_basic, '베이직 볼캡', 25000, 10, '기본 야구모자. 데일리 캡.', '/uploads/products/cat3.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_acc, @brand_mood, '니트 비니', 19000, 20, '겨울용 니트 비니. 다양한 컬러.', '/uploads/products/cat.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_acc, @brand_corner, '레더 벨트', 29000, 0, '소가죽 벨트. 심플한 버클.', '/uploads/products/cat2.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_acc, @brand_layer, '울 머플러', 32000, 30, '도톰한 울 머플러. 겨울 방한.', '/uploads/products/cat3.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_acc, NULL, '실버 체인 목걸이', 22000, 0, '미니멀 실버 체인. 데일리 주얼리.', NULL, NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_acc, @brand_policy, '스퀘어 선글라스', 45000, 50, '스퀘어 프레임 선글라스. 시즌오프 특가.', '/uploads/products/cat.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_acc, @brand_basic, '가죽 카드지갑', 28000, 10, '슬림 카드지갑. 심플한 디자인.', '/uploads/products/cat2.jpg', NOW() - INTERVAL @h HOUR, FALSE);
SET @h = @h - 1;
INSERT INTO products (seller_id, category_id, brand_id, name, price, discount_rate, description, thumbnail_url, created_at, is_deleted)
VALUES (@seller, @cat_acc, @brand_mood, '클래식 가죽 시계', 33000, 20, '심플한 아날로그 손목시계. 데일리 액세서리.', '/uploads/products/cat3.jpg', NOW() - INTERVAL @h HOUR, FALSE);

-- ----------------------------------------------------------------------------
-- 5. 옵션 (모든 카탈로그 상품에 S/M/L/XL 4종, 일부 상품은 XL 품절)
--    상품마다 손으로 넣지 않고, 방금 넣은 카탈로그 상품 전체를 대상으로 일괄 생성.
-- ----------------------------------------------------------------------------
INSERT INTO product_options (product_id, option_name, additional_price, stock_quantity, is_deleted)
SELECT p.id, 'S', 0, 20, FALSE FROM products p WHERE p.seller_id = @seller;
INSERT INTO product_options (product_id, option_name, additional_price, stock_quantity, is_deleted)
SELECT p.id, 'M', 0, 30, FALSE FROM products p WHERE p.seller_id = @seller;
INSERT INTO product_options (product_id, option_name, additional_price, stock_quantity, is_deleted)
SELECT p.id, 'L', 0, 15, FALSE FROM products p WHERE p.seller_id = @seller;
-- XL 은 짝수 id 상품만 품절(0), 홀수 id 상품은 재고 있음 → soldOut UI 검증
INSERT INTO product_options (product_id, option_name, additional_price, stock_quantity, is_deleted)
SELECT p.id, 'XL', 2000, IF(p.id % 2 = 0, 0, 10), FALSE FROM products p WHERE p.seller_id = @seller;

-- ----------------------------------------------------------------------------
-- 6. 상세 갤러리 이미지 (썸네일이 있는 상품은 3장, NULL 인 상품은 상세도 비움)
-- ----------------------------------------------------------------------------
INSERT INTO product_images (product_id, image_url, sort_order, created_at)
SELECT p.id, '/uploads/products/cat.jpg',  0, NOW() FROM products p WHERE p.seller_id = @seller AND p.thumbnail_url IS NOT NULL;
INSERT INTO product_images (product_id, image_url, sort_order, created_at)
SELECT p.id, '/uploads/products/cat2.jpg', 1, NOW() FROM products p WHERE p.seller_id = @seller AND p.thumbnail_url IS NOT NULL;
INSERT INTO product_images (product_id, image_url, sort_order, created_at)
SELECT p.id, '/uploads/products/cat3.jpg', 2, NOW() FROM products p WHERE p.seller_id = @seller AND p.thumbnail_url IS NOT NULL;

-- ----------------------------------------------------------------------------
-- 7. 리뷰 (testuser1 이 있으면 상품마다 개수 차등으로 삽입 → 별점/리뷰수 검증)
--    reviews 는 order_detail FK 가 없어 상품에 바로 붙일 수 있다.
--    상품 id 를 5로 나눈 나머지에 따라 0~4개씩 넣어 상품별 리뷰수를 다르게 한다.
-- ----------------------------------------------------------------------------
SET @reviewer = (SELECT id FROM users WHERE username = 'testuser1' LIMIT 1);

-- testuser1 이 없으면 이 블록은 0건 삽입되고 넘어간다 (@reviewer IS NULL)
INSERT INTO reviews (user_id, product_id, rating, content, image_url, is_deleted, created_at)
SELECT @reviewer, p.id, 5, '만족스러운 상품이에요. 재구매 의사 있습니다.', NULL, FALSE, NOW()
FROM products p WHERE p.seller_id = @seller AND @reviewer IS NOT NULL AND (p.id % 5) >= 1;
INSERT INTO reviews (user_id, product_id, rating, content, image_url, is_deleted, created_at)
SELECT @reviewer, p.id, 4, '가성비 좋아요. 배송도 빨랐습니다.', NULL, FALSE, NOW()
FROM products p WHERE p.seller_id = @seller AND @reviewer IS NOT NULL AND (p.id % 5) >= 2;
INSERT INTO reviews (user_id, product_id, rating, content, image_url, is_deleted, created_at)
SELECT @reviewer, p.id, 5, '핏이 예뻐요. 색상도 화면과 동일합니다.', NULL, FALSE, NOW()
FROM products p WHERE p.seller_id = @seller AND @reviewer IS NOT NULL AND (p.id % 5) >= 3;
INSERT INTO reviews (user_id, product_id, rating, content, image_url, is_deleted, created_at)
SELECT @reviewer, p.id, 3, '무난합니다. 사이즈는 정사이즈예요.', NULL, FALSE, NOW()
FROM products p WHERE p.seller_id = @seller AND @reviewer IS NOT NULL AND (p.id % 5) >= 4;

-- ----------------------------------------------------------------------------
-- 8. 배너 3개 (커밋된 이미지 사용)
-- ----------------------------------------------------------------------------
INSERT INTO banners (title, image_url, link_url, sort_order, is_active, start_at, end_at, created_at) VALUES
    ('[카탈로그] 신상품 기획전',   '/uploads/products/cat.jpg',  'product-list.html?view=best', 0, TRUE, NULL, NULL, NOW()),
    ('[카탈로그] 시즌오프 세일',   '/uploads/products/cat2.jpg', 'product-list.html?cat=outer', 1, TRUE, NULL, NULL, NOW()),
    ('[카탈로그] 브랜드 위크',     '/uploads/products/cat3.jpg', 'brand.html',                  2, TRUE, NULL, NULL, NOW());

-- ----------------------------------------------------------------------------
-- 완료 - 삽입 결과 요약
-- ----------------------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM products         WHERE seller_id = @seller) AS product_count,
    (SELECT COUNT(*) FROM product_options  WHERE product_id IN (SELECT id FROM products WHERE seller_id = @seller)) AS option_count,
    (SELECT COUNT(*) FROM product_images   WHERE product_id IN (SELECT id FROM products WHERE seller_id = @seller)) AS image_count,
    (SELECT COUNT(*) FROM reviews          WHERE product_id IN (SELECT id FROM products WHERE seller_id = @seller)) AS review_count,
    (SELECT COUNT(*) FROM categories       WHERE name IN ('아우터','상의','셔츠','니트','팬츠','데님','신발','가방','액세서리')) AS category_count,
    (SELECT COUNT(*) FROM brands           WHERE name IN ('캐치베이직','무드로우','온더코너','레이어드','폴리시')) AS brand_count;

SET SQL_SAFE_UPDATES = 1;
