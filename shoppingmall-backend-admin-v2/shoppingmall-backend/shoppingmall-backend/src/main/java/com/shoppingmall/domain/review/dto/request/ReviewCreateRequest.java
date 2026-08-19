package com.shoppingmall.domain.review.dto.request;

import com.shoppingmall.global.validation.NoHtml;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewCreateRequest {

    /**
     * 대상 상품 ID.
     * - POST /reviews : 바디로 직접 받음
     * - POST /products/{productId}/reviews : 경로 값을 컨트롤러가 주입 (바디 생략 가능)
     * 두 경로를 모두 지원해야 해서 @NotNull은 제거하고 서비스에서 null 검증한다.
     */
    private Long productId;

    @NotNull(message = "주문 세부 내역(OrderDetail) ID는 필수입니다.")
    private Long orderDetailId; // 구매 확정 유효성 검증용 식별값

    @Min(value = 1, message = "최소 별점은 1점입니다.")
    @Max(value = 5, message = "최대 별점은 5점입니다.")
    private int rating;

    @NotBlank(message = "리뷰 내용은 필수 입력 사항입니다.")
    @Size(max = 2000, message = "리뷰는 2,000자 이하여야 합니다.")   // [1-6]
    @NoHtml                                                        // [1-1]
    private String content;

    // 포토 리뷰 이미지 (선택)
    @Size(max = 512, message = "이미지 URL은 512자 이하여야 합니다.")                    // [1-6]
    @Pattern(regexp = "^$|^(/uploads/|https?://)[\\w\\-./%]*$",                       // [1-1]
             message = "허용되지 않은 이미지 경로입니다.")
    private String imageUrl;

    /** 경로 기반 등록(POST /products/{id}/reviews)에서 경로 값을 우선 적용하기 위한 주입 메서드 */
    public void applyProductId(Long productId) {
        this.productId = productId;
    }
}