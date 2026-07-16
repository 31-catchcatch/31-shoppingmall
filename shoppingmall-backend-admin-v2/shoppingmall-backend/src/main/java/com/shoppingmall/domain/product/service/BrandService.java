package com.shoppingmall.domain.product.service;

import com.shoppingmall.domain.activity.dto.response.ProductLikeToggleResponse;
import com.shoppingmall.domain.product.dto.response.BrandResponse;
import com.shoppingmall.domain.product.entity.Brand;
import com.shoppingmall.domain.product.entity.BrandLike;
import com.shoppingmall.domain.product.repository.BrandLikeRepository;
import com.shoppingmall.domain.product.repository.BrandRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandLikeRepository brandLikeRepository;
    private final UserRepository userRepository;

    /** GET /api/v1/brands - 노출 중인 전체 브랜드 목록 (이름순) */
    public List<BrandResponse> getBrands() {
        return brandRepository.findAll(Sort.by("name")).stream()
                .filter(Brand::isActive)
                .map(BrandResponse::from)
                .toList();
    }

    /** POST /api/v1/brands/{brandId}/like - 브랜드 즐겨찾기 토글 */
    @Transactional
    public ProductLikeToggleResponse toggleLike(Long userId, Long brandId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

        return brandLikeRepository.findByUserAndBrand(user, brand)
                .map(existing -> {
                    brandLikeRepository.delete(existing);
                    return new ProductLikeToggleResponse(false);
                })
                .orElseGet(() -> {
                    brandLikeRepository.save(BrandLike.builder().user(user).brand(brand).build());
                    return new ProductLikeToggleResponse(true);
                });
    }
}
