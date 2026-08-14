package com.sk.skala.myapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT
    private Long id;

    @Column(name = "product_name", nullable = false, length = 100)
    private String name; // 상품명 (최대 100자, NOT NULL)

    @Column(nullable = false)
    private Integer price; // 가격 (NOT NULL)

    @Column(name = "stock_quantity", columnDefinition = "INT DEFAULT 0")
    private Integer stockQuantity; // 재고 수량 (기본값 0)

    @Enumerated(EnumType.STRING) // DB에 "ON_SALE" 같은 문자열로 저장
    @Column(nullable = false)
    private ProductStatus status; // 상품 상태 (ON_SALE / SOLD_OUT / DISCONTINUED)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description; // 상품 상세 설명 (대용량 텍스트)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 이 상품을 등록한 사용자

    @Transient
    private String displayLabel; // "상품명 (상태)" 형태의 표시용 라벨, DB에 저장 안 됨

    public String getDisplayLabel() {
        return name + " (" + (status != null ? status.name() : "N/A") + ")";
    }
}
