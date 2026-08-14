package com.sk.skala.myapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import com.sk.skala.myapp.domain.ProductStatus;

public record ProductRequest(
        @NotBlank(message = "상품명은 필수입니다") String name,

        @NotNull(message = "가격은 필수입니다")
        @PositiveOrZero(message = "가격은 0 이상이어야 합니다") Integer price,

        @NotNull(message = "재고 수량은 필수입니다")
        @PositiveOrZero(message = "재고 수량은 0 이상이어야 합니다") Integer stockQuantity,

        @NotNull(message = "상품 상태는 필수입니다") ProductStatus status,

        String description
) {
}
