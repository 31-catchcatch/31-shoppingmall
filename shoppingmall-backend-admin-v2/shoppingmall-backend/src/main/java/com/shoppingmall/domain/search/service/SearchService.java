package com.shoppingmall.domain.search.service;

import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * API 명세서 "공통/인증 - 검색 - 검색어 자동완성".
 * 지금은 상품명 앞부분 일치(LIKE 'keyword%')로 단순 구현.
 * 데이터가 많아지고 응답속도가 문제되면 Elasticsearch 등으로 교체 고려.
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int MAX_SUGGESTIONS = 10;

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<String> autocomplete(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        // [1-2 조치] LIKE 와일드카드를 리터럴로 처리한다 (JPQL 의 escape '!' 와 짝)
        return productRepository.findTop10NamesStartingWith(
                ProductService.escapeLike(keyword.trim()), PageRequest.of(0, MAX_SUGGESTIONS));
    }
}
