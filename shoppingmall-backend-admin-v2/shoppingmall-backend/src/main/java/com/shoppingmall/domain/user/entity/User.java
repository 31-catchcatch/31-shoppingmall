package com.shoppingmall.domain.user.entity;

import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 판매자 마이페이지(PUT /seller/me) 등에서 이메일만 변경할 때 사용 */
    public void changeEmail(String email) {
        this.email = email;
    }

    public void softDelete() {
        this.deleted = true;
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
