package com.shoppingmall.domain.seller.dto.request;

import com.shoppingmall.global.validation.NoHtml;
import com.shoppingmall.global.validation.ValidPassword;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

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
    @Size(max = 100)                                                        // [1-6]
    @NoHtml                                                                 // [1-1]
    private String businessName;

    @NotBlank(message = "대표자명은 필수입니다.")
    @Size(max = 50)                                                         // [1-6]
    @NoHtml                                                                 // [1-1]
    private String representativeName;

    @NotBlank(message = "담당자 연락처는 필수입니다.")
    @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",                     // [1-6]
             message = "연락처 형식이 올바르지 않습니다.")
    private String managerPhone;

    @Size(max = 255)                                                        // [1-6]
    @NoHtml                                                                 // [1-1]
    private String businessAddress;

    // 비밀번호 변경 (선택)
    // ⚠️ currentPassword 에는 @ValidPassword 를 부착하지 않는다 (기존 비밀번호 입력란)
    private String currentPassword;

    @ValidPassword                                                          // [3-1]
    private String newPassword;
}
