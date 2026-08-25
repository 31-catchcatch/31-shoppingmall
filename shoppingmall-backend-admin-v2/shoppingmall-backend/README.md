# ShoppingMall Backend (WAS-01)

## 📌 관리자(admin) + 포인트(point) 도메인 추가 안내

### 이번에 결정된 것들

1. **관리자 로그인은 완전히 분리된 엔드포인트** — `POST /api/v1/admin/users`. 일반/판매자 로그인(`/auth/user/login`, `/auth/seller/login`)과 절대 같은 URL을 쓰지 않도록 `AuthService.login()`에서 `Role.ADMIN`도 명시적으로 막아뒀습니다. 이렇게 분리한 이유는 바로 아래 IP 제한 때문입니다.
2. **사용자 정지 기능은 이번엔 안 만듦** — `User` 엔티티에 정지 여부를 나타낼 컬럼이 DB 정의서에도 없어서, 지금은 조회(`GET /admin/users`)만 만들고 정지 기능은 범위에서 뺐습니다. 나중에 필요해지면 `users` 테이블에 `is_suspended` 컬럼 추가 + `User.suspend()` 메서드만 추가하면 됩니다.
3. **포인트는 조회 + 관리자 수동 조정만** — 구매확정/환불 시 자동 적립·회수는 아직 없습니다. `PointService.adjustPoint(userId, amount, reason)` 하나로 적립/차감을 다 처리하니, 나중에 자동화할 때 이 메서드를 order/refund 서비스 쪽에서 호출하면 됩니다.
4. **관리자 첫 계정은 DB에 직접 INSERT** — 회원가입 API가 스펙에 없으므로, 아래 SQL로 로컬/운영 DB에 직접 넣어서 테스트하세요 (비밀번호는 BCrypt로 미리 인코딩해야 함 - 로컬에서 회원가입 API로 테스트 계정 하나 만든 뒤 그 해시를 복사해서 role만 ADMIN으로 바꾸는 게 제일 간편합니다).

### ⚠️ 배포 시 필수 — Nginx에서 `/api/v1/admin/` IP 제한

애플리케이션 코드에는 IP 체크를 넣지 않았습니다 (프록시 뒤에서는 실제 클라이언트 IP 판별이 불안정해서). **web-01의 Nginx 설정에서 처리하세요:**

```nginx
location /api/v1/admin/ {
    allow  1.2.3.4;      # 사무실/VPN IP로 교체
    deny   all;
    proxy_pass http://was-01_IP:8080;
}
```

인프라 가이드의 `nginx.conf` (`location /api/`) 블록보다 **더 구체적인 경로라 먼저 매치되므로 위쪽에 추가**하시면 됩니다.

### 새로 생긴 연결 로직

- **입점 승인 → 실제 판매자 전환**: `AdminSellerService.reviewApplication()`이 승인 시 `SellerApplication.approve()` + `Seller` 엔티티 생성 + `User.role`을 SELLER로 승격까지 한 번에 처리합니다. 이 연결이 이전까지는 빠져 있었어요 (신청서 상태만 바뀌고 실제로는 판매자가 안 되는 상태였음).
- **쿠폰 승인 → 실제 쿠폰 발행**: `AdminCouponService.reviewRequest()`가 승인 시 이미 sell이 만들어둔 `Coupon.from(CouponRequest)` 팩토리로 실제 쿠폰을 발행합니다.
- **정산 대시보드**: `SellerSettlementService`와 동일한 방식(별도 정산 테이블 없이 구매확정 주문 데이터 실시간 집계)을 판매자 전체로 확장해서 재사용했습니다. 수수료율(10%)도 동일하게 맞춰뒀습니다.

---


## 📌 병합 안내 2차 (sell 판매자 파트 병합)

kim+seo 병합본 위에 **sell(판매자 파트)** 코드를 추가로 병합했습니다. 이번엔 서로 다른 두 사람이 **Seller, Order, Qna를 각자 독립적으로 새로 설계**해서 실제 충돌이 있었고, 아래처럼 정리했습니다.

### 채택한 설계 (sell 버전 기준)

- **Seller / SellerApplication 분리** — `Seller`(승인된 판매자, ACTIVE/SUSPENDED/CLOSED/FORCED_CLOSED)와 `SellerApplication`(입점 신청서, PENDING/APPROVED/REJECTED/CANCELED)을 분리한 sell의 설계를 그대로 채택했습니다. 관리자 승인 API 구조와 정확히 맞아떨어지기 때문입니다.
- **Order / OrderDetail 재설계** — 배송 상태(`DeliveryStatus`)를 상품 단위로, 주문 전체 상태(`OrderStatus`)를 주문 단위로 분리한 sell 설계를 채택. 다만 **`OrderDetail.productOption` 필드는 병합 시 다시 추가**했습니다 (사이즈/수량 옵션이 있는 상품 주문·재고 확인을 위해 필요).
- **Qna / QnaAnswer 분리** — 답변을 별도 엔티티로 분리한 sell 설계 채택. 단, 비밀글 마스킹 로직(kim이 만든 것)이 sell 버전엔 없어서, `QnaResponse.from(Qna, Long currentUserId)` 오버로드로 다시 추가했습니다.
- **`Product.seller` 필드 타입은 `SellerApplication`을 유지**했습니다. 처음엔 `Seller` 타입이 맞는 것 아닌가 싶었는데, sell이 만든 `SellerProductService`/`SellerOrderService`/`SellerQnaService` 등 판매자 도메인 전체가 이미 `SellerApplication.id` 기준으로 일관되게 짜여 있어서, 바꾸면 오히려 더 많은 코드를 건드려야 했습니다. (다만 이 방식은 "승인된 신청서"와 "현재 판매자 상태(정지 등)"가 별개로 관리된다는 뜻이라, 나중에 판매자를 정지시켜도 이미 등록된 상품엔 반영이 안 되는 구조적 허점이 있습니다 - 여유 있을 때 정리하면 좋을 부분입니다.)

### 병합 중 발견해서 고친 버그

- **`Long userId = 1L;` 하드코딩** — sell이 만든 컨트롤러 10개(`SellerApplicationController`, `SellerProductController`, `SellerOrderController`, `SellerClaimController`, `SellerCouponController`, `SellerDashboardController`, `SellerQnaController`, `SellerRefundController`, `SellerSettlementController`, `ClaimController`, 예전 `OrderController`)가 전부 로그인 사용자 ID를 `1L`로 하드코딩해두고 "TODO JWT 연동 후 교체" 주석만 남겨뒀던 상태였습니다. 전부 `@AuthenticationPrincipal CustomUserDetails userDetails`로 교체했습니다. **이게 없었으면 배포해도 모든 사용자가 사용자 1번으로 동작하는 심각한 버그였을 거예요.**
- **QnA 등록 API가 인증 없이 뚫려있던 문제** — sell이 `POST /products/{id}/qna`(로그인 필요)를 `GET /products/{id}/qna`(공개)와 같은 경로로 만들면서, 기존 `SecurityConfig`의 permitAll 패턴이 경로 단위라 POST까지 같이 뚫려 있었습니다. `SecurityConfig`에서 GET만 permitAll로 걸도록 수정했습니다.
- **`ErrorCode.java` 자체 중복 코드** — sell 코드에 `CLAIM-001`, `SELLER-013`이 각각 두 번씩 쓰이고 있어서 재배치했습니다.
- **`ProductDetailResponse`가 옛 `Seller.getCompanyName()` 참조** — `Product.seller`가 `SellerApplication` 타입으로 바뀌면서 `getBusinessName()`으로 수정했습니다.
- **`AuthController`의 `/auth/seller/login` 중복 매핑** — sell이 `domain.seller.controller.SellerAuthController`에 같은 URL을 이미 구현해뒀어서, 기존 auth 패키지의 것은 제거했습니다 (안 그러면 Spring 기동 시 "Ambiguous mapping" 에러). `/auth/seller/signup`은 계정 생성과 입점 신청(SellerApplication PENDING)을 한 번에 처리합니다. (2026-08-21 정정: 종전에 별도로 두었던 `/seller/applications` 는 `hasRole("SELLER")` 보호 경로라 신청자가 호출할 수 없어 제거했습니다.)

### 이번엔 실제로 컴파일 검증을 못 했습니다

이 병합은 여러 도메인에 걸쳐 필드명/타입이 바뀐 대규모 작업이라, 등장하는 모든 참조를 grep으로 찾아 고쳤지만 **실제 `./gradlew build`를 돌려본 건 아닙니다** (환경 제약, 앞서 설명한 것과 동일). 받으시면 제일 먼저 빌드부터 한번 돌려보시고, 컴파일 에러 나면 로그 그대로 보여주세요.

---


## 📌 병합 안내 (2026-07-14)

이 코드는 **seo(공통/인증)** 와 **kim(일반 사용자: cart/order/payment/qna/review/mypage)** 두 zip을 병합한 결과입니다. 병합 중 발견되어 수정한 부분:

1. **`JwtAuthenticationFilter` 원래 버그 수정** — seo 버전은 인증 성공 시 `Authentication`의 principal에 `Long userId`만 넣고 있었는데, kim이 만든 모든 컨트롤러(cart/order/payment/qna/review/mypage/address)는 `@AuthenticationPrincipal CustomUserDetails userDetails` 로 받아서 `userDetails.getUser().getId()` 로 꺼내 쓰는 방식이었습니다. 타입이 안 맞아서 그대로 두면 로그인한 상태로 저 API들을 호출하는 순간 전부 500 에러가 났을 거예요. **필터가 토큰의 userId로 User를 조회해서 `CustomUserDetails`로 감싸 넣도록 고쳤습니다.** (요청마다 DB 조회가 하나 추가되는 트레이드오프가 있긴 한데, 지금 규모에서는 문제없을 거예요)
2. **`ErrorCode.java` AUTH-008 코드 충돌** — seo는 `WRONG_LOGIN_ENDPOINT`, kim은 `INVALID_PASSWORD`를 둘 다 AUTH-008로 씀. kim의 `INVALID_PASSWORD`를 **AUTH-012로 재배치**했습니다 (enum 상수명은 그대로라 kim의 코드는 안 고쳐도 됨, 응답 JSON의 `code` 문자열 값만 바뀝니다).
3. **`SecurityConfig`는 충돌 없음** — 두 분 다 안 건드렸고, 원래 있던 permitAll 규칙이 kim이 만든 `/products/{id}/reviews`, `/products/{id}/qna` GET과도 정확히 맞아떨어져서 수정 불필요했습니다.

### ⚠️ 확인 필요 — 테이블명이 DB 정의서와 다른 엔티티

`ddl-auto: validate`로 실제 db-01에 붙이면 아래 엔티티들은 테이블을 못 찾아서 기동이 실패합니다. DB 정의서 기준으로 바꿀지, 정의서를 kim 코드 기준으로 바꿀지 결정해서 알려주시면 반영하겠습니다.

| 엔티티 | 코드상 테이블명 | DB 정의서 테이블명 |
|---|---|---|
| `CartItem` | `cart_items` | `carts` |
| `OrderDetail` | `order_details` | `order_items` |
| `Qna` | `qnas` | `product_inquiries` |
| `Address` | `user_addresses` | `delivery_addresses` |

(`Order`, `Payment`, `Review`는 정의서와 일치해서 문제없습니다.)

---


3-Tier 인프라(Nginx → Tomcat/Spring Boot → MySQL) 기준 백엔드 뼈대 코드.
`ShoppingMall_DB_Definition_v2.xlsx`, `ShoppingMall_API_Specification_v6.xlsx`, 웹 기능 정의서, `infra 구성 설치 가이드`를 기준으로 구성했습니다.

## ⚠️ 먼저 확인할 것: Spring Boot 3.x

infra 가이드의 `was-01` 은 **Tomcat 10.1** 입니다. Tomcat 10부터 서블릿 API 네임스페이스가
`javax.*` → `jakarta.*` 로 바뀌었기 때문에, **반드시 Spring Boot 3.x** 를 써야 합니다.
Spring Boot 2.x(=Tomcat 9/`javax` 기준)로 만든 WAR을 Tomcat 10.1에 올리면 배포 자체가 깨집니다.
팀 전체(특히 새로 합류하는 백엔드 인원)에 공유해두세요.

## 기술 스택

| 계층 | 선택 | 비고 |
|---|---|---|
| 언어/프레임워크 | Java 17, Spring Boot 3.3.4 | Tomcat 10.1 호환 |
| 빌드 도구 | Gradle | |
| 영속성 | Spring Data JPA (Hibernate) | 생산성 우선. 정산/통계처럼 복잡한 쿼리가 필요해지면 해당 부분만 `@Query`/native SQL로 보강 |
| 인증 | JWT (jjwt 0.12.x) + Spring Security | Stateless, `/api/v1/auth/**` 외 API는 기본적으로 인증 필요 |
| DB | MySQL 8.0 (db-01, 192.168.10.10) | |

## 패키지 구조

```
com.shoppingmall
├── global                     # 도메인에 상관없이 공통으로 쓰는 것들
│   ├── config/SecurityConfig  # 필터체인, 인증/인가 규칙 (permitAll / hasRole 매핑)
│   ├── security/jwt           # JwtTokenProvider, JwtAuthenticationFilter, 401/403 핸들러
│   ├── common                 # ApiResponse<T>, PageResponse<T>, BaseTimeEntity
│   └── exception              # CustomException, ErrorCode, GlobalExceptionHandler
└── domain
    ├── user       (구현 완료 - kim)      # User/Role 엔티티 + 마이페이지 조회/수정 + 배송지 주소록 CRUD
    ├── seller     (entity)              # Seller (users 1:1 확장)
    ├── auth       (구현 완료)            # 로그인/회원가입/로그아웃/토큰재발급(rotation)/판매자 인증/본인인증(mock)/이메일인증(mock)/계정찾기
    ├── product    (구현 완료 예시)       # 상품 리스트/상세, 카테고리 트리, Category/Product/ProductImage/ProductOption
    ├── search     (구현 완료)            # 검색어 자동완성 (상품명 LIKE 기준)
    ├── file       (구현 완료)            # 파일 업로드 (was-01 로컬 디스크 - S3 연동 전 임시)
    ├── notification (구현 완료)          # 알림 발송/이력 저장 (다른 도메인이 서비스로 직접 호출하는 내부 연동 지점)
    ├── cart       (구현 완료 - kim)      # 장바구니 조회/추가/수정/삭제
    ├── order      (구현 완료 - kim+sell 통합) # 주문서 조회/생성/구매확정/주문내역
    ├── payment    (구현 완료 - kim)      # 결제 검증
    ├── qna        (구현 완료 - kim+sell 통합) # 상품 문의 등록/조회 (답변은 QnaAnswer 분리)
    ├── review     (구현 완료 - kim)      # 상품 리뷰 등록/조회
    ├── claim      (구현 완료 - sell)     # 교환/환불 신청 (일반 사용자)
    ├── coupon     (구현 완료 - sell)     # 쿠폰 발행요청/보유쿠폰 (일반 사용자 + 판매자 발행요청)
    ├── point      (구현 완료 - 관리자 작업 시 신규)  # 포인트 내역 조회(사용자) + 조정(관리자) 공용
    ├── admin      (구현 완료 - 관리자 작업 시 신규)  # 사용자/판매자 승인, 쿠폰 승인, 정산 대시보드 등
    └── seller     (구현 완료 - sell)     # 입점신청/상품/주문목록·배송/매출통계/클레임/쿠폰요청/대시보드/Q&A/환불/정산
```

### 공통/인증 도메인 - 실제 연동 필요(TODO) 표시된 부분

아래는 mock으로 만들어서 인터페이스는 API 명세서와 동일하지만, 실제 외부 서비스 연동은 안 된 상태입니다:

| API | 현재 상태 |
|---|---|
| `POST /auth/user/verify`, `/auth/seller/verify` | 항상 성공(verified=true) 응답. 실제 SMS/PASS 연동 필요 |
| `POST /auth/email-verification` | 인증코드를 실제 발송 안 하고 서버 로그에만 출력. 인메모리 저장이라 was-01 인스턴스가 여러 대로 늘어나면 Redis로 교체 필요 |
| `POST /auth/user/find-account`, `/auth/seller/find-account` (PASSWORD) | 임시 비밀번호 발급은 되지만 실제 메일 발송 안 하고 로그로만 출력 |
| `POST /files/upload` | S3 대신 was-01 로컬 디스크(`file.upload-dir`)에 저장 + `/uploads/**` 정적 서빙 |
| `POST /notifications/send` | DB 이력 저장까지만 하고 실제 알림톡/문자 발송 안 함. **다른 도메인(order/payment/qna) 담당자는 `NotificationService.send(...)` 를 직접 주입받아 호출하면 됨** (REST 엔드포인트가 아니라 내부 Java 호출이 정식 사용법) |

각 TODO 패키지의 `package-info.java` 에 어떤 DB 정의서 테이블 / API 명세서 엔드포인트를 담당하는지 적어뒀습니다.
새 도메인을 만들 때는 `auth`, `product` 패키지의 계층 구조(`entity / repository / service / controller / dto`)를 그대로 복사해서 시작하면 됩니다.

## 실행 방법

이 프로젝트엔 Gradle Wrapper 바이너리(`gradle-wrapper.jar`)가 빠져 있습니다(샌드박스에 네트워크 제한이 있어 다운로드 불가).
아래 둘 중 하나로 생성하세요.

1. **IntelliJ로 폴더 열기** → 최초 1회 Gradle sync 시 자동으로 wrapper가 생성/사용됩니다. (가장 간단)
2. 로컬에 Gradle이 설치되어 있다면:
   ```bash
   gradle wrapper --gradle-version 8.10
   ```

이후:
```bash
./gradlew bootRun                 # 로컬에서 바로 실행 (내장 톰캣, 8080)
./gradlew bootWar                 # ROOT.war 생성 -> build/libs/ROOT.war
```

### DB 접속 설정

`application.yml` 은 팀 공용이라 실제 비밀번호를 넣지 않았습니다. 로컬 실행 시:

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
./gradlew bootRun --args='--spring.profiles.active=local'
```

또는 환경변수로 직접 주입:
```bash
export DB_HOST=192.168.10.10
export DB_USERNAME=appuser
export DB_PASSWORD=AppUserPass!2026
export JWT_SECRET=아무거나-256비트-이상-랜덤값
./gradlew bootRun
```

### was-01(Tomcat)에 배포

```bash
./gradlew bootWar
# 생성된 build/libs/ROOT.war 를 was-01의 /opt/tomcat/webapps/ROOT.war 로 교체
# application.properties(코드가 아닌 배포 서버 쪽 설정)에 DB_HOST/DB_USERNAME/DB_PASSWORD/JWT_SECRET 환경변수 설정 필요
```

## 다음 스프린트 제안 순서

DB 정의서의 "확인이 더 필요한 항목"과 API 우선순위를 함께 고려하면:

1. **cart → order → payment** : 즉시구매/장바구니 모두 `POST /orders` 하나로 처리한다는 가정이 DB 정의서에 명시되어 있으니, 프론트와 먼저 확정 필요
2. **coupon** : 판매자 발행요청 → 관리자 승인 워크플로우가 얽혀 있어 seller/admin 도메인과 같이 설계
3. **review / qna** : 비교적 독립적이라 병렬 작업 가능
4. **admin (settlements 포함)** : 화면 정의서에 정산 관리 화면 자체가 아직 없다고 DB 정의서에 적혀 있으므로, 화면 스펙부터 프론트와 협의 필요

## 참고

- `is_deleted` 컬럼이 있는 테이블(users, products, product_options, reviews)은 물리 삭제 대신 반드시 soft delete로 처리하세요.
- 모든 신규 엔티티는 `BaseTimeEntity` 를 상속해서 `created_at`/`updated_at` 을 직접 관리하지 않도록 하세요 (단, `categories`, `product_images`, `notifications` 처럼 `updated_at` 이 없는 테이블은 예외 - 각 엔티티 참고).
- **`refresh_tokens` 테이블이 DB 정의서에 신규 추가되었습니다** (id, user_id, token, expires_at, created_at). 로그인 시 발급, `/auth/refresh` 호출 시 rotation(기존 삭제 후 재발급), `/auth/user/logout` 호출 시 삭제됩니다. db-01 실제 스키마에도 이 테이블이 반영되어 있는지 확인 필요합니다.
