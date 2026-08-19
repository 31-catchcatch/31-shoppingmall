package com.shoppingmall.domain.cart.service;

import com.shoppingmall.domain.cart.dto.request.CartAddItemRequest;
import com.shoppingmall.domain.cart.dto.request.CartItemUpdateRequest;
import com.shoppingmall.domain.cart.dto.response.CartItemResponse;
import com.shoppingmall.domain.cart.entity.CartItem;
import com.shoppingmall.domain.cart.repository.CartItemRepository;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductOption;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // 1. 내 장바구니 목록 가져오기 및 변환
    public List<CartItemResponse> getMyCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<CartItem> cartItems = cartItemRepository.findAllByUser(user);
        return cartItems.stream()
                .map(CartItemResponse::from)
                .collect(Collectors.toList());
    }

    // 2. 장바구니 물품 추가
    @Transactional
    public void addCartItem(Long userId, CartAddItemRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 상품 내부의 옵션 목록에서 요청온 옵션이 실제 존재하는지 찾아 매핑
        ProductOption productOption = product.getActiveOptions().stream()
                .filter(option -> option.getId().equals(request.getProductOptionId()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT)); // 매칭되는 올바른 옵션 에러 처리

        // 비즈니스 규칙: 이미 장바구니에 존재하는 상품+옵션이면 신규 등록이 아니라 수량만 늘린다.
        Optional<CartItem> existingItem = cartItemRepository
                .findByUserAndProductIdAndProductOptionId(user, product.getId(), productOption.getId());

        if (existingItem.isPresent()) {
            existingItem.get().addQuantity(request.getQuantity());
        } else {
            CartItem cartItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .productOption(productOption)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(cartItem);
        }
    }

    // 3. 수량 직접 편집
    @Transactional
    public void updateCartItem(Long userId, Long cartItemId, CartItemUpdateRequest request) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

        // 보안 장치: 악성 사용자가 타인의 cartItemId를 변조해 바꾸려는지 식별
        if (!cartItem.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        cartItem.updateQuantity(request.getQuantity());
    }

    // 4. 개별 품목 비우기
    @Transactional
    public void deleteCartItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

        if (!cartItem.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        cartItemRepository.delete(cartItem);
    }
}
