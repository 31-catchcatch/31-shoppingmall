package com.shoppingmall.domain.seller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 판매자 회원가입 요청 (S-AUTH-003)
 *
 * 일반 회원가입과 달리 "계정 생성"과 "입점 신청"을 한 번에 처리한다.
 * - users 에 role=SELLER 로 즉시 저장
 * - seller_applications 에 PENDING 으로 즉시 저장
 * 승인 전까지는 sellers 테이블에 행이 없으므로 판매자 로그인은 불가능하다.
 */
public record SellerSignupRequest(

        @NotBlank(message = "아이디를 입력해 주세요.")
        @Size(max = 50, message = "아이디는 50자 이하여야 합니다.")
        String username,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
        String name,

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100)
        String email,

        @NotBlank(message = "전화번호를 입력해 주세요.")
        @Pattern(regexp = "\\d{9,11}", message = "전화번호는 하이픈 없이 숫자만 입력해주세요.")
        String phoneNumber,

        @NotBlank(message = "상호명을 입력해 주세요.")
        @Size(max = 100, message = "상호명은 100자 이하여야 합니다.")
        String businessName,

        @NotBlank(message = "사업자등록번호를 입력해 주세요.")
        @Pattern(regexp = "^\\d{3}-?\\d{2}-?\\d{5}$", message = "사업자등록번호 형식이 올바르지 않습니다.")
        String businessRegistrationNumber,

        @NotBlank(message = "대표자명을 입력해 주세요.")
        @Size(max = 50, message = "대표자명은 50자 이하여야 합니다.")
        String representativeName,

        @NotBlank(message = "담당자 연락처를 입력해 주세요.")
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "연락처 형식이 올바르지 않습니다.")
        String contactNumber,

        @NotBlank(message = "사업장 주소를 입력해 주세요.")
        @Size(max = 255)
        String businessAddress,

        @NotBlank(message = "사업자등록증 파일 URL을 입력해 주세요.")
        @Size(max = 500)
        String businessRegistrationFileUrl,

        @Size(max = 500)
        String mailOrderReportFileUrl

) {
}
