package com.shoppingmall.domain.claim.dto.response;

import com.shoppingmall.domain.claim.entity.Claim;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 클레임 목록 응답 DTO
 */
public record ClaimListResponse(

        List<ClaimResponse> claims,
        int page,
        int size,
        long totalElements,
        int totalPages

) {

    /**
     * Claim Page 객체를 목록 응답 DTO로 변환한다.
     */
    public static ClaimListResponse from(
            Page<Claim> claimPage
    ) {
        return new ClaimListResponse(
                claimPage.getContent()
                        .stream()
                        .map(ClaimResponse::from)
                        .toList(),
                claimPage.getNumber(),
                claimPage.getSize(),
                claimPage.getTotalElements(),
                claimPage.getTotalPages()
        );
    }
}