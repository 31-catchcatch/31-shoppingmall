package com.shoppingmall.domain.activity.dto.response;

/** POST /api/v1/products/{productId}/like 응답 - 토글 후 최종 좋아요 상태 */
public record ProductLikeToggleResponse(
        boolean liked
) {
}
