package com.shoppingmall.domain.product.dto.response;

import com.shoppingmall.domain.product.entity.Brand;

/** GET /api/v1/brands 목록의 개별 항목 */
public record BrandResponse(
        Long id,
        String name,
        String logoUrl
) {
    public static BrandResponse from(Brand brand) {
        return new BrandResponse(brand.getId(), brand.getName(), brand.getLogoUrl());
    }
}
