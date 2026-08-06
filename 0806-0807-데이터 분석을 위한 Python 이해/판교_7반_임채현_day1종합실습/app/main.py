# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [Day 1 종합실습] 데이터 수집 미니 파이프라인 - 실행 진입점
#               1) asyncio.gather()로 Open-Meteo/Countries.dev/ip-api 3개를 동시 수집
#               2) 수집한 JSON을 Pydantic v2(strict) 모델로 검증, 오류는 별도 분리
#               3) 검증 통과 데이터를 CSV/Parquet 두 형식으로 저장하고 성능 비교
#               4) 저장한 파일을 다시 읽어 건수(및 키 값)가 일치하는지 검증
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, run_pipeline + main(예외 처리) 구현
#
# ------------------------------------------------------------------
"""Day 1 종합실습 전체 파이프라인을 실행합니다."""

from __future__ import annotations

import asyncio

import httpx

from app.api_client import ApiFetchError, fetch_all_data
from app.config import (
    COUNTRY_CSV,
    COUNTRY_PARQUET,
    IP_CSV,
    IP_PARQUET,
    OUTPUT_DIR,
    PERFORMANCE_OUTPUT,
    VALIDATION_ERROR_OUTPUT,
    WEATHER_CSV,
    WEATHER_PARQUET,
)
from app.models import CountryInfo, IPInfo, WeatherHourRecord
from app.pipeline import extract_weather_rows, validate_many
from app.storage import save_json, save_with_performance, verify_saved_data


async def run_pipeline() -> None:
    """수집, 검증, 저장, 재로딩 검증을 순서대로 실행합니다."""
    print("=== 1. API 3개 동시 수집 ===")
    # follow_redirects=True: countries.dev가 https로 리다이렉트할 가능성에 대비.
    async with httpx.AsyncClient(follow_redirects=True) as client:
        raw_data = await fetch_all_data(client)

    weather_hour_count = len(raw_data["weather"].get("hourly", {}).get("time", []))
    print(
        "수집 완료:",
        f"weather(시간대)={weather_hour_count},",
        f"country={raw_data['country'].get('name')},",
        f"ip={raw_data['ip'].get('query')}",
    )

    print("\n=== 2. Pydantic v2 검증 (strict) ===")
    # weather는 열 지향 원본을 행 단위로 펼친 뒤 검증한다.
    weather_rows = extract_weather_rows(raw_data["weather"])
    weather_records, weather_errors = validate_many(
        WeatherHourRecord, weather_rows, "weather"
    )
    # country/ip는 원래 단일 dict라서, validate_many가 요구하는 "행 리스트" 형태로
    # 맞추기 위해 1건짜리 리스트로 감싸서 넘긴다.
    country_records, country_errors = validate_many(
        CountryInfo, [raw_data["country"]], "country"
    )
    ip_records, ip_errors = validate_many(IPInfo, [raw_data["ip"]], "ip")

    # 3개 소스의 오류를 한데 모아, 하나라도 있으면 파이프라인을 여기서 멈춘다.
    # (검증 실패 데이터를 그대로 저장 단계로 흘려보내지 않기 위한 안전장치)
    validation_errors = weather_errors + country_errors + ip_errors
    if validation_errors:
        save_json(validation_errors, VALIDATION_ERROR_OUTPUT)
        raise RuntimeError("검증 오류가 발생했습니다. validation_errors.json을 확인하세요.")

    # 이전 실행에서 남은 오류 파일이 있다면 정리한다 (이번엔 오류가 없었다는 뜻이므로).
    if VALIDATION_ERROR_OUTPUT.exists():
        VALIDATION_ERROR_OUTPUT.unlink()

    print(
        "검증 완료:",
        f"weather={len(weather_records)},",
        f"country={len(country_records)},",
        f"ip={len(ip_records)}",
    )

    print("\n=== 3. CSV / Parquet 저장 및 성능 측정 ===")
    # 소스마다 스키마가 달라(weather=72행, country/ip=1행) 하나의 표로 합치지 않고
    # save_with_performance()를 세 번 호출해 각각 별도 CSV/Parquet 쌍으로 저장한다.
    weather_perf = save_with_performance(weather_records, WEATHER_CSV, WEATHER_PARQUET)
    weather_perf["name"] = "weather"  # 나중에 performance_result.json에서 구분하기 위한 라벨
    country_perf = save_with_performance(country_records, COUNTRY_CSV, COUNTRY_PARQUET)
    country_perf["name"] = "country"
    ip_perf = save_with_performance(ip_records, IP_CSV, IP_PARQUET)
    ip_perf["name"] = "ip"

    for perf in (weather_perf, country_perf, ip_perf):
        print(
            f"[{perf['name']}] rows={perf['rows']} "
            f"CSV={perf['csv_seconds']}s({perf['csv_bytes']}B) "
            f"Parquet={perf['parquet_seconds']}s({perf['parquet_bytes']}B)"
        )

    save_json([weather_perf, country_perf, ip_perf], PERFORMANCE_OUTPUT)

    print("\n=== 4. 저장 결과 재로딩 검증 ===")
    # weather는 datetime 컬럼이라 CSV/Parquet 왕복 시 문자열 표현이 달라질 수 있어
    # 건수만 비교하고, country/ip는 대표 컬럼 값까지 정확히 일치하는지 확인한다.
    weather_csv_rows, weather_parquet_rows = verify_saved_data(WEATHER_CSV, WEATHER_PARQUET)
    country_csv_rows, country_parquet_rows = verify_saved_data(
        COUNTRY_CSV, COUNTRY_PARQUET, key_column="name"
    )
    ip_csv_rows, ip_parquet_rows = verify_saved_data(IP_CSV, IP_PARQUET, key_column="query")

    print(f"재로딩 완료: weather CSV={weather_csv_rows}건/Parquet={weather_parquet_rows}건")
    print(f"재로딩 완료: country CSV={country_csv_rows}건/Parquet={country_parquet_rows}건")
    print(f"재로딩 완료: ip CSV={ip_csv_rows}건/Parquet={ip_parquet_rows}건")

    print("\n=== 5. 완료 ===")
    print(f"출력 폴더: {OUTPUT_DIR}")
    print(f"성능 결과: {PERFORMANCE_OUTPUT}")


def main() -> None:
    """예외를 사용자에게 알기 쉬운 메시지로 출력합니다.

    예외 종류별로 분기하는 이유: 스택 트레이스만 보여주는 대신, 원인 범주
    (API 문제 / 패키지 미설치 / 그 외 실행 오류)를 한눈에 알 수 있는 메시지를
    먼저 보여주고, SystemExit(1)로 종료 코드를 비정상(1)으로 남긴다.
    (이 종료 코드는 scripts/preflight_check.py의 run_command()가 실패 판정에 사용한다.)
    """
    try:
        asyncio.run(run_pipeline())
    except ApiFetchError as exc:
        # HTTP 상태 오류/네트워크 오류가 api_client.py에서 여기까지 하나로 통일돼 온다.
        print(f"[API 오류] {exc}")
        raise SystemExit(1) from exc
    except ImportError as exc:
        # httpx/pydantic/pandas/pyarrow 중 하나라도 미설치면 여기서 잡힌다.
        print("[의존성 오류] requirements.txt를 다시 설치하세요.")
        raise SystemExit(1) from exc
    except (OSError, RuntimeError, ValueError) as exc:
        # 파일 I/O 실패(OSError), 검증 오류로 인한 의도적 중단(RuntimeError),
        # CSV/Parquet 불일치(ValueError) 등 나머지 실행 중 오류를 포괄한다.
        print(f"[실행 오류] {exc}")
        raise SystemExit(1) from exc


if __name__ == "__main__":
    main()
