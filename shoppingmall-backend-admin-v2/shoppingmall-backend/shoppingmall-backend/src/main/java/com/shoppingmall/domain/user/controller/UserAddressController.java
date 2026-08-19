package com.shoppingmall.domain.user.controller;

import com.shoppingmall.domain.user.dto.request.AddressRequest;
import com.shoppingmall.domain.user.dto.response.AddressResponse;
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

    // 1. 내 주소록 리스트 조회
    // [5-2 조치] 엔티티(Address)를 직접 반환하면 Address.user -> User.password(BCrypt 해시)까지
    //            직렬화되어 응답에 계정 정보가 노출된다. 응답 DTO 로 변환해서 내려준다.
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<AddressResponse> response =
                addressService.getMyAddresses(userDetails.getUser().getId()).stream()
                        .map(AddressResponse::from)
                        .toList();

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
        addressService.deleteAddress(userDetails.getUser().getId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("선택하신 배송지 정보가 정상 삭제되었습니다.", null));
    }
}