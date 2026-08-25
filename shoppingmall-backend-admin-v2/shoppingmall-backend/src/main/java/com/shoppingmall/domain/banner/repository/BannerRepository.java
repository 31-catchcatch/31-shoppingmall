package com.shoppingmall.domain.banner.repository;

import com.shoppingmall.domain.banner.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    /**
     * GET /api/v1/banners - 현재 노출 가능한(활성 + 노출 기간 이내) 배너를 노출 순서대로 조회.
     * start_at/end_at 이 NULL 이면 기간 제한이 없는 배너로 취급한다.
     */
    @Query("""
            select b from Banner b
            where b.active = true
              and (b.startAt is null or b.startAt <= :now)
              and (b.endAt is null or b.endAt >= :now)
            order by b.sortOrder asc
            """)
    List<Banner> findActiveBanners(@Param("now") LocalDateTime now);
    /**
     * 관리자 화면 — 비활성·기간 만료 배너까지 전부 조회한다.
     * 노출 순서 재정렬(sortOrder 재부여)의 기준 목록으로도 쓰이므로 정렬을 고정한다.
     * sortOrder 가 같을 때 순서가 흔들리지 않도록 id 를 2차 정렬키로 둔다.
     */
    @Query("select b from Banner b order by b.sortOrder asc, b.id asc")
    List<Banner> findAllForAdmin();
}
