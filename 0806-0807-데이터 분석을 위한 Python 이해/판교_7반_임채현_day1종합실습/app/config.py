# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [Day 1 종합실습] API 주소와 출력 경로를 한 곳에서 관리
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, WEATHER/COUNTRY/IP URL 및 출력 경로 상수 정의
#
# ------------------------------------------------------------------
"""API 주소와 출력 경로를 관리합니다."""

from pathlib import Path

# 슬라이드에 명시된 요청 URL을 그대로 상수로 고정한다.
WEATHER_URL = (
    "https://api.open-meteo.com/v1/forecast"
    "?latitude=37.5665&longitude=126.9780"
    "&hourly=temperature_2m,precipitation_probability"
    "&forecast_days=3&timezone=Asia/Seoul"
)
COUNTRY_URL = "https://countries.dev/alpha/KOR"  # 한국(KOR) 고정 조회
IP_URL = "http://ip-api.com/json/8.8.8.8"  # 구글 공개 DNS IP 고정 조회

# 네트워크가 느리거나 응답이 없을 때 무한 대기하지 않도록 공통 타임아웃을 둔다.
# (api_client.fetch_json이 client.get() 호출 시 이 값을 사용한다.)
REQUEST_TIMEOUT_SECONDS = 15.0

# app/config.py -> app/ -> 프로젝트 루트 순으로 두 단계 위를 계산한다.
# 이렇게 __file__ 기준 상대 경로로 구하면, 스크립트를 어느 디렉터리에서
# 실행하든(터미널 cwd와 무관하게) 항상 같은 절대경로를 가리킨다.
PROJECT_ROOT = Path(__file__).resolve().parent.parent
OUTPUT_DIR = PROJECT_ROOT / "data" / "output"

# 소스별로 CSV/Parquet 출력 경로를 미리 상수로 고정해,
# storage.py/main.py/scripts/*.py가 모두 같은 경로를 참조하게 한다.
WEATHER_CSV = OUTPUT_DIR / "weather.csv"
WEATHER_PARQUET = OUTPUT_DIR / "weather.parquet"
COUNTRY_CSV = OUTPUT_DIR / "country.csv"
COUNTRY_PARQUET = OUTPUT_DIR / "country.parquet"
IP_CSV = OUTPUT_DIR / "ip.csv"
IP_PARQUET = OUTPUT_DIR / "ip.parquet"

# 3개 소스의 성능 측정 결과를 모아서 저장하는 파일 (main.py에서 리스트로 저장)
PERFORMANCE_OUTPUT = OUTPUT_DIR / "performance_result.json"
# 검증 실패 레코드가 하나라도 있을 때만 생성되고, 없으면 main.py가 삭제한다.
VALIDATION_ERROR_OUTPUT = OUTPUT_DIR / "validation_errors.json"
