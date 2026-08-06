# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [Day 1 종합실습] app/models.py의 Pydantic strict 스키마 검증 테스트
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, WeatherHourRecord/CountryInfo/IPInfo 정상/비정상 케이스 작성
#
# ------------------------------------------------------------------
"""Pydantic 모델 검증 테스트입니다."""

from datetime import datetime

import pytest
from pydantic import ValidationError

from app.models import CountryInfo, IPInfo, WeatherHourRecord


def test_weather_hour_record_valid() -> None:
    """정상 범위의 값이면 WeatherHourRecord가 예외 없이 생성돼야 한다."""
    record = WeatherHourRecord(
        time=datetime(2026, 8, 6, 0, 0),
        temperature_2m=28.5,
        precipitation_probability=40,
    )
    assert record.temperature_2m == 28.5


def test_weather_hour_record_rejects_string_time() -> None:
    """strict 모드에서는 ISO 문자열을 datetime으로 자동 변환하지 않는다."""
    with pytest.raises(ValidationError):
        WeatherHourRecord(
            time="2026-08-06T00:00",  # strict 모드에서 문자열 -> datetime 자동 변환 안 됨
            temperature_2m=28.5,
            precipitation_probability=40,
        )


def test_weather_hour_record_invalid_precipitation_raises() -> None:
    """precipitation_probability는 0~100 범위여야 하므로, 120은 ValidationError여야 한다."""
    with pytest.raises(ValidationError):
        WeatherHourRecord(
            time=datetime(2026, 8, 6, 0, 0),
            temperature_2m=28.5,
            precipitation_probability=120,  # 범위(0~100) 초과 -> 실패해야 정상
        )


def test_country_info_valid() -> None:
    """실제 Countries.dev 응답 형태로 정상 생성돼야 한다."""
    country = CountryInfo(
        name="Korea (Republic of)",
        capital="Seoul",
        region="Asia",
        subregion="Eastern Asia",
        population=51780579,
        area=100210,
        timezones=["UTC+09:00"],
    )
    assert country.name == "Korea (Republic of)"


def test_country_info_invalid_population_raises() -> None:
    """population은 0보다 커야 하므로, 0 이하 값은 ValidationError여야 한다."""
    with pytest.raises(ValidationError):
        CountryInfo(
            name="Korea (Republic of)",
            capital="Seoul",
            region="Asia",
            subregion="Eastern Asia",
            population=0,  # gt=0 위반 -> 실패해야 정상
            area=100210,
            timezones=["UTC+09:00"],
        )


def test_ip_info_valid_with_camel_case_alias() -> None:
    """ip-api 응답의 실제 camelCase 키(countryCode, regionName)로도 생성돼야 한다."""
    ip_info = IPInfo(
        query="8.8.8.8",
        country="United States",
        countryCode="US",
        regionName="Virginia",
        city="Ashburn",
        lat=39.03,
        lon=-77.5,
        isp="Google LLC",
    )
    assert ip_info.country_code == "US"
    assert ip_info.region_name == "Virginia"


def test_ip_info_invalid_latitude_raises() -> None:
    """위도(lat)는 -90~90 범위여야 하므로, 범위를 벗어나면 ValidationError여야 한다."""
    with pytest.raises(ValidationError):
        IPInfo(
            query="8.8.8.8",
            country="United States",
            countryCode="US",
            regionName="Virginia",
            city="Ashburn",
            lat=999,  # 범위(-90~90) 초과 -> 실패해야 정상
            lon=-77.5,
            isp="Google LLC",
        )
