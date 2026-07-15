package com.shoppingmall.domain.user.repository;

import com.shoppingmall.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndDeletedFalse(String username);

    Optional<User> findByNameAndEmailAndDeletedFalse(String name, String email);

    Optional<User> findByUsernameAndEmailAndDeletedFalse(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
