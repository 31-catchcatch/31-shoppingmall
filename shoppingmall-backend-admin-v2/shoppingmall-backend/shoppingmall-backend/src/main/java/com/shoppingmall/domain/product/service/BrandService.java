package com.shoppingmall.domain.product.service;

import com.shoppingmall.domain.product.dto.response.BrandResponse;
import com.shoppingmall.domain.product.entity.Brand;
import com.shoppingmall.domain.product.repository.BrandRepository;
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

    /** GET /api/v1/brands - 노출 중인 전체 브랜드 목록 (이름순) */
    public List<BrandResponse> getBrands() {
        return brandRepository.findAll(Sort.by("name")).stream()
                .filter(Brand::isActive)
                .map(BrandResponse::from)
                .toList();
    }
}
