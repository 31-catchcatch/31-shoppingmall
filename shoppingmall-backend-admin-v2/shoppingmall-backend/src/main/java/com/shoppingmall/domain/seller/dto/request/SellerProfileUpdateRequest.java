package com.shoppingmall.domain.seller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PUT /api/v1/seller/me - 판매자 회원정보 수정 (프론트 seller-mypage-edit.js 대응).
 * 비밀번호 변경은 선택: currentPassword + newPassword 둘 다 있을 때만 처리.
 */
@Getter
@NoArgsConstructor
public class SellerProfileUpdateRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "상호명은 필수입니다.")
    private String businessName;

    @NotBlank(message = "대표자명은 필수입니다.")
    private String representativeName;

    @NotBlank(message = "담당자 연락처는 필수입니다.")
    private String managerPhone;

    private String businessAddress;

    // 비밀번호 변경 (선택)
    private String currentPassword;
    private String newPassword;
}
