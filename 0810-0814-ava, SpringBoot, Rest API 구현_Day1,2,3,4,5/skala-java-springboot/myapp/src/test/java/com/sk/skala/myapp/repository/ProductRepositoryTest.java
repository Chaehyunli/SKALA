package com.sk.skala.myapp.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;

import com.sk.skala.myapp.domain.Product;
import com.sk.skala.myapp.domain.ProductStatus;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findByStatus_returns_only_matching_products() {
        Product soldOut = new Product();
        soldOut.setName("테스트 상품");
        soldOut.setPrice(1000);
        soldOut.setStockQuantity(0);
        soldOut.setStatus(ProductStatus.SOLD_OUT);
        productRepository.save(soldOut);

        assertThat(productRepository.findByStatus(ProductStatus.SOLD_OUT))
                .extracting(Product::getName)
                .contains("테스트 상품");
        assertThat(productRepository.findByStatus(ProductStatus.SOLD_OUT))
                .allMatch(p -> p.getStatus() == ProductStatus.SOLD_OUT);
    }
}
