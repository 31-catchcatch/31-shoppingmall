package com.shoppingmall.domain.banner.dto.response;

import com.shoppingmall.domain.banner.entity.Banner;

public record BannerResponse(
        Long id,
        String title,
        String imageUrl,
        String linkUrl,
        Integer sortOrder
) {
    public static BannerResponse from(Banner banner) {
        return new BannerResponse(
                banner.getId(),
                banner.getTitle(),
                banner.getImageUrl(),
                banner.getLinkUrl(),
                banner.getSortOrder()
        );
    }
}
