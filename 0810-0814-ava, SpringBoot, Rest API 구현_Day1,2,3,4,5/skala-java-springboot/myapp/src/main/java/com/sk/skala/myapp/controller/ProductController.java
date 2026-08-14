package com.sk.skala.myapp.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sk.skala.myapp.domain.Product;
import com.sk.skala.myapp.domain.ProductStatus;
import com.sk.skala.myapp.dto.ProductRequest;
import com.sk.skala.myapp.dto.ProductResponse;
import com.sk.skala.myapp.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ProductRequest -> Product 변환
    private Product toEntity(ProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setStatus(request.status());
        product.setDescription(request.description());
        return product;
    }

    // GET: 전체 조회
    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts().stream().map(ProductResponse::from).toList();
    }

    // GET: 단건 조회
    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return productService.getProductById(id).map(ProductResponse::from).orElse(null);
    }

    // GET: 상태별 조회
    @GetMapping("/status")
    public List<ProductResponse> getProductsByStatus(@RequestParam("value") ProductStatus status) {
        return productService.getProductsByStatus(status).stream().map(ProductResponse::from).toList();
    }

    // POST: 등록
    @PostMapping
    public ProductResponse createProduct(@RequestBody @Valid ProductRequest request) {
        Product product = productService.createProduct(toEntity(request));
        return ProductResponse.from(product);
    }

    // PUT: 수정
    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @RequestBody @Valid ProductRequest request) {
        return productService.updateProduct(id, toEntity(request)).map(ProductResponse::from).orElse(null);
    }

    // DELETE: 삭제
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
}
