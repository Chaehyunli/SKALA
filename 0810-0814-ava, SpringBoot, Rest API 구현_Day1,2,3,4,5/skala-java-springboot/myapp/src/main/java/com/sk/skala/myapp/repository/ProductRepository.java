package com.sk.skala.myapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sk.skala.myapp.domain.Product;
import com.sk.skala.myapp.domain.ProductStatus;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 상태별 상품 목록 조회 (쿼리 메서드)
    List<Product> findByStatus(ProductStatus status);

    List<Product> findByUserId(Long userId);

    List<Product> findByUserName(String userName);
}
