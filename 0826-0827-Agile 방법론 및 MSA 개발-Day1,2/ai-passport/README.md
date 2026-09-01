# AgentPass MSA

AI가 업무에 필요한 권한을 분석하고, 사용자가 Passport 승인을 요청하면 담당자가 검토·승인하는 교육용 MSA 프로젝트입니다.

> 이 코드는 교육 목적으로 제공됩니다. 상용 환경에서 사용하려면 보안, 운영, 장애 대응 정책을 추가로 검토해야 합니다. 문의: audit@korea.ac.kr, Sungryel Lim Ph.D

## 주요 기능

- 사용자 회원가입 및 OAuth2 로그인
- 사용자·관리자 역할 기반 화면 분리
- 업무 요청 생성과 AI 권한 분석
- Passport 승인 요청, 취소, 상태 및 상세 조회
- 관리자 승인 대기 목록, 상세 검토, 승인 및 반려
- 사용자·관리자 알림과 로그아웃
- 크레딧 조회 및 충전

## 서비스 구성

| 서비스 | 포트 | 설명 |
|---|---:|---|
| Vue frontend | 3000 | 사용자 및 관리자 웹 화면 |
| API Gateway | 8080 | API 진입점 및 JWT 검증 |
| User service | 8081 | 사용자 정보 |
| Course service | 8082 | 업무 요청 및 AI 분석 |
| Enrollment service | 8083 | Passport 신청·승인 |
| Payment service | 8084 | 크레딧 |
| Recommend service | 8085 | 기존 Passport 재사용 추천 |
| Eureka server | 8761 | 서비스 디스커버리 |
| Auth server | 9000 | OAuth2 인증·인가 |
| MariaDB | 3379 | 데이터 저장소 |
| Kafka | 9092 | 서비스 이벤트 |

백엔드는 다음 순서로 기동됩니다.

```text
MariaDB / Kafka
  -> Eureka
    -> Auth server
      -> API Gateway + domain services
        -> Recommend service
```

## 준비 사항

- Docker Desktop 및 Docker Compose
- Node.js 20 이상과 npm
- `infra-images.tar` 또는 별도로 배포된 Auth server/API Gateway 이미지

`infra-images.tar`는 용량 때문에 Git 저장소에서 제외됩니다. GitHub Release에 첨부하거나 컨테이너 레지스트리에서 이미지를 배포하세요.

## 환경 설정

저장소를 받은 뒤 공개 예제 설정을 복사합니다.

```bash
cp .env.example .env
```

`.env`의 MariaDB 비밀번호를 로컬 환경에 맞게 변경하세요. `.env`는 Git에 포함되지 않습니다.

Recommend service를 Docker 밖에서 직접 실행한다면 별도 설정도 복사합니다.

```bash
cp recommend-service/.env.example recommend-service/.env
```

## 백엔드 실행

이미지 tar를 전달받은 경우 프로젝트 루트에 둔 뒤 불러옵니다.

```bash
docker load -i infra-images.tar
docker images | grep msa-lecture
```

컨테이너를 빌드하고 실행합니다.

```bash
docker compose build
docker compose up -d
docker compose ps
```

전체 로그 또는 개별 서비스 로그를 확인할 수 있습니다.

```bash
docker compose logs -f
docker compose logs -f enrollment-service
```

종료하려면 다음 명령을 사용합니다. 데이터 볼륨은 유지됩니다.

```bash
docker compose down
```

## 로컬 Ollama AI 분석 테스트

Course service의 `POST /api/courses/{id}/analyze`는 로컬 Ollama를 호출해 두 단계로
분석합니다. 먼저 적합한 에이전트를 복수 선택하고, 다음으로 각 에이전트에 필요한
최소 권한만 선택합니다. 선택되지 않은 YAML 권한은 백엔드가 안전하게 제외 목록으로
완성해 응답 JSON에 반환합니다. 클라우드 API 키나 LLM 관련 `.env` 값은 필요하지 않습니다.

macOS에서 Homebrew를 사용한다면 Ollama와 기본 테스트 모델을 설치합니다.

```bash
brew install --cask ollama
ollama serve
ollama pull qwen3.5:4b
```

`ollama serve`는 별도 터미널에서 계속 실행해 둡니다. Ollama 앱을 실행한 경우에는
앱이 서버를 실행하므로 이 명령은 생략할 수 있습니다. 다음 명령으로 모델 준비 상태를
확인합니다.

```bash
curl http://localhost:11434/api/tags
```

기본 모델은 `qwen3.5:4b`이며, 권한 분류 단계는 로컬 환경에서 최대 120초까지 걸릴 수
있습니다. 분석 요청을 보내는 동안 Course service를 종료하지 마세요.

Docker에는 MariaDB, Eureka 등 나머지 서비스를 유지하고, Course와 Enrollment만
로컬에서 실행하는 예시는 다음과 같습니다. 첫 번째 명령은 컨테이너의 두 서비스를
중지하며 DB는 유지합니다. 두 서비스는 기본으로 `local` Spring 프로필을 사용하고,
프로필이 저장소 루트의 `.env`에서 DB 계정을 읽으므로 긴 환경변수 지정은 필요하지
않습니다.

```bash
docker compose stop course-service enrollment-service

cd course-service
./gradlew bootRun
```

새 터미널에서 Enrollment도 실행합니다. `local` 프로필은 Docker Kafka의 호스트 광고
문제를 피하기 위해 Kafka 리스너와 admin 자동 생성을 비활성화하고, Course에는
`localhost:8082`로 연결합니다.

```bash
cd enrollment-service
./gradlew bootRun
```

Docker Compose는 `docker` 프로필을 명시적으로 활성화하므로 컨테이너에서는
`lecturedb`, `eureka-server`, `course-service` 같은 Docker 네트워크 주소가 자동으로
사용됩니다.

Course Swagger는 <http://localhost:8082/swagger-ui.html>, Enrollment Swagger는
<http://localhost:8083/swagger-ui.html>입니다. Course에서 업무 요청을 생성한 뒤 같은
`X-User-Id` 헤더로 `POST /api/courses/{id}/analyze`를 호출하면 됩니다. 성공 응답의
`data.agentList`에 선택된 복수 에이전트와 각 권한/제외 권한이 들어갑니다.

## 프론트엔드 실행

```bash
cd vue-frontend
npm install
npm run dev
```

브라우저에서 <http://localhost:3000>에 접속합니다. API 요청은 개발 서버 프록시를 통해 `http://localhost:8080`으로 전달됩니다.

프로덕션 빌드는 다음과 같이 확인합니다.

```bash
cd vue-frontend
npm run build
```

## 역할별 흐름

- `STUDENT`: 업무 요청 생성 → AI 분석 확인 → Passport 승인 요청 → 발급 상태 확인
- `INSTRUCTOR`: 승인 대기 현황 → 요청 상세 검토 → 승인 또는 반려

로그인 후 토큰의 역할에 따라 사용자 화면(`/`) 또는 관리자 화면(`/admin`)으로 이동합니다.

## GitHub 업로드 전 확인

아래 명령으로 빌드 결과물, 환경 파일, Docker 이미지 tar가 추적되지 않는지 확인하세요.

```bash
git status --short
git check-ignore -v infra-images.tar recommend-service/.env vue-frontend/node_modules vue-frontend/dist
```

다음 파일과 디렉터리는 커밋하지 않습니다.

- `.env`, `recommend-service/.env`
- `infra-images.tar`
- `node_modules`, `dist`
- Gradle의 `.gradle`, `build`, `bin`
- IDE 설정, 로그, 운영체제 임시 파일
