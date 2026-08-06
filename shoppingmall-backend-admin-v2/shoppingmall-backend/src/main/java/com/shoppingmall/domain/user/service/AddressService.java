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
                .zipCode(request.getZipCode())
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

        // 본인 소유 배송지만 수정 가능
        if (!address.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 이 배송지를 기본으로 지정하면, 기존 기본 배송지는 해제 (자기 자신 제외)
        if (request.isDefaultAddress()) {
            addressRepository.findByUserAndDefaultAddressTrue(user)
                    .filter(existing -> !existing.getId().equals(addressId))
                    .ifPresent(existing -> existing.updateDefaultStatus(false));
        }

        // 더티 체킹으로 UPDATE 실행
        address.update(
                request.getAddressName(),
                request.getRecipientName(),
                request.getRecipientPhone(),
                request.getBaseAddress(),
                request.getDetailAddress(),
                request.getZipCode(),
                request.isDefaultAddress()
        );
    }

    // 주소 삭제 (DELETE)
    @Transactional
    public void deleteAddress(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADDRESS_NOT_FOUND));
        addressRepository.delete(address);
    }
}