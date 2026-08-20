package com.shoppingmall.domain.admin.service;

import com.shoppingmall.domain.admin.dto.request.AdminBannerRequest;
import com.shoppingmall.domain.admin.dto.request.BannerOrderUpdateRequest;
import com.shoppingmall.domain.admin.dto.response.AdminBannerResponse;
import com.shoppingmall.domain.banner.entity.Banner;
import com.shoppingmall.domain.banner.repository.BannerRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * API 명세서 "관리자 - 운영 - 배너" 담당.
 * - GET    /admin/banners
 * - POST   /admin/banners
 * - PUT    /admin/banners/{bannerId}
 * - DELETE /admin/banners/{bannerId}
 * - PATCH  /admin/banners/order
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBannerService {

    private final BannerRepository bannerRepository;

    /** 중지·기간만료 배너까지 전부 반환한다. 공개용 GET /banners 와 달리 필터링하지 않는다. */
    public List<AdminBannerResponse> getBanners() {
        return bannerRepository.findAllForAdmin().stream()
                .map(AdminBannerResponse::from)
                .toList();
    }

    @Transactional
    public AdminBannerResponse createBanner(AdminBannerRequest request) {
        validate(request);

        Banner banner = Banner.builder()
                .title(request.title().trim())
                .imageUrl(request.imageUrl().trim())
                .linkUrl(emptyToNull(request.linkUrl()))
                .sortOrder(request.sortOrder())
                .active(Boolean.TRUE.equals(request.active()))
                .startAt(request.startAt())
                .endAt(request.endAt())
                .build();

        return AdminBannerResponse.from(bannerRepository.save(banner));
    }

    @Transactional
    public AdminBannerResponse updateBanner(Long bannerId, AdminBannerRequest request) {
        validate(request);

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new CustomException(ErrorCode.BANNER_NOT_FOUND));

        banner.update(
                request.title().trim(),
                request.imageUrl().trim(),
                emptyToNull(request.linkUrl()),
                request.sortOrder(),
                Boolean.TRUE.equals(request.active()),
                request.startAt(),
                request.endAt()
        );

        return AdminBannerResponse.from(banner);
    }

    /** 하드 삭제. banners 에는 소프트 삭제 컬럼이 없어 되돌릴 수 없다. */
    @Transactional
    public void deleteBanner(Long bannerId) {
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new CustomException(ErrorCode.BANNER_NOT_FOUND));

        bannerRepository.delete(banner);
        bannerRepository.flush();   // 아래 재정렬이 삭제된 배너를 다시 집지 않도록 먼저 반영한다

        // 중간 배너를 지우면 순서에 구멍이 생긴다(1,3,4). 같은 트랜잭션에서 1..n 으로 다시 매긴다.
        resequence();
    }

    /**
     * 남은 배너의 노출 순서를 1..n 으로 압축한다. 정렬 기준은 조회 쿼리와 동일한 (sortOrder, id).
     * 더티 체킹으로 반영되므로 save 호출은 필요 없다.
     */
    private void resequence() {
        List<Banner> banners = bannerRepository.findAllForAdmin();
        int order = 1;
        for (Banner banner : banners) {
            banner.changeSortOrder(order++);
        }
    }

    /**
     * 노출 순서 일괄 변경. 하나라도 없는 배너를 가리키면 전체를 롤백한다
     * (일부만 반영되면 두 배너가 같은 순서를 갖고 조용히 깨진다).
     */
    @Transactional
    public List<AdminBannerResponse> updateOrder(BannerOrderUpdateRequest request) {
        List<Long> ids = request.items().stream()
                .map(BannerOrderUpdateRequest.Item::id)
                .toList();

        Map<Long, Banner> banners = bannerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Banner::getId, Function.identity()));

        for (BannerOrderUpdateRequest.Item item : request.items()) {
            Banner banner = banners.get(item.id());
            if (banner == null) {
                throw new CustomException(ErrorCode.BANNER_NOT_FOUND);
            }
            banner.changeSortOrder(item.sortOrder());
        }

        return getBanners();
    }

    // ---------------------------------------------------------

    private void validate(AdminBannerRequest request) {
        assertSafeUrl(request.imageUrl());
        assertSafeUrl(request.linkUrl());

        // 시각은 둘 다 타임존 없는 LocalDateTime 이라 그대로 비교하면 된다.
        if (request.startAt() != null && request.endAt() != null
                && !request.endAt().isAfter(request.startAt())) {
            throw new CustomException(ErrorCode.INVALID_BANNER_PERIOD);
        }
    }

    /**
     * [1-1 조치 연장] 배너 URL 은 화면에서 img src / a href 로 그대로 쓰인다.
     * javascript:, data:, vbscript: 같은 스킴이 저장되면 관리자 화면과 메인 페이지 양쪽에서
     * 클릭 유도형 스크립트 실행 통로가 되므로, 저장 시점에 막는다.
     *
     * <p>프론트(admin-banners.js)도 같은 규칙으로 거르지만 그건 우회 가능한 편의 검증이고,
     * 실제 방어선은 여기다.
     *
     * <p>허용: "/uploads/..." 같은 절대경로, "http://", "https://"
     * 차단: 그 외 모든 스킴(콜론 앞에 스킴 문자열이 오는 형태), "//evil.com" 같은 프로토콜 상대 URL
     */
    private void assertSafeUrl(String value) {
        if (value == null || value.isBlank()) return;   // linkUrl 은 선택값

        String url = value.trim();
        String lower = url.toLowerCase(Locale.ROOT);

        if (lower.startsWith("http://") || lower.startsWith("https://")) return;

        // "//evil.com/x" 는 브라우저가 현재 프로토콜로 외부 호스트를 부르는 형태라 상대경로로 취급하면 안 된다
        if (url.startsWith("//")) {
            throw new CustomException(ErrorCode.INVALID_BANNER_URL);
        }
        if (url.startsWith("/")) return;

        // 콜론이 스킴 구분자로 쓰였는지 본다. 경로 안의 콜론(첫 '/' 뒤)은 스킴이 아니다.
        int colon = url.indexOf(':');
        int slash = url.indexOf('/');
        if (colon >= 0 && (slash < 0 || colon < slash)) {
            throw new CustomException(ErrorCode.INVALID_BANNER_URL);
        }
    }

    private String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
