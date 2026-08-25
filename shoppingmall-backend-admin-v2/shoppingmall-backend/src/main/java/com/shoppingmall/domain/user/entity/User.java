package com.shoppingmall.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DB 정의서 'users' 테이블 매핑.
 * sellers 테이블은 users 를 확장(1:1, user_id UNIQUE FK)하는 구조이므로
 * 판매자도 role='SELLER' 인 User 행을 가진다.
 */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** [5-2 조치] 어떤 경로로도 직렬화되지 않도록 못을 박는다 (근본 조치는 응답 DTO 사용). */
    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password; // BCrypt 해시

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private Integer point;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    // ===== [3-2 조치] 로그인 실패 횟수 제한 =====

    @Column(name = "login_fail_count", nullable = false)
    private int loginFailCount;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // ===== [4-2 조치] 로그아웃 시 Access Token 무효화 =====
    //
    // 무효화 기준 시각은 여기(users.token_invalidated_at 컬럼)에 두지 않는다.
    // 이 기록의 수명은 Access Token 유효기간(15분)이면 충분한 임시 정보라서,
    // 스키마를 늘리는 대신 TokenInvalidationRegistry 가 메모리로 들고 있다.
    // ⚠️ 여기에 필드를 되살리면 ddl-auto: update 로 구동되는 was-01 이
    //    기동만으로 컬럼을 다시 만든다. 그게 이 방식을 택한 이유다.

    @Builder
    public User(String username, String password, String name, String email,
                String phoneNumber, Role role) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role == null ? Role.USER : role;
        this.point = 0;
        this.deleted = false;
        this.loginFailCount = 0;   // [3-2]
    }

    // ===== [3-2 조치] 로그인 실패 횟수 관련 도메인 메서드 =====

    /** 현재 잠금 상태인지 판정한다. */
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    /** 로그인 실패 1건 반영. 임계값에 도달하면 잠근다. */
    public void recordLoginFailure(int threshold, int lockMinutes) {
        this.loginFailCount++;
        if (this.loginFailCount >= threshold) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
        }
    }

    /** 로그인 성공 시 카운터를 초기화한다. */
    public void recordLoginSuccess() {
        this.loginFailCount = 0;
        this.lockedUntil = null;
        this.lastLoginAt = LocalDateTime.now();
    }

    // [4-2 조치] 토큰 무효화는 TokenInvalidationRegistry 담당 (위 주석 참고).
    // 비밀번호 변경·계정 정지에서 전 기기 강제 로그아웃이 필요하면
    // 그 레지스트리의 invalidate(userId) 를 부르면 된다.

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateProfile(String name, String email, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    /** 판매자 마이페이지(PUT /seller/me) 등에서 이메일만 변경할 때 사용 */
    public void changeEmail(String email) {
        this.email = email;
    }

    public void softDelete() {
        this.deleted = true;
    }

    /** 관리자 - 정지 해제/복구 (PATCH /admin/users/{id}/status). "정지"는 별도 플래그 없이 is_deleted를 그대로 재사용한다. */
    public void restore() {
        this.deleted = false;
    }

    /**
     * 포인트를 증감시킨다. amount는 양수(적립)/음수(차감) 모두 가능.
     * 차감 결과가 음수가 되는지는 호출하는 서비스(PointService)에서 미리 검증하고 불러야 한다.
     */
    public void adjustPoint(int amount) {
        this.point += amount;
    }

    /** 관리자가 입점 신청을 승인했을 때 일반 사용자를 판매자로 승격시킨다. */
    public void promoteToSeller() {
        this.role = Role.SELLER;
    }
}
