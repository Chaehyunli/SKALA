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
COUNTRY_URL = "https://countries.dev/alpha/KOR"
IP_URL = "http://ip-api.com/json/8.8.8.8"

# 네트워크가 느리거나 응답이 없을 때 무한 대기하지 않도록 공통 타임아웃을 둔다.
REQUEST_TIMEOUT_SECONDS = 15.0

# app/config.py 기준으로 두 단계 위(프로젝트 루트)를 계산한다.
PROJECT_ROOT = Path(__file__).resolve().parent.parent
OUTPUT_DIR = PROJECT_ROOT / "data" / "output"

WEATHER_CSV = OUTPUT_DIR / "weather.csv"
WEATHER_PARQUET = OUTPUT_DIR / "weather.parquet"
COUNTRY_CSV = OUTPUT_DIR / "country.csv"
COUNTRY_PARQUET = OUTPUT_DIR / "country.parquet"
IP_CSV = OUTPUT_DIR / "ip.csv"
IP_PARQUET = OUTPUT_DIR / "ip.parquet"

PERFORMANCE_OUTPUT = OUTPUT_DIR / "performance_result.json"
VALIDATION_ERROR_OUTPUT = OUTPUT_DIR / "validation_errors.json"
