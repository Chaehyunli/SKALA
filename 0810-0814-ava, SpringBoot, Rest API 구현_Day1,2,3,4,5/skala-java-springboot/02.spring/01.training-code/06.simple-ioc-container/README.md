# Simple IoC Container (순수 Java 구현)

## 목적

Spring의 IoC(Inversion of Control) 컨테이너가 내부적으로 어떻게 동작하는지를  
**순수 Java 리플렉션**만으로 재현하여 핵심 개념을 학습한다.

- 개발자가 직접 `new`로 객체를 생성하지 않고 **컨테이너가 대신 생성·주입**한다는 제어의 역전(IoC) 원리를 이해한다.  
- 생성자 기반 의존성 주입(Constructor Injection)이 어떻게 이루어지는지 코드 수준에서 파악한다.  
- `@Service` 같은 **커스텀 어노테이션을 런타임에 읽는 방법**(`@Retention(RUNTIME)` + 리플렉션)을 익힌다.

---

## 주요 학습 범위

| 개념 | 설명 |
|------|------|
| **IoC (제어의 역전)** | 객체 생성과 의존성 연결의 책임을 개발자가 아닌 컨테이너에 위임 |
| **DI (의존성 주입)** | 생성자 파라미터 타입을 분석해 필요한 빈을 자동으로 주입 (Constructor Injection) |
| **커스텀 어노테이션** | `@Target`, `@Retention`을 설정해 런타임에 읽힐 수 있는 마커 어노테이션 정의 |
| **Java Reflection** | `Class.getConstructors()`, `Constructor.getParameterTypes()`, `Constructor.newInstance()` |
| **싱글톤 패턴** | 동일 타입 빈은 한 번만 생성하고 재사용 (컨테이너가 보장) |
| **ConcurrentHashMap** | 스레드 안전한 빈 저장소로 `ConcurrentMap<String, Object>` 활용 |
| **재귀적 의존성 해결** | 의존 빈이 없으면 재귀 호출로 먼저 생성한 뒤 주입 |

---

## 파일 구조

```
06.simple-ioc-container/
├── README.md
└── src/
    └── com/
        └── sk/
            └── skala/
                └── ioc/
                    ├── Main.java                          ← 진입점 (컨테이너 사용 예시)
                    ├── annotation/
                    │   └── Service.java                   ← 커스텀 @Service 어노테이션
                    ├── container/
                    │   └── SimpleIoCContainer.java        ← IoC 컨테이너 핵심 구현
                    └── service/
                        ├── DatabaseService.java           ← 최하위 서비스 (의존성 없음)
                        ├── UserService.java               ← DatabaseService 에 의존
                        └── OrderService.java              ← UserService 에 의존 (최상위)
```

---

## 각 파일별 역할

### `annotation/Service.java`
컨테이너가 관리할 클래스를 표시하는 **마커 어노테이션**.

```java
@Target(ElementType.TYPE)           // 클래스에만 사용 가능
@Retention(RetentionPolicy.RUNTIME) // 런타임 리플렉션으로 읽기 가능
public @interface Service {
}
```

---

### `container/SimpleIoCContainer.java`
IoC 컨테이너의 **핵심 로직**을 담당한다.

| 메서드 | 역할 |
|--------|------|
| `register(Class<?>... classes)` | `@Service` 클래스 목록을 받아 빈 등록 시작 |
| `createBean(Class<?>)` | 리플렉션으로 생성자를 분석, 의존성을 재귀 해결 후 인스턴스 생성 |
| `getBean(Class<T>)` | 등록된 빈을 타입으로 조회 (`clazz.getSimpleName()` 키 사용) |
| `getBeanRegistry()` | 전체 빈 저장소(`ConcurrentHashMap`) 반환 |

> 빈 이름은 별도 메서드 없이 `clazz.getSimpleName()`을 직접 사용한다. (예: `DatabaseService`)

**동작 흐름 (재귀 의존성 해결):**

```
register(OrderService, UserService, DatabaseService)
   │
   ├─ OrderService 생성 시도 → 생성자 파라미터: UserService 필요
   │      │
   │      ├─ UserService 생성 시도 → 생성자 파라미터: DatabaseService 필요
   │      │      │
   │      │      └─ DatabaseService 생성 (파라미터 없음) → beanRegistry 등록
   │      │
   │      └─ DatabaseService 주입 → UserService 생성 → beanRegistry 등록
   │
   └─ UserService 주입 → OrderService 생성 → beanRegistry 등록
```

---

### `service/DatabaseService.java`
**최하위 서비스** — 아무 의존성 없이 기본 생성자로 직접 생성된다.

```
의존성: 없음
역할: DB 조회를 흉내 내는 단순 메서드 제공
```

---

### `service/UserService.java`
**중간 계층 서비스** — `DatabaseService`를 생성자로 주입받는다.

```
의존성: DatabaseService (생성자 주입)
역할: 사용자 정보 조회
```

---

### `service/OrderService.java`
**최상위 서비스** — `UserService`를 생성자로 주입받는다.

```
의존성: UserService (생성자 주입)
역할: 주문 처리 흐름 제어
```

---

### `Main.java`
컨테이너를 사용하는 **클라이언트 코드**.  
아래 4가지를 순서대로 시연한다.

1. 컨테이너에 `@Service` 클래스 등록
2. 등록된 빈 목록 출력 (`ConcurrentHashMap` 내용)
3. 실제 빈을 꺼내서 비즈니스 로직 실행
4. 동일 타입 빈이 싱글톤인지 확인

---

## 실행 방법

### 1. 컴파일

```bash
# 프로젝트 루트로 이동
cd 06.simple-ioc-container

# bin 디렉토리에 컴파일
javac -d bin $(find src -name "*.java")
```

### 2. 실행

```bash
java -cp bin com.sk.skala.ioc.Main
```

### 3. 실행 결과 (예시)

```
========== Simple IoC Container 시작 ==========

========== 빈(Bean) 등록 & 의존성 주입 ==========
[컨테이너] 'OrderService' 생성 준비 - 의존성 1개 분석 중...
[컨테이너] 'UserService' 생성 준비 - 의존성 1개 분석 중...
[컨테이너] 'DatabaseService' 생성 (의존성 없음)
  DatabaseService 생성자 호출
[컨테이너] 'DatabaseService' 등록 완료 → beanRegistry 크기: 1
  ↳ 주입 파라미터[0]: DatabaseService
  UserService 생성자 호출 (DatabaseService 주입됨)
[컨테이너] 'UserService' 등록 완료 → beanRegistry 크기: 2
  ↳ 주입 파라미터[0]: UserService
  OrderService 생성자 호출 (UserService 주입됨)
[컨테이너] 'OrderService' 등록 완료 → beanRegistry 크기: 3

========== 등록된 빈 목록 (ConcurrentHashMap) ==========
  DatabaseService      → DatabaseService@...
  UserService          → UserService@... -> (databaseService=DatabaseService@...)
  OrderService         → OrderService@... -> (userService=UserService@...)

========== 빈 사용 예시 ==========
  [ORDER] 주문 처리 완료 - [DB] 사용자 조회 -> 홍길동

========== 싱글톤 확인 (같은 인스턴스인지 비교) ==========
  getBean(UserService) 1회: UserService@5b6f7412 -> ...
  getBean(UserService) 2회: UserService@5b6f7412 -> ...
  동일 인스턴스? → true

========== 완료 ==========
```

---

## Spring IoC와의 비교

| 항목 | 이 구현체 | Spring IoC |
|------|-----------|------------|
| 빈 표시 | `@Service` (커스텀) | `@Component`, `@Service`, `@Bean` 등 |
| 의존성 주입 방식 | 생성자 주입 | 생성자 / 필드 / Setter 모두 지원 |
| 빈 저장소 | `ConcurrentHashMap` | `DefaultListableBeanFactory` |
| 클래스 스캔 | 수동으로 클래스 목록 전달 | `@ComponentScan`으로 패키지 자동 스캔 |
| 싱글톤 | 단순 `containsKey` 체크 | `Scope` 어노테이션으로 Singleton / Prototype 구분 |
| 순환 참조 감지 | 없음 (무한 재귀 발생) | 감지 후 예외 처리 |

> 이 구현체는 Spring의 복잡한 기능을 제거하고 **IoC의 본질적인 원리만 단순하게 표현**하는 데 목적이 있다.
