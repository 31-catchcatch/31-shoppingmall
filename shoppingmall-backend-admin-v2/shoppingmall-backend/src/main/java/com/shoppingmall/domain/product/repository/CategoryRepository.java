package com.shoppingmall.domain.product.repository;

import com.shoppingmall.domain.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** 메인/전체 메뉴용 카테고리 트리의 최상위 노드 목록. children은 지연로딩으로 따라옴. */
    List<Category> findByParentIsNullOrderById();
}
