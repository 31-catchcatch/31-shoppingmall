package com.shoppingmall.domain.product.repository;

import com.shoppingmall.domain.product.entity.Brand;
import com.shoppingmall.domain.product.entity.BrandLike;
import com.shoppingmall.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrandLikeRepository extends JpaRepository<BrandLike, Long> {

    Optional<BrandLike> findByUserAndBrand(User user, Brand brand);
}
