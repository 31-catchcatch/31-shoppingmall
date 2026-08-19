package com.shoppingmall.domain.admin.dto.response;

import com.shoppingmall.domain.banner.entity.Banner;

import java.time.LocalDateTime;

/**
 * GET /admin/banners 응답.
 *
 * <p>공개용 {@link com.shoppingmall.domain.banner.dto.response.BannerResponse} 를 재사용하지 않고
 * 따로 둔다 — 공개 응답에 active/노출기간을 얹으면 비로그인 사용자에게도 운영 정보가 새어 나간다.
 *
 * <p>프론트(admin-banners.js)의 mapRow() 가 읽는 필드명에 맞췄다:
 * id, title, imageUrl, linkUrl, sortOrder, active, startAt, endAt
 */
public record AdminBannerResponse(

        Long id,
        String title,
        String imageUrl,
        String linkUrl,
        Integer sortOrder,
        boolean active,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime createdAt

) {

    public static AdminBannerResponse from(Banner banner) {
        return new AdminBannerResponse(
                banner.getId(),
                banner.getTitle(),
                banner.getImageUrl(),
                banner.getLinkUrl(),
                banner.getSortOrder(),
                banner.isActive(),
                banner.getStartAt(),
                banner.getEndAt(),
                banner.getCreatedAt()
        );
    }
}
