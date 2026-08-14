package com.sk.skala.myapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sk.skala.myapp.domain.Product;
import com.sk.skala.myapp.domain.ProductStatus;
import com.sk.skala.myapp.dto.ProductRequest;
import com.sk.skala.myapp.service.ProductService;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductService productService;

    private Product sampleProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("노트북");
        product.setPrice(1500000);
        product.setStockQuantity(10);
        product.setStatus(ProductStatus.ON_SALE);
        product.setDescription("고성능 개발용 노트북입니다.");
        return product;
    }

    @Test
    void getAllProducts_returns_list() throws Exception {
        given(productService.getAllProducts()).willReturn(List.of(sampleProduct()));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("노트북"));
    }

    @Test
    void getProductById_returns_product() throws Exception {
        given(productService.getProductById(1L)).willReturn(Optional.of(sampleProduct()));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("노트북"));
    }

    @Test
    void getProductsByStatus_returns_filtered_list() throws Exception {
        given(productService.getProductsByStatus(ProductStatus.ON_SALE)).willReturn(List.of(sampleProduct()));

        mockMvc.perform(get("/api/products/status").param("value", "ON_SALE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ON_SALE"));
    }

    @Test
    void createProduct_returns_created_product() throws Exception {
        ProductRequest request = new ProductRequest("노트북", 1500000, 10, ProductStatus.ON_SALE, "고성능 개발용 노트북입니다.");
        given(productService.createProduct(any(Product.class))).willReturn(sampleProduct());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("노트북"));
    }

    @Test
    void createProduct_with_blank_name_returns_bad_request() throws Exception {
        ProductRequest request = new ProductRequest("", 1500000, 10, ProductStatus.ON_SALE, null);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProduct_returns_updated_product() throws Exception {
        ProductRequest request = new ProductRequest("노트북 Pro", 1800000, 5, ProductStatus.ON_SALE, "업그레이드된 노트북입니다.");
        Product updated = sampleProduct();
        updated.setName("노트북 Pro");
        given(productService.updateProduct(eq(1L), any(Product.class))).willReturn(Optional.of(updated));

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("노트북 Pro"));
    }

    @Test
    void deleteProduct_returns_ok() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isOk());
    }
}
