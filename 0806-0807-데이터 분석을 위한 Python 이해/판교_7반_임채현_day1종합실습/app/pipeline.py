# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [Day 1 종합실습] API 응답을 검증하고 오류를 분리
#               Open-Meteo의 열(컬럼) 지향 hourly 데이터를 행(레코드) 단위로
#               펼친 뒤, 3개 소스(weather/country/ip) 모두 동일한 validate_many()
#               제네릭 함수로 Pydantic 검증 -> valid/errors 분리를 수행한다.
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, extract_weather_rows + validate_many 구현
#
# ------------------------------------------------------------------
"""API 응답을 검증하고 정상/오류 데이터로 분리합니다."""

from __future__ import annotations

from datetime import datetime
from typing import Any, TypeVar

from pydantic import BaseModel, ValidationError

# Python 3.11은 PEP 695(def f[T](...)) 제네릭 문법을 지원하지 않으므로
# 전통적인 TypeVar 방식으로 "여러 모델 타입에 재사용 가능한" 함수를 만든다.
ModelT = TypeVar("ModelT", bound=BaseModel)


def extract_weather_rows(raw: dict[str, Any]) -> list[dict[str, Any]]:
    """Open-Meteo의 열(컬럼) 지향 hourly 데이터를 행(레코드) 리스트로 변환합니다.

    원본: {"hourly": {"time": [...], "temperature_2m": [...], ...}}
    변환 후: [{"time": <datetime>, "temperature_2m": ..., "precipitation_probability": ...}, ...]
    """
    hourly = raw.get("hourly", {})
    times = hourly.get("time", [])
    temperatures = hourly.get("temperature_2m", [])
    precipitations = hourly.get("precipitation_probability", [])

    rows: list[dict[str, Any]] = []
    # zip()으로 같은 인덱스의 세 배열을 한 시간 단위로 묶어 순회한다.
    for time_value, temperature, precipitation in zip(
        times, temperatures, precipitations, strict=True
    ):
        rows.append(
            {
                # WeatherHourRecord가 strict 모드라 문자열을 그대로 못 받으므로
                # 여기서 미리 datetime 객체로 변환해 둔다.
                "time": datetime.fromisoformat(time_value),
                "temperature_2m": temperature,
                "precipitation_probability": precipitation,
            }
        )

    return rows


def validate_many(
    model_class: type[ModelT],
    rows: list[dict[str, Any]],
    source_name: str,
) -> tuple[list[ModelT], list[dict[str, Any]]]:
    """여러 행을 검증하고 정상 목록과 오류 목록으로 분리합니다.

    weather(72건)처럼 여러 행이든, country/ip처럼 1건짜리 리스트든
    똑같이 이 함수 하나로 처리할 수 있어 소스별로 검증 코드를 반복하지 않는다.
    """
    valid: list[ModelT] = []
    errors: list[dict[str, Any]] = []

    for index, row in enumerate(rows, start=1):
        try:
            valid.append(model_class.model_validate(row))
        except ValidationError as exc:
            # 체크포인트: ValidationError 발생 시 오류 내용을 출력한다.
            print(f"[VALIDATION ERROR] {source_name}[{index}] -> {exc}")
            errors.append(
                {
                    "source": source_name,
                    "index": index,
                    "errors": exc.errors(include_url=False),
                }
            )

    return valid, errors
