# JPA 어노테이션 실습 — Product 도메인 구현하기

---

## 1. 학습 목표

- `@Table`, `@Id`, `@GeneratedValue`, `@Column` 으로 엔티티와 DB 테이블을 매핑할 수 있다
- `@Enumerated` 를 사용해 Enum 타입을 DB에 문자열로 저장할 수 있다
- `@Lob` 으로 대용량 텍스트 컬럼을 매핑할 수 있다
- `@Transient` 로 DB에 저장하지 않는 계산 필드를 만들 수 있다
- `JpaRepository` 를 상속해 CRUD 메서드를 사용할 수 있다

---

## 2. 작성 파일 목록 및 설명

| 파일 | 패키지 | 역할 |
|------|--------|------|
| `ProductStatus.java` | `domain` | 상품 상태를 표현하는 Enum (판매중, 품절, 단종) |
| `Product.java` | `domain` | JPA 어노테이션이 적용된 엔티티 |
| `ProductRepository.java` | `repository` | JpaRepository 상속 인터페이스 |
| `ProductRequest.java` | `dto` | 클라이언트 요청 DTO |
| `ProductResponse.java` | `dto` | 클라이언트 응답 DTO |
| `ProductService.java` | `service` | 비즈니스 로직 |
| `ProductController.java` | `controller` | REST API 엔드포인트 |
| `data.sql` | `resources` | 애플리케이션 시작 시 자동으로 실행되는 초기 데이터 |

---

## 3. 작성 파일별 요구 사항 및 힌트


### 3-1. `ProductStatus.java` — Enum

**요구 사항**
- `ON_SALE`(판매중), `SOLD_OUT`(품절), `DISCONTINUED`(단종) 세 가지 상태를 표현한다

**작성할 코드**

```java
package com.sk.skala.myapp.domain;

public enum ProductStatus {
    // TODO: 세 가지 상태 상수를 선언하세요
}
```

---

### 3-2. `Product.java` — Entity

**요구 사항**

| 필드 | 타입 | 적용 어노테이션 | 조건 |
|------|------|----------------|------|
| `id` | `Long` | `@Id` `@GeneratedValue` | AUTO_INCREMENT |
| `name` | `String` | `@Column` | DB 컬럼명 `product_name`, NOT NULL, 최대 100자 |
| `price` | `Integer` | `@Column` | NOT NULL |
| `stockQuantity` | `Integer` | `@Column` | DB 기본값 0 |
| `status` | `ProductStatus` | `@Enumerated` | 문자열("ON_SALE")로 DB에 저장, NOT NULL |
| `description` | `String` | `@Lob` | 대용량 텍스트 |
| `displayLabel` | `String` | `@Transient` | DB에 저장하지 않음 |

**작성할 코드**

```java
package com.sk.skala.myapp.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = /* TODO: 테이블명을 "products"로 지정 */)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = /* TODO: AUTO_INCREMENT 전략 */)
    private Long id;

    @Column(name = /* TODO: 컬럼명 "product_name" */,
            nullable = /* TODO: NOT NULL */,
            length  = /* TODO: 최대 100자 */)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer stockQuantity;

    /* TODO: Enum을 문자열로 저장하는 어노테이션 */
    @Column(nullable = false)
    private ProductStatus status;

    /* TODO: 대용량 텍스트 어노테이션 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /* TODO: DB에 저장하지 않는 어노테이션 */
    private String displayLabel;

    // @Transient 필드는 DB에서 읽어오지 않으므로 직접 계산
    public String getDisplayLabel() {
        // TODO: "상품명 (상태)" 형태의 문자열을 반환하세요
        // 예) "노트북 (ON_SALE)"
        return null;
    }
}
```

>  힌트
> - `@GeneratedValue(strategy = GenerationType.???)` — IDENTITY 전략은 DB의 AUTO_INCREMENT에 위임합니다
> - `@Enumerated(EnumType.???)` — STRING을 선택하면 숫자(0,1)가 아닌 이름 문자열이 저장됩니다
> - `@Lob` 은 필드 선언 위에 단독으로 붙입니다
> - `@Transient` 는 JPA가 해당 필드를 완전히 무시합니다

---

### 3-3. `ProductRepository.java` — Repository

**요구 사항**
- `JpaRepository<Product, Long>` 을 상속한다
- 상태(status)로 상품 목록을 조회하는 쿼리 메서드를 선언한다

**작성할 코드**

```java
package com.sk.skala.myapp.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sk.skala.myapp.domain.Product;
import com.sk.skala.myapp.domain.ProductStatus;

public interface ProductRepository extends /* TODO: JpaRepository 상속 */ {

    // TODO: ProductStatus로 조회하는 쿼리 메서드를 선언하세요
    //       Spring Data JPA 명명 규칙: findBy필드명(타입 파라미터)
}
```

> 힌트: `findBy` + 필드명의 첫 글자를 대문자로 쓰면 WHERE 조건이 자동 생성됩니다.

---

### 3-4. `ProductRequest.java` — 요청 DTO

**요구 사항**
- `name`, `price`, `stockQuantity`, `status`, `description` 필드를 가진다
- `name` 은 빈 값 불가, `price`·`status` 는 null 불가, `price`·`stockQuantity` 는 0 이상이어야 한다

**작성할 코드**

```java
package com.sk.skala.myapp.dto;

import com.sk.skala.myapp.domain.ProductStatus;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "상품명은 필수입니다")
    private String name;

    /* TODO: null 불가, 0 이상 제약을 추가하세요 */
    private Integer price;

    /* TODO: 0 이상 제약을 추가하세요 */
    private Integer stockQuantity;

    /* TODO: null 불가 제약을 추가하세요 */
    private ProductStatus status;

    private String description;
}
```

> 힌트: `@NotNull`, `@Min(value = 0)` 을 조합하세요.

---

### 3-5. `ProductResponse.java` — 응답 DTO

**요구 사항**
- Entity의 모든 필드와 `displayLabel`(@Transient 값)을 포함한다

**작성할 코드**

```java
package com.sk.skala.myapp.dto;

import com.sk.skala.myapp.domain.ProductStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    // TODO: id, name, price, stockQuantity, status, description, displayLabel 필드를 선언하세요
}
```

---

### 3-6. `ProductService.java` — Service

**요구 사항**
- 전체 조회, 단건 조회, 상태별 조회, 등록, 수정, 삭제 메서드를 구현한다

**작성할 코드**

```java
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

    // TODO: 생성자 주입으로 ProductRepository를 받으세요

    // 전체 조회
    public List<Product> getAllProducts() {
        // TODO
    }

    // 단건 조회
    public Optional<Product> getProductById(Long id) {
        // TODO
    }

    // 상태별 조회
    public List<Product> getProductsByStatus(ProductStatus status) {
        // TODO: ProductRepository의 쿼리 메서드를 호출하세요
    }

    // 등록
    public Product createProduct(Product product) {
        // TODO
    }

    // 수정
    public Optional<Product> updateProduct(Long id, Product updated) {
        // TODO: findById로 조회 후, 존재하면 필드를 교체하고 save하세요
        //       존재하지 않으면 Optional.empty()를 반환하세요
    }

    // 삭제
    public void deleteProduct(Long id) {
        // TODO
    }
}
```

> 힌트: `findById(id).map(product -> { ... return repository.save(product); })` 패턴을 활용하세요.

---

### 3-7. `ProductController.java` — Controller

**요구 사항**
- 기본 경로는 `/api/products`
- ProductRequest → Product 변환, Product → ProductResponse 변환 헬퍼 메서드를 만든다
- 아래 6개 엔드포인트를 구현한다

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/products` | 전체 조회 |
| GET | `/api/products/{id}` | 단건 조회 |
| GET | `/api/products/status?value=ON_SALE` | 상태별 조회 |
| POST | `/api/products` | 등록 |
| PUT | `/api/products/{id}` | 수정 |
| DELETE | `/api/products/{id}` | 삭제 |

**작성할 코드**

```java
package com.sk.skala.myapp.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.sk.skala.myapp.domain.Product;
import com.sk.skala.myapp.domain.ProductStatus;
import com.sk.skala.myapp.dto.ProductRequest;
import com.sk.skala.myapp.dto.ProductResponse;
import com.sk.skala.myapp.service.ProductService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    // TODO: 생성자 주입

    // TODO: ProductRequest → Product 변환 헬퍼 메서드
    private Product toEntity(ProductRequest request) { ... }

    // TODO: Product → ProductResponse 변환 헬퍼 메서드
    //       displayLabel은 product.getDisplayLabel()로 가져오세요
    private ProductResponse toResponse(Product product) { ... }

    // TODO: 전체 조회 GET /api/products

    // TODO: 단건 조회 GET /api/products/{id}

    // TODO: 상태별 조회 GET /api/products/status?value=ON_SALE
    //       @RequestParam ProductStatus value 로 파라미터를 받으세요

    // TODO: 등록 POST /api/products  (@Valid 붙이기)

    // TODO: 수정 PUT /api/products/{id}  (@Valid 붙이기)

    // TODO: 삭제 DELETE /api/products/{id}
}
```

---

### 3-8. `data.sql` — 초기 데이터

**요구 사항**
- 애플리케이션 시작 시 `PRODUCTS` 테이블에 샘플 데이터 5건을 자동으로 삽입한다
- `@Column(name="product_name")` 설정에 맞게 컬럼명을 `product_name`으로 작성한다
- `@Enumerated(EnumType.STRING)` 설정에 맞게 status 값을 문자열로 작성한다

**작성할 코드** (`src/main/resources/data.sql`)

```sql
-- 기존 users 초기 데이터 아래에 추가
INSERT INTO products (product_name, price, stock_quantity, status, description)
  VALUES ('노트북',      1500000, 10, 'ON_SALE',      '고성능 개발용 노트북입니다.');

INSERT INTO products (product_name, price, stock_quantity, status, description)
  VALUES ('무선 마우스',   35000, 50, 'ON_SALE',      '2.4GHz 무선 마우스입니다.');

INSERT INTO products (product_name, price, stock_quantity, status, description)
  VALUES ('기계식 키보드', 120000,  0, 'SOLD_OUT',    '청축 기계식 키보드입니다.');

INSERT INTO products (product_name, price, stock_quantity, status, description)
  VALUES ('27인치 모니터',350000,  5, 'ON_SALE',      'QHD 해상도 모니터입니다.');

INSERT INTO products (product_name, price, stock_quantity, status, description)
  VALUES ('USB 허브',     25000,  0, 'DISCONTINUED', '단종된 4포트 USB 허브입니다.');
```

> 힌트
> - `data.sql`이 동작하려면 `application-local.yaml`에 아래 두 설정이 모두 필요합니다
>   ```yaml
>   spring:
>     jpa:
>       defer-datasource-initialization: true  # Hibernate 테이블 생성 후 data.sql 실행
>     sql:
>       init:
>         mode: always                          # 내장 DB가 아니어도 항상 실행
>   ```
> - `defer-datasource-initialization: true` 가 없으면 테이블이 만들어지기 전에 INSERT가 실행되어 오류가 발생합니다

---

## 4. 실행 및 결과 확인

### 애플리케이션 실행

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

---

### API 동작 확인 (curl)

**상품 등록**
```bash
curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "노트북",
    "price": 1500000,
    "stockQuantity": 10,
    "status": "ON_SALE",
    "description": "고성능 노트북입니다."
  }' | jq .
```

**전체 조회**
```bash
curl -s http://localhost:8080/api/products | jq .
```

**상태별 조회**
```bash
curl -s "http://localhost:8080/api/products/status?value=ON_SALE" | jq .
```

**수정**
```bash
curl -s -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "노트북",
    "price": 1200000,
    "stockQuantity": 0,
    "status": "SOLD_OUT",
    "description": "품절된 노트북입니다."
  }' | jq .
```

**삭제**
```bash
curl -s -X DELETE http://localhost:8080/api/products/1
```

---

### 응답 예시

등록 성공 시 아래와 같이 `displayLabel` 필드가 포함되어야 합니다.
`displayLabel`은 DB에 저장되지 않고(`@Transient`) 런타임에 계산된 값입니다.

```json
{
  "id": 1,
  "name": "노트북",
  "price": 1500000,
  "stockQuantity": 10,
  "status": "ON_SALE",
  "description": "고성능 노트북입니다.",
  "displayLabel": "노트북 (ON_SALE)"
}
```
