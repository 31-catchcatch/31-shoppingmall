package com.shoppingmall.domain.user.repository;

import com.shoppingmall.domain.user.entity.Address;
import com.shoppingmall.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    // 특정 회원의 모든 등록 주소록 조회
    List<Address> findAllByUser(User user);

    // 기본 배송지로 등록된 주소 조회 (수정 시 이전 기본 배송지를 해제하기 위함)
    Optional<Address> findByUserAndDefaultAddressTrue(User user);
}