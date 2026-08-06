# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [Day 1 종합실습] app/pipeline.py의 검증/변환 로직 테스트
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, extract_weather_rows + validate_many 테스트 작성
#
# ------------------------------------------------------------------
"""검증 파이프라인(extract_weather_rows, validate_many) 테스트입니다."""

from datetime import datetime

from app.models import CountryInfo, WeatherHourRecord
from app.pipeline import extract_weather_rows, validate_many


def test_extract_weather_rows_zips_columns_into_rows() -> None:
    """hourly의 열(컬럼) 배열 3개를 같은 인덱스끼리 묶어 행으로 펼쳐야 한다."""
    raw = {
        "hourly": {
            "time": ["2026-08-06T00:00", "2026-08-06T01:00"],
            "temperature_2m": [28.5, 27.0],
            "precipitation_probability": [40, 50],
        }
    }

    rows = extract_weather_rows(raw)

    assert len(rows) == 2
    assert rows[0]["time"] == datetime(2026, 8, 6, 0, 0)
    assert rows[0]["temperature_2m"] == 28.5
    assert rows[1]["precipitation_probability"] == 50


def test_validate_many_separates_valid_and_invalid_rows() -> None:
    """정상 행은 valid에, 검증 실패 행은 errors에 각각 담겨야 한다."""
    rows = [
        {
            "time": datetime(2026, 8, 6, 0, 0),
            "temperature_2m": 28.5,
            "precipitation_probability": 40,
        },
        {
            "time": datetime(2026, 8, 6, 1, 0),
            "temperature_2m": 27.0,
            "precipitation_probability": 150,  # 범위(0~100) 초과 -> 실패해야 정상
        },
    ]

    valid, errors = validate_many(WeatherHourRecord, rows, "weather")

    assert len(valid) == 1
    assert len(errors) == 1
    assert errors[0]["source"] == "weather"
    assert errors[0]["index"] == 2


def test_validate_many_wraps_single_dict_source() -> None:
    """country/ip처럼 원본이 단일 dict라도 1건짜리 리스트로 감싸면 그대로 검증된다."""
    raw_country = {
        "name": "Korea (Republic of)",
        "capital": "Seoul",
        "region": "Asia",
        "subregion": "Eastern Asia",
        "population": 51780579,
        "area": 100210,
        "timezones": ["UTC+09:00"],
    }

    valid, errors = validate_many(CountryInfo, [raw_country], "country")

    assert len(valid) == 1
    assert len(errors) == 0
    assert valid[0].name == "Korea (Republic of)"
