package com.shoppingmall.domain.user.service;

import com.shoppingmall.domain.user.dto.request.AddressRequest;
import com.shoppingmall.domain.user.entity.Address;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.AddressRepository;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    // 내 주소록 리스트 전체 파싱
    public List<Address> getMyAddresses(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return addressRepository.findAllByUser(user);
    }

    // 주소록 신규 등록
    @Transactional
    public void createAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // [비즈니스 규칙] 새 주소를 기본 배송지로 지정할 시 기존 기본 주소를 먼저 false로 토글 차단
        if (request.isDefaultAddress()) {
            addressRepository.findByUserAndDefaultAddressTrue(user)
                    .ifPresent(existing -> existing.updateDefaultStatus(false));
        }

        Address address = Address.builder()
                .user(user)
                .addressName(request.getAddressName())
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .baseAddress(request.getBaseAddress())
                .detailAddress(request.getDetailAddress())
                .defaultAddress(request.isDefaultAddress())
                .build();

        addressRepository.save(address);
    }

    // 특정 주소 정보 덮어쓰기 수정 (PUT 표준 규칙 대응)
    @Transactional
    public void updateAddress(Long userId, Long addressId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADDRESS_NOT_FOUND));

        if (request.isDefaultAddress()) {
            addressRepository.findByUserAndDefaultAddressTrue(user)
                    .ifPresent(existing -> existing.updateDefaultStatus(false));
        }

        // JPA 영속성 변경 메서드 호출을 통한 상태 제어 (Update 실행)
        // 엔티티 내에 setter가 없으므로 Address.java에 정보 수정용 메서드를 구현하거나 재생성 처리를 진행합니다.
    }

    // 주소 삭제 (DELETE)
    @Transactional
    public void deleteAddress(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADDRESS_NOT_FOUND));
        addressRepository.delete(address);
    }
}