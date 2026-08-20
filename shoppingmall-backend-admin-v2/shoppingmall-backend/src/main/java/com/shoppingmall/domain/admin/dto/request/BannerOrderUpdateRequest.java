package com.shoppingmall.domain.admin.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 배너 노출 순서 일괄 변경. (PATCH /admin/banners/order)
 *
 * <p>순서 변경을 배너별 PUT 여러 번으로 처리하면 중간에 실패했을 때
 * 두 배너가 같은 sort_order 를 갖고 조용히 깨진다. 그래서 한 트랜잭션에서 처리한다.
 */
public record BannerOrderUpdateRequest(

        @NotEmpty(message = "변경할 배너 순서를 입력해 주세요.")
        @Valid
        List<Item> items

) {
    public record Item(
            @NotNull(message = "배너 ID가 필요합니다.")
            Long id,

            @NotNull(message = "노출 순서가 필요합니다.")
            @Positive(message = "노출 순서는 1 이상이어야 합니다.")
            Integer sortOrder
    ) {
    }
}
