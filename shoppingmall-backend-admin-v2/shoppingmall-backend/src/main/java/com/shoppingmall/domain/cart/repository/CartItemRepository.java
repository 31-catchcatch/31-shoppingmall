package com.shoppingmall.domain.cart.repository;

import com.shoppingmall.domain.cart.entity.CartItem;
import com.shoppingmall.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 1. 현재 접속 유저의 장바구니에 등록된 전체 리스트 파싱 (Eager 로딩 최적화 대상)
    List<CartItem> findAllByUser(User user);

    // 2. 이미 해당 유저의 장바구니 목록에 완전히 똑같은 옵션의 상품이 있는지 확인
    Optional<CartItem> findByUserAndProductIdAndProductOptionId(User user, Long productId, Long productOptionId);
}