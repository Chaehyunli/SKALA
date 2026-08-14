package com.sk.skala.myapp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sk.skala.myapp.domain.Product;
import com.sk.skala.myapp.domain.ProductStatus;
import com.sk.skala.myapp.domain.User;
import com.sk.skala.myapp.repository.ProductRepository;
import com.sk.skala.myapp.repository.UserRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // [전체 상품 목록 조회]
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // [상품 단건 조회]
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    // [상태별 상품 목록 조회]
    public List<Product> getProductsByStatus(ProductStatus status) {
        return productRepository.findByStatus(status);
    }

    public List<Product> getProductsByUserId(Long userId) {
        return productRepository.findByUserId(userId);
    }

    public List<Product> getProductsByUserName(String userName) {
        return productRepository.findByUserName(userName);
    }

    // [신규 상품 등록]
    public Product createProduct(Product product, Long userId) {
        product.setUser(findUser(userId));
        return productRepository.save(product);
    }

    // [기존 상품 정보 수정]
    public Optional<Product> updateProduct(Long id, Product updated, Long userId) {
        return productRepository.findById(id).map(product -> {
            product.setName(updated.getName());
            product.setPrice(updated.getPrice());
            product.setStockQuantity(updated.getStockQuantity());
            product.setStatus(updated.getStatus());
            product.setDescription(updated.getDescription());
            product.setUser(findUser(userId));
            return productRepository.save(product);
        });
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    // [상품 삭제]
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
