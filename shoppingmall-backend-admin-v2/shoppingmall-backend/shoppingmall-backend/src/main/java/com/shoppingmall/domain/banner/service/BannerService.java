package com.shoppingmall.domain.banner.service;

import com.shoppingmall.domain.banner.dto.response.BannerResponse;
import com.shoppingmall.domain.banner.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerService {

    private final BannerRepository bannerRepository;

    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findActiveBanners(LocalDateTime.now()).stream()
                .map(BannerResponse::from)
                .toList();
    }
}
