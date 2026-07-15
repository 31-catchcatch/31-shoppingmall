package com.shoppingmall.domain.auth.repository;

import com.shoppingmall.domain.auth.entity.RefreshToken;
import com.shoppingmall.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    /** 전 기기 로그아웃(비밀번호 변경 등) 시 사용. */
    void deleteAllByUser(User user);
}
