#!/usr/bin/env bash
# ============================================================
# 실습 환경 점검 + 실행 명령 안내 스크립트
#
#   chmod +x run-examples.sh && ./run-examples.sh
#
# 이 스크립트는 애플리케이션을 직접 띄우지 않는다.
# API 키 설정 여부만 확인하고, 각 STEP의 실행 명령을 출력한다.
# (모든 STEP이 서버를 계속 점유하므로 하나씩 골라 실행하는 편이 낫다)
# ============================================================

# set -e : 명령이 하나라도 실패하면 즉시 스크립트를 중단한다.
set -e

# 필수 환경변수 확인.
#   -z "문자열"        : 비어 있으면 참
#   ${OPENAI_API_KEY:-} : 변수가 없어도 오류 없이 빈 값으로 취급 (set -u 대비 안전 표기)
if [ -z "${OPENAI_API_KEY:-}" ]; then
  echo 'OPENAI_API_KEY가 없습니다.'
  echo 'export OPENAI_API_KEY="YOUR_API_KEY"'
  # 0이 아닌 종료 코드 = 실패. CI 등에서 이 값으로 성공/실패를 판단한다.
  exit 1
fi

# 따옴표로 감싼 'EOF' → 내부의 $ 나 백틱이 치환되지 않고 그대로 출력된다.
cat <<'EOF'
실행 예:
  ./gradlew :step01-basic-chat:bootRun         # 8101 기본 Chat
  ./gradlew :step02-structured-output:bootRun  # 8102 구조화 출력
  ./gradlew :step03-streaming:bootRun          # 8103 스트리밍 (브라우저: http://localhost:8103)
  ./gradlew :step04-advisor:bootRun            # 8104 Advisor + 로깅
  ./gradlew :step05-memory:bootRun             # 8105 대화 기억
  ./gradlew :step06-rag-pgvector:bootRun       # 8106 RAG (먼저 docker compose up -d)
  ./gradlew :step07-tool-calling:bootRun       # 8107 Tool Calling
  ./gradlew :step08-mcp-server:bootRun         # 8108 MCP 서버 (반드시 먼저 실행)
  ./gradlew :step08-mcp-client:bootRun         # 8109 MCP 클라이언트
  ./gradlew :step10-observability:bootRun      # 8110 관측 (/actuator/prometheus)

STEP 09는 서버가 아니라 테스트로 실행한다 (pgvector 필요):
  ./gradlew :step09-evaluation:test
EOF
