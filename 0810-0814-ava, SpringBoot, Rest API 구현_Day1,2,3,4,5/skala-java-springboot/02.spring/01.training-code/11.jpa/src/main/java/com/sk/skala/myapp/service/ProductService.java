package com.sk.skala.myapp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sk.skala.myapp.domain.Product;
import com.sk.skala.myapp.domain.ProductStatus;
import com.sk.skala.myapp.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 전체 상품 조회
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // 상품 단건 조회
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    // 상태별 상품 조회 (@Enumerated 실습)
    public List<Product> getProductsByStatus(ProductStatus status) {
        return productRepository.findByStatus(status);
    }

    // 상품 등록
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    // 상품 수정
    public Optional<Product> updateProduct(Long id, Product updated) {
        return productRepository.findById(id).map(product -> {
            product.setName(updated.getName());
            product.setPrice(updated.getPrice());
            product.setStockQuantity(updated.getStockQuantity());
            product.setStatus(updated.getStatus());
            product.setDescription(updated.getDescription());
            return productRepository.save(product);
        });
    }

    // 상품 삭제
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
