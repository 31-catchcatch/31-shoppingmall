package com.shoppingmall.domain.product.service;

import com.shoppingmall.domain.product.dto.response.CategoryResponse;
import com.shoppingmall.domain.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        return categoryRepository.findByParentIsNullOrderById().stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
