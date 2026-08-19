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
}
