package com.shoppingmall.domain.product.repository;

import com.shoppingmall.domain.product.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
}
