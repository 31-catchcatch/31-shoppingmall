package com.shoppingmall.domain.banner.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DB 정의서 'banners' 테이블 매핑.
 * GET /api/v1/banners - 메인 페이지 노출용 이벤트 배너 목록 조회 대응.
 */
@Getter
@Entity
@Table(name = "banners")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    @Column(name = "link_url", length = 512)
    private String linkUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Banner(String title, String imageUrl, String linkUrl, Integer sortOrder,
                  boolean active, LocalDateTime startAt, LocalDateTime endAt) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
        this.active = active;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
    public void update(String title, String imageUrl, String linkUrl, Integer sortOrder,
                       boolean active, LocalDateTime startAt, LocalDateTime endAt) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
        this.active = active;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    /** 노출 순서 재정렬. int / Integer 양쪽 호출을 모두 받는다. */
    public void changeSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }
}
