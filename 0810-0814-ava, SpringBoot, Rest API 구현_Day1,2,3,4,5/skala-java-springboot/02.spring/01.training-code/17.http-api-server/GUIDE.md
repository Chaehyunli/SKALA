# HttpWebServer 학습 가이드

## 1. 이 코드의 목적

이 코드는 실제 서비스에 쓰기 위한 것이 아니라, **Spring(Spring MVC, Spring Boot)이 내부적으로 어떻게 동작하는지**를   
순수 Java(`ServerSocket`, 리플렉션, 커스텀 어노테이션)만으로 아주 단순하게 재현해 본 학습용 예제이다.

Spring을 쓰면 `@Controller`, `@GetMapping`, `@PostMapping`, `@PathVariable` 같은 어노테이션만 붙이면 알아서 요청이 라우팅되고 객체가 JSON으로 변환된다.   
그 "알아서"가 실제로는 다음과 같은 원리로 동작한다는 것을 코드 레벨에서 보여주는 것이 이 예제의 목표이다.

- HTTP는 결국 소켓으로 주고받는 **텍스트 프로토콜**이라는 것 (`ServerSocket` / `Socket` / `BufferedReader` / `PrintWriter`)
- `@Controller`가 붙은 클래스를 어떻게 찾아서 등록하는지 (`registerController`)
- `@GetMapping("/users/{id}")` 같은 어노테이션의 값(경로 문자열)을 **리플렉션**으로 읽어서 라우팅 테이블을 만드는 원리 (`registerRoute`)
- 요청이 들어왔을 때 URL과 라우팅 테이블을 비교해서 어떤 메서드를 호출할지 찾는 과정 (`handleRequest`)
- 메서드 파라미터에 `@PathVariable`이 붙어 있으면 경로 값을, 없으면 요청 Body를 넘겨주는 원리 (`convertParam`)
- 메서드가 반환한 Java 객체(`User`, `List<User>`)를 JSON 문자열로 바꿔 응답하는 원리 (`toResponseBody`, `User.toJson()`)

즉 "Controller 객체로 요청이 전달되는 구조"와 "annotation들이 서로 어떻게 연결되어 동작하는가"를 눈으로 확인하기 위한 코드이다.

## 2. 전체 패키지 구조

```
17.httpwebserver/
├── run.sh                                 빌드 + 서버 실행 스크립트
├── HttpWebClient.java                     테스트용 HTTP 클라이언트 (패키지 없음)
└── sk/skala/com/httpserver/
    ├── Main.java                          애플리케이션 진입점
    ├── annotation/
    │   ├── Controller.java                클래스 대상 어노테이션
    │   ├── GetMapping.java                메서드 대상 어노테이션 (GET)
    │   ├── PostMapping.java               메서드 대상 어노테이션 (POST)
    │   └── PathVariable.java              파라미터 대상 어노테이션
    ├── domain/
    │   └── User.java                      사용자 도메인 객체 (DTO)
    ├── controller/
    │   └── UserController.java            사용자 요청 처리 컨트롤러
    └── server/
        └── HttpWebServer.java             소켓 기반 HTTP 서버 본체
```

## 3. 패키지별 역할

| 패키지 | 역할 | Spring에서의 대응 개념 |
|---|---|---|
| `sk.skala.com.httpserver` (root) | 애플리케이션 진입점 | `@SpringBootApplication` + `main()` |
| `sk.skala.com.httpserver.annotation` | 라우팅/바인딩에 쓰이는 커스텀 어노테이션 정의 | `org.springframework.web.bind.annotation` 패키지 |
| `sk.skala.com.httpserver.domain` | 요청/응답에 사용되는 데이터 객체 | 도메인 클래스 / DTO |
| `sk.skala.com.httpserver.controller` | 실제 요청 처리 로직 | `@Controller` 클래스 |
| `sk.skala.com.httpserver.server` | 소켓 수신, 라우팅, 리플렉션 호출, 응답 전송을 담당하는 프레임워크 본체 | 내장 톰캣 + `DispatcherServlet` + `RequestMappingHandlerMapping` |

## 4. 클래스별 기능 및 역할

### 4.1 annotation 패키지

| 클래스 | 대상(`@Target`) | 필드 | 역할 |
|---|---|---|---|
| `Controller` | `TYPE` (클래스) | 없음 | 이 클래스가 요청을 처리하는 컨트롤러임을 표시.<br>`HttpWebServer.registerController()`가 이 어노테이션이 있는지 검사 |
| `GetMapping` | `METHOD` (메서드) | `value()` : URL 경로 | 이 메서드가 GET 요청을 처리함을 표시하고 매핑할 경로를 지정 |
| `PostMapping` | `METHOD` (메서드) | `value()` : URL 경로 | 이 메서드가 POST 요청을 처리함을 표시하고 매핑할 경로를 지정 |
| `PathVariable` | `PARAMETER` (파라미터) | `value()` : 경로 변수 이름 | 이 파라미터가 URL 경로의 변수 값(`{id}` 등)을 받는 것임을 표시 |

모든 어노테이션은 `@Retention(RetentionPolicy.RUNTIME)`으로 선언되어 있다. <br>
이는 컴파일 후에도 어노테이션 정보가 클래스 파일에 남아 있어야, 실행 중에 리플렉션(`Class.isAnnotationPresent`, `Method.getAnnotation` 등)으로 읽을 수 있기 때문이다. <br>
 `RUNTIME`이 아니라면 `HttpWebServer`가 어노테이션 정보를 전혀 읽을 수 없다.

### 4.2 domain 패키지

| 클래스 | 역할 |
|---|---|
| `User` | `id`, `name`, `role`, `email` 필드를 가진 불변 객체. `toJson()` 메서드로 자기 자신을 JSON 문자열로 직렬화한다. <br> Spring에서 Jackson이 자동으로 해주는 객체→JSON 변환을 여기서는 직접 문자열을 조립해서 수행한다 |

### 4.3 controller 패키지

| 클래스 | 역할 |
|---|---|
| `UserController` | `@Controller`가 붙은 클래스. 메모리 리스트(`List<User>`)로 사용자 데이터를 관리하며 GET/POST 요청 처리 메서드 3개를 제공 |

#### `UserController`의 메서드:

| 메서드 | 어노테이션 | 파라미터 | 반환값 | 역할 |
|---|---|---|---|---|
| `getUsers()` | `@GetMapping("/users")` | 없음 | `List<User>` | 전체 사용자 목록 반환 |
| `getUser(int id)` | `@GetMapping("/users/{id}")` + `@PathVariable("id")` | 경로 변수 `id` | `User` (없으면 `null`) | id로 사용자 한 명 조회 |
| `createUser(User user)` | `@PostMapping("/users")` | 요청 Body를 변환한 `User` | `User` | 새 사용자를 목록에 추가 후 반환 |

### 4.4 server 패키지

| 클래스/레코드 | 역할 |
|---|---|
| `HttpWebServer` | 소켓을 열어 요청을 받고, 라우팅 테이블에서 처리할 메서드를 찾아 리플렉션으로 호출한 뒤 응답을 만들어 보내는 프레임워크 본체 |
| `RouteEntry` (내부 record) | 라우팅 테이블의 값 타입. `(컨트롤러 인스턴스, Method)` 쌍을 저장 |
| `PatternRoute` (내부 record) | 경로 변수가 있는 라우트 정보. `(HTTP 메서드, URL prefix, RouteEntry)` 저장 |

#### `HttpWebServer`의 메서드:

| 메서드 | 역할 |
|---|---|
| `registerController(Object controller)` | `@Controller` 여부 검사 후, 클래스의 모든 메서드를 훑어 `@GetMapping`/`@PostMapping`이 있으면 `registerRoute()` 호출 |
| `registerRoute(...)` | 경로에 `{`가 없으면 `routes` Map(정확 일치)에, 있으면 `patternRoutes` 목록(prefix 일치)에 등록 |
| `start()` | `ServerSocket`을 열고 `accept()`로 클라이언트 연결을 반복 대기, 연결마다 `handleRequest()` 호출 |
| `handleRequest(Socket)` | 요청 한 건을 읽고, 라우팅하고, 메서드를 호출하고, 응답을 전송하는 전체 처리 담당 |
| `convertParam(Class, String)` | 문자열 값을 메서드 파라미터 타입(`int`, `User`, `String`)에 맞게 변환 |
| `toResponseBody(Object)` | 메서드 반환값(`User`, `List<User>`, 기타)을 JSON 문자열로 변환 |
| `parseUser(String json)` | 정규식으로 JSON 문자열에서 `id/name/role/email`을 추출해 `User` 객체 생성 |
| `extractString`, `extractInt` | `parseUser`에서 사용하는 정규식 기반 필드 추출 헬퍼 |
| `sendResponse(...)` | 상태라인 + 헤더 + 빈 줄 + Body 순서로 HTTP 응답을 조립해 전송 |

### 4.5 root 패키지

| 클래스 | 역할 |
|---|---|
| `Main` | `HttpWebServer`를 생성하고 `UserController`를 등록한 뒤 서버를 시작하는 진입점 |
| `HttpWebClient` | 서버에 GET/POST 요청을 보내 응답을 콘솔에 출력하는 테스트 클라이언트 (패키지 없이 독립 실행) |

## 5. 서버 기동 시 호출 흐름

서버가 켜질 때 메서드가 호출되는 순서는 다음과 같다.

1. `Main.main()` 실행
2. `new HttpWebServer(8080)` → 생성자에서 `port` 필드만 저장, `routes`/`patternRoutes`는 빈 상태로 초기화
3. `server.registerController(new UserController())` 호출
   1. `UserController` 클래스에 `@Controller`가 있는지 확인
   2. `clazz.getDeclaredMethods()`로 선언된 모든 메서드를 순회
   3. `getUsers()` → `@GetMapping` 발견 → `registerRoute("GET", "/users", ...)` → `{"GET /users": RouteEntry}`를 `routes`에 저장
   4. `getUser(int id)` → `@GetMapping("/users/{id}")` 발견 → 경로에 `{`가 있으므로 `registerRoute()`가 `patternRoutes`에 `PatternRoute("GET", "/users/", RouteEntry)` 추가
   5. `createUser(User user)` → `@PostMapping` 발견 → `registerRoute("POST", "/users", ...)` → `{"POST /users": RouteEntry}`를 `routes`에 저장
4. `server.start()` 호출
   1. `new ServerSocket(8080)`으로 포트를 열고 리스닝 시작
   2. `while(true)` 루프에서 `serverSocket.accept()`가 클라이언트 연결이 들어올 때까지 대기(blocking)

이 시점부터 서버는 요청을 받을 준비가 끝난 상태로 대기한다.

## 6. HTTP 요청 대기 및 처리·응답 흐름

클라이언트가 요청을 보낸 순간부터 응답을 받기까지, `HttpWebServer` 내부에서 메서드가 호출되는 순서는 다음과 같다.

1. `accept()`가 대기를 끝내고 `Socket clientSocket`을 반환 → `handleRequest(clientSocket)` 호출
2. **Step 1** : `reader.readLine()`으로 요청 라인 한 줄을 읽는다 (예: `GET /users/1 HTTP/1.1`)
3. **Step 2** : 빈 줄이 나올 때까지 헤더를 한 줄씩 읽으며 `Content-Length` 헤더 값을 찾아 `contentLength`에 저장
4. **Step 3** : `contentLength`가 0보다 크면(POST인 경우) 그 길이만큼 문자를 읽어 `requestBody`에 저장
5. **Step 4** : 요청 라인을 공백 기준으로 나눠 `httpMethod`("GET"/"POST")와 `path`("/users/1")를 분리
6. **Step 5** : 라우팅 검색
   1. 먼저 `"GET /users/1"` 같은 키로 `routes` Map에서 정확히 일치하는 라우트를 찾는다
   2. 없으면 `patternRoutes`를 순회하며 `path`가 `prefix`(`"/users/"`)로 시작하는지 검사하고, 일치하면 나머지 부분(`"1"`)을 `pathVariable`로 추출한다
   3. 그래도 못 찾으면 `sendResponse(404, ...)`로 응답하고 종료
7. **Step 6** : 찾은 `Method`를 리플렉션으로 호출
   1. 파라미터가 없으면 `method.invoke(instance)`
   2. 파라미터가 있으면 `method.getParameters()[0]`을 확인해 `@PathVariable`이 붙어 있으면 `pathVariable`을, 아니면 `requestBody`를 원본 문자열로 선택
   3. `convertParam(파라미터타입, 원본문자열)`로 실제 타입(`int` 또는 `User`)에 맞게 변환
   4. `method.invoke(instance, 변환된값)`으로 컨트롤러 메서드 실행 → 결과 객체(`User`, `List<User>` 등) 획득
      - `createUser(User user)` 호출 전에는 내부적으로 `convertParam` → `parseUser(requestBody)` → `extractString`/`extractInt`가 차례로 실행되어 JSON 문자열이 `User` 객체로 바뀐다
8. `toResponseBody(result)`로 반환 객체를 JSON 문자열로 변환
   - `User` 하나면 `user.toJson()` 호출
   - `List<User>`면 각 원소마다 `toJson()`을 호출해 `[...]` 배열 문자열로 조합
9. `sendResponse(writer, 200, "OK", responseBody)` 호출 → 상태라인, 헤더, 빈 줄, Body 순서로 소켓에 출력
10. `handleRequest()` 종료 → try-with-resources로 `reader`/`writer`가 닫히며 연결 종료
11. `start()`의 `while(true)` 루프가 다시 `accept()`에서 다음 클라이언트 연결을 대기

이 서버는 스레드 풀 없이 한 번에 하나의 요청만 순서대로 처리하는 단일 스레드 구조이다. 그래서 요청 하나를 처리하는 동안에는 다른 클라이언트가 연결되어도 대기하게 된다. Spring Boot의 내장 톰캣은 이 부분을 스레드 풀로 병렬 처리하도록 확장한 것이라고 이해하면 된다.

## 7. 테스트 방법

### 7.1 빌드

`17.httpwebserver` 디렉터리에서 실행한다.

```
javac -d out $(find sk -name "*.java")
```

`sk` 디렉터리 아래의 모든 `.java` 파일을 컴파일해서 `out` 디렉터리에 클래스 파일을 생성한다. `run.sh`를 실행하면 이 컴파일 과정과 서버 실행이 한 번에 수행된다.

```
./run.sh
```

### 7.2 서버 실행

```
java -cp out sk.skala.com.httpserver.Main
```

실행하면 콘솔에 등록된 라우트 목록과 `HttpWebServer started on port 8080`이 출력되고, 프로세스가 종료되지 않은 채 요청을 대기한다. `run.sh`를 사용했다면 빌드 후 자동으로 이 명령까지 실행된다.

서버를 멈추려면 해당 터미널에서 `Ctrl + C`를 입력한다.

### 7.3 클라이언트 실행

서버가 실행 중인 상태에서, 새 터미널을 열어 같은 디렉터리에서 실행한다.

```
java HttpWebClient.java
```

`HttpWebClient`는 다음 순서로 요청을 보내고 응답을 콘솔에 출력한다.

1. `GET /users` — 전체 사용자 목록 조회
2. `GET /users/1` — id가 1인 사용자 조회
3. `GET /notfound` — 존재하지 않는 경로 → 404 응답 확인용
4. `POST /users` — JSON Body로 새 사용자 생성 요청

각 요청마다 서버가 반환한 상태 코드, 헤더, JSON Body가 출력되며, 동시에 서버 쪽 터미널에도 요청 라인과 응답 로그가 출력되는 것을 확인할 수 있다.

### 7.4 브라우저에서 실행

서버가 실행 중인 상태에서 브라우저 주소창에 아래 URL을 입력해 GET 요청 결과를 직접 확인할 수 있다.

```
http://localhost:8080/users
http://localhost:8080/users/1
http://localhost:8080/users/2
http://localhost:8080/notfound
```

브라우저는 GET 요청만 간단히 보낼 수 있으므로, POST 요청(`createUser`) 동작은 7.3의 `HttpWebClient` 실행 결과로 확인한다. 브라우저 개발자 도구의 네트워크 탭을 열어두면 응답 헤더(`Content-Type`, `Content-Length` 등)도 함께 확인할 수 있다.
