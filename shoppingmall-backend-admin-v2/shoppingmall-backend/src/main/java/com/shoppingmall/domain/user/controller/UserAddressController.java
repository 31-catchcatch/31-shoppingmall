package com.shoppingmall.domain.user.controller;

import com.shoppingmall.domain.user.dto.request.AddressRequest;
import com.shoppingmall.domain.user.entity.Address;
import com.shoppingmall.domain.user.service.AddressService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final AddressService addressService;

    // 1. 내 주소록 리스트 조회 (성공 시 데이터 List<Address> 반환)
    @GetMapping
    public ResponseEntity<ApiResponse<List<Address>>> getAddresses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Address> response = addressService.getMyAddresses(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("주소록 목록 조회가 완료되었습니다.", response));
    }

    // 2. 신규 배송지 추가 (성공 시 Void 반환하므로 두 번째 인자에 null 기입)
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddressRequest request) {
        addressService.createAddress(userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("배송지 정보가 추가되었습니다.", null));
    }

    // 3. 특정 주소 수정 (성공 시 Void 반환하므로 두 번째 인자에 null 기입)
    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> updateAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        addressService.updateAddress(userDetails.getUser().getId(), addressId, request);
        return ResponseEntity.ok(ApiResponse.success("배송지 주소가 수정되었습니다.", null));
    }

    // 4. 특정 배송지 삭제 (성공 시 Void 반환하므로 두 번째 인자에 null 기입)
    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long addressId) {
        addressService.deleteAddress(addressId);
        return ResponseEntity.ok(ApiResponse.success("선택하신 배송지 정보가 정상 삭제되었습니다.", null));
    }
}