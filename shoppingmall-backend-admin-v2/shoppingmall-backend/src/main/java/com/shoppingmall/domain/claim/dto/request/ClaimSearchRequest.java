package com.shoppingmall.domain.claim.dto.request;

import com.shoppingmall.domain.claim.entity.ClaimStatus;
import com.shoppingmall.domain.claim.entity.ClaimType;

/**
 * 클레임 목록 검색 조건 DTO
 */
public record ClaimSearchRequest(

        ClaimType type,
        ClaimStatus status,
        Integer page,
        Integer size

) {
}