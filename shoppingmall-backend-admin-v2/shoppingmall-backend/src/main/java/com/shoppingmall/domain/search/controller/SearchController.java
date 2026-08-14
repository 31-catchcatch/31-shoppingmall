package com.shoppingmall.domain.search.controller;

import com.shoppingmall.domain.search.service.SearchService;
import com.shoppingmall.global.common.ApiResponse;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/autocomplete")
    public ResponseEntity<ApiResponse<List<String>>> autocomplete(
            @RequestParam @Size(max = 50, message = "검색어는 50자 이하여야 합니다.") String keyword) {
        return ResponseEntity.ok(ApiResponse.success(searchService.autocomplete(keyword)));
    }
}
