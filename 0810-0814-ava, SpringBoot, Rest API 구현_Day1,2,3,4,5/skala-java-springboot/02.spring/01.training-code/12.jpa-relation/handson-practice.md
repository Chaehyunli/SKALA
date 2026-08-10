# JPA @ManyToOne 연관관계 매핑 실습

---

## 1. 학습 목표

- `@ManyToOne` 어노테이션으로 두 엔티티 간 N:1 연관관계를 설정할 수 있다.
- `@JoinColumn`으로 FK 컬럼명을 지정하는 방법을 이해한다.
- 연관관계 엔티티를 DTO 변환 과정에서 올바르게 처리할 수 있다.
- Service 계층에서 연관 엔티티를 조회하고 연결하는 흐름을 구현할 수 있다.

---

## 2. 작성 파일 목록 및 설명

현재 `User`와 `Product`는 서로 독립적인 엔티티입니다.  
이번 실습에서는 **"상품은 한 명의 사용자에게 속한다"** 는 N:1 관계를 코드에 반영합니다.

```
Product (N) ─────────────► User (1)
  여러 상품이 한 명의 사용자에 속함
```

수정할 파일은 다음과 같습니다.

| 파일 | 역할 |
|------|------|
| `domain/Product.java` | `@ManyToOne`으로 User 참조 필드 추가 |
| `dto/ProductRequest.java` | 상품 등록/수정 시 사용자 ID와 이름을 받는 필드 추가 |
| `dto/ProductResponse.java` | 응답에 사용자 ID와 이름 포함 || `repository/ProductRepository.java` | 사용자 ID 및 이름으로 상품 검색 쿼리 메서드 추가 || `service/ProductService.java` | UserRepository 주입, User 연결 로직 추가 |
| `controller/ProductController.java` | DTO 변환 메서드에 User 정보 처리 추가 |
| `resources/data.sql` | 초기 데이터에 `user_id` 컬럼 추가 |

---

## 3. 파일별 요구 사항 및 작성 코드

---

### 3-1. `domain/Product.java`

**요구 사항**

`Product`가 `User`를 참조하도록 연관관계 필드를 추가합니다.

- `@ManyToOne`을 사용하고 fetch 전략은 `LAZY`로 설정합니다.
- `@JoinColumn`으로 FK 컬럼명을 `user_id`로 지정합니다.
- 필드명은 `user`이고 타입은 `User`입니다.

**작성 위치**

`@Transient private String displayLabel;` 바로 위에 아래 코드를 작성합니다.

```java
// [추가] 파일 상단 import 3개 추가
// - jakarta.persistence.FetchType
// - jakarta.persistence.JoinColumn
// - jakarta.persistence.ManyToOne

// [추가] @ManyToOne 연관관계 필드
// - fetch 전략을 LAZY로 지정하는 @ManyToOne 어노테이션을 작성한다
// - FK 컬럼명을 "user_id"로 지정하는 @JoinColumn 어노테이션을 작성한다
// - User 타입의 user 필드를 선언한다
```

---

### 3-2. `dto/ProductRequest.java`

**요구 사항**

API 요청 바디에서 사용자 ID와 이름을 받을 수 있도록 필드를 추가합니다.

- `userId` : 필드 타입 `Long`, 상품을 등록한 사용자 ID
- `userName` : 필드 타입 `String`, 사용자 이름으로 검색 시 사용 (조회용)

**작성 위치**

`private String description;` 아래에 추가합니다.

```java
// [추가] 상품을 등록한 사용자 ID 필드
// - Long 타입의 userId 필드를 선언한다

// [추가] 상품을 등록한 사용자 이름 필드
// - String 타입의 userName 필드를 선언한다
```

---

### 3-3. `dto/ProductResponse.java`

**요구 사항**

응답 DTO에 연관된 사용자 정보를 포함합니다.
- `userId` : 필드 타입 `Long`, 상품을 등록한 사용자 ID
- `userName` : 필드 타입 `String`

`private String displayLabel;` 아래에 두 필드를 추가합니다.

```java
// [추가] 등록한 사용자의 ID와 이름 필드
// - Long 타입의 userId 필드를 선언한다
// - String 타입의 userName 필드를 선언한다
```

---

### 3-4. `repository/ProductRepository.java`

**요구 사항**

`@ManyToOne`으로 연결된 User 필드를 활용해 두 가지 쿼리 메서드를 추가합니다.

- `findByUserId` : `user_id` FK 값으로 상품 목록을 조회합니다.
- `findByUserName` : 연관된 User의 `name` 필드 값으로 상품 목록을 조회합니다.

Spring Data JPA는 메서드 이름을 분석해 아래와 같이 자동으로 JOIN 쿼리를 생성합니다.

```
findBy  User  Id    → WHERE products.user_id = ?
findBy  User  Name  → JOIN users ON ... WHERE users.name = ?
```

**작성 위치**

`findByStatus()` 아래에 두 메서드를 추가합니다.

```java
// [추가] 사용자 ID로 상품 목록 조회
// - 파라미터: Long userId
// - 반환: List<Product>

// [추가] 사용자 이름으로 상품 목록 조회
// - 파라미터: String userName
// - 반환: List<Product>
// - User 엔티티의 name 필드를 탐색하는 메서드명 규칙을 따른다 (findByUser + Name)
```

---

### 3-5. `service/ProductService.java`

**요구 사항**

상품 등록과 수정 시 `userId`를 받아 `User` 엔티티를 조회하고 `Product`에 연결합니다.

**작성 위치 - import 및 필드**

```java
// [추가] import 2개
// - com.sk.skala.myapp.domain.User
// - com.sk.skala.myapp.repository.UserRepository

// [추가] UserRepository 필드를 선언하고 생성자에서 주입받는다
```

**작성 위치 - createProduct**

기존 `createProduct(Product product)` 메서드를 아래와 같이 수정합니다.

```java
// [수정] 파라미터에 Long userId를 추가한다
public Product createProduct(Product product, Long userId) {

    // [추가] userId가 null이 아닌 경우:
    //   1. userRepository.findById(userId)로 User를 조회한다
    //   2. 조회되지 않으면 IllegalArgumentException을 발생시킨다
    //   3. 조회된 user를 product.setUser()로 연결한다

    return productRepository.save(product);
}
```

**작성 위치 - updateProduct**

기존 `updateProduct(Long id, Product updated)` 메서드를 아래와 같이 수정합니다.

```java
// [수정] 파라미터에 Long userId를 추가한다
public Optional<Product> updateProduct(Long id, Product updated, Long userId) {
    return productRepository.findById(id).map(product -> {
        product.setName(updated.getName());
        product.setPrice(updated.getPrice());
        product.setStockQuantity(updated.getStockQuantity());
        product.setStatus(updated.getStatus());
        product.setDescription(updated.getDescription());

        // [추가] userId가 null이 아닌 경우:
        //   1. userRepository.findById(userId)로 User를 조회한다
        //   2. 조회되지 않으면 IllegalArgumentException을 발생시킨다
        //   3. 조회된 user를 product.setUser()로 연결한다

        return productRepository.save(product);
    });
}
```

**작성 위치 - 사용자 조회 메서드**

```java
// [추가] 사용자 ID로 상품 목록 조회 메서드
// - productRepository.findByUserId()를 호출한다

// [추가] 사용자 이름으로 상품 목록 조회 메서드
// - productRepository.findByUserName()을 호출한다
```

---

### 3-6. `controller/ProductController.java`

**API 엔드포인트 변경 목록**

| 메서드 | URL | 파라미터 | 변경 내용 |
|--------|-----|----------|----------|
| GET | `/api/products` | 없음 | 변경 없음 |
| GET | `/api/products/{id}` | `@PathVariable Long id` | 변경 없음 |
| GET | `/api/products/status` | `@RequestParam ProductStatus value` | 변경 없음 |
| GET | `/api/products/user/{userId}` | `@PathVariable Long userId` | **신규 추가** |
| GET | `/api/products/user` | `@RequestParam String name` | **신규 추가** |
| POST | `/api/products` | `@RequestBody ProductRequest` | `userId`, `userName` 필드 추가됨 |
| PUT | `/api/products/{id}` | `@PathVariable Long id`, `@RequestBody ProductRequest` | `userId`, `userName` 필드 추가됨 |
| DELETE | `/api/products/{id}` | `@PathVariable Long id` | 변경 없음 |

**요구 사항**

`toResponse()`와 서비스 호출 두 곳을 수정하고, 사용자 조회 엔드포인트 2개를 추가합니다.

**작성 위치 - toResponse**

```java
private ProductResponse toResponse(Product product) {
    // [추가] product.getUser()가 null이 아닌 경우 getId()로 userId를 추출한다
    // [추가] product.getUser()가 null이 아닌 경우 getName()으로 userName을 추출한다

    return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getPrice(),
            product.getStockQuantity(),
            product.getStatus(),
            product.getDescription(),
            product.getDisplayLabel(),
            // [추가] userId 전달
            // [추가] userName 전달
    );
}
```

**작성 위치 - createProduct**

```java
// [수정] productService.createProduct() 호출 시
//        두 번째 인자로 request.getUserId()를 전달한다
Product saved = productService.createProduct(toEntity(request), /* userId */);
```

**작성 위치 - updateProduct**

```java
// [수정] productService.updateProduct() 호출 시
//        세 번째 인자로 request.getUserId()를 전달한다
return productService.updateProduct(id, toEntity(request), /* userId */)
        .map(this::toResponse)
        .orElse(null);
```

**작성 위치 - 사용자 조회 엔드포인트**

```java
// [추가] GET /api/products/user/{userId} 엔드포인트
// - @PathVariable Long userId를 받아 productService.getProductsByUserId()를 호출한다
// - 결과를 stream()으로 toResponse() 변환 후 반환한다

// [추가] GET /api/products/user?name=홍길동 엔드포인트
// - @RequestParam String name을 받아 productService.getProductsByUserName()을 호출한다
// - 결과를 stream()으로 toResponse() 변환 후 반환한다
```

---

### 3-7. `resources/data.sql`

**요구 사항**

기존 `products` INSERT 문에 `user_id` 컬럼을 추가합니다.  
users 테이블에는 id=1(홍길동), id=2(김철수), id=3(이영희)가 이미 있습니다.

각 상품을 아래 기준으로 사용자와 연결합니다.

| 상품 | user_id | 사용자 |
|------|---------|--------|
| 노트북, 무선 마우스 | 1 | 홍길동 |
| 기계식 키보드, 27인치 모니터 | 2 | 김철수 |
| USB 허브 | 3 | 이영희 |

**작성 위치**

기존 INSERT 문의 컬럼 목록과 VALUES에 `user_id`를 추가합니다.

```sql
-- [수정] 컬럼 목록에 user_id 추가, VALUES에 해당 사용자 id 값 추가
INSERT INTO products (product_name, price, stock_quantity, status, description, user_id)
VALUES ('노트북', 1500000, 10, 'ON_SALE', '고성능 개발용 노트북입니다.', /* userId */);

INSERT INTO products (product_name, price, stock_quantity, status, description, user_id)
VALUES ('무선 마우스', 35000, 50, 'ON_SALE', '2.4GHz 무선 마우스입니다.', /* userId */);

INSERT INTO products (product_name, price, stock_quantity, status, description, user_id)
VALUES ('기계식 키보드', 120000, 0, 'SOLD_OUT', '청축 기계식 키보드입니다.', /* userId */);

INSERT INTO products (product_name, price, stock_quantity, status, description, user_id)
VALUES ('27인치 모니터', 350000, 5, 'ON_SALE', 'QHD 해상도 모니터입니다.', /* userId */);

INSERT INTO products (product_name, price, stock_quantity, status, description, user_id)
VALUES ('USB 허브', 25000, 0, 'DISCONTINUED', '단종된 4포트 USB 허브입니다.', /* userId */);
```

---

## 4. 실행 및 결과 확인

### 4-1. 컴파일 확인

프로젝트 루트에서 아래 명령으로 컴파일 오류가 없는지 확인합니다.

```bash
./mvnw compile
```

오류 없이 `BUILD SUCCESS`가 출력되면 정상입니다.

---

### 4-2. 애플리케이션 실행

```bash
./mvnw spring-boot:run
```

---

### 4-3. 전체 상품 조회 — userId/userName 포함 여부 확인

```bash
curl http://localhost:8080/api/products | python3 -m json.tool
```

응답 예시:

```json
[
  {
    "id": 1,
    "name": "노트북",
    "price": 1500000,
    "stockQuantity": 10,
    "status": "ON_SALE",
    "description": "고성능 개발용 노트북입니다.",
    "displayLabel": "노트북 (ON_SALE)",
    "userId": 1,
    "userName": "홍길동"
  },
  ...
]
```

`userId`와 `userName`이 응답에 포함되어야 합니다.

---

### 4-4. 상품 등록 — userId와 함께 등록

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "블루투스 스피커",
    "price": 80000,
    "stockQuantity": 20,
    "status": "ON_SALE",
    "description": "휴대용 블루투스 스피커입니다.",
    "userId": 2
  }'
```

응답에 `"userId": 2, "userName": "김철수"` 가 포함되어야 합니다.

---

### 4-5. 존재하지 않는 userId로 등록 시도

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "테스트 상품",
    "price": 10000,
    "stockQuantity": 5,
    "status": "ON_SALE",
    "description": "테스트입니다.",
    "userId": 999
  }'
```

`"존재하지 않는 사용자입니다. id=999"` 예외 메시지가 출력되어야 합니다.

---

### 4-6. 사용자 ID로 상품 목록 조회

```bash
# userId=1 (홍길동)의 상품 목록 조회
curl http://localhost:8080/api/products/user/1 | python3 -m json.tool
```

응답에 `"userId": 1, "userName": "홍길동"` 인 상품 2개(노트북, 무선 마우스)가 반환되어야 합니다.

---

### 4-7. 사용자 이름으로 상품 목록 조회

```bash
curl "http://localhost:8080/api/products/user?name=김철수" | python3 -m json.tool
```

응답에 `"userId": 2, "userName": "김철수"` 인 상품 2개(기계식 키보드, 27인치 모니터)가 반환되어야 합니다.
