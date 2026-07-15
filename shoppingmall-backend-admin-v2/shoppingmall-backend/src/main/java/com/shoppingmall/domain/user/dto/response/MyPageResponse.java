package com.shoppingmall.domain.user.dto.response;

import com.shoppingmall.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class MyPageResponse {

    private final String username;
    private final String name;
    private final String email;
    private final String phoneNumber;
    private final Integer point;
    private final String role;

    @Builder
    public MyPageResponse(String username, String name, String email, String phoneNumber, Integer point, String role) {
        this.username = username;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.point = point;
        this.role = role;
    }

    // 엔티티 구조를 DTO 반환 규격으로 변환하는 매퍼 팩토리 메서드
    public static MyPageResponse from(User user) {
        return MyPageResponse.builder()
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .point(user.getPoint())
                .role(user.getRole().name())
                .build();
    }
}