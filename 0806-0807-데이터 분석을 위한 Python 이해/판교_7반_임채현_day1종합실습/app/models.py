# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [Day 1 종합실습] 3개 API 응답을 검증하기 위한 Pydantic v2 모델
#               strict=True로 타입 자동 변환을 막아, 문자열로 온 숫자 등
#               "그럴듯하지만 실제로는 잘못된" 데이터를 확실히 걸러낸다.
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, WeatherHourRecord/CountryInfo/IPInfo 3종 모델 정의 (strict 모드)
#
# ------------------------------------------------------------------
"""API 응답을 검증하는 Pydantic 모델입니다."""

from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class WeatherHourRecord(BaseModel):
    """Open-Meteo 응답의 hourly 배열 한 시간 단위를 표현하는 레코드.

    원본 응답은 {"hourly": {"time": [...], "temperature_2m": [...], ...}}처럼
    "컬럼 지향(열 배열)" 구조라서, api_client.py에서 zip으로 한 시간씩 묶어
    이 모델 하나하나로 변환(=행 지향으로 전환)한 뒤 검증한다.
    """

    model_config = ConfigDict(strict=True)

    # ISO8601 문자열("2026-08-06T00:00")도 strict 모드에서 datetime으로 파싱된다.
    time: datetime
    # 기온(섭씨). strict 모드에서도 int 입력은 float로 안전하게 승격된다.
    temperature_2m: float
    # 강수확률(%)은 정의상 0~100 사이여야 하므로 ge/le로 범위를 강제한다.
    precipitation_probability: int = Field(ge=0, le=100)


class CountryInfo(BaseModel):
    """Countries.dev 응답 중 실습에 필요한 필드만 추린 국가 정보 모델."""

    model_config = ConfigDict(strict=True)

    name: str = Field(min_length=1)
    capital: str = Field(min_length=1)
    region: str = Field(min_length=1)
    subregion: str = Field(min_length=1)
    # 인구/면적은 0보다 커야 의미가 있는 값이므로 gt=0으로 검증.
    population: int = Field(gt=0)
    area: float = Field(gt=0)
    timezones: list[str]


class IPInfo(BaseModel):
    """ip-api 응답 중 실습에 필요한 필드만 추린 IP 위치 정보 모델."""

    model_config = ConfigDict(populate_by_name=True, strict=True)

    query: str = Field(min_length=1)  # 조회한 IP 주소
    country: str = Field(min_length=1)
    # ip-api 응답의 실제 키(countryCode, regionName)는 camelCase라서 alias로 매핑한다.
    country_code: str = Field(min_length=1, alias="countryCode")
    region_name: str = Field(min_length=1, alias="regionName")
    city: str = Field(min_length=1)
    # 위도/경도는 지구 좌표 범위를 벗어나면 명백히 잘못된 데이터이므로 범위를 둔다.
    lat: float = Field(ge=-90, le=90)
    lon: float = Field(ge=-180, le=180)
    isp: str = Field(min_length=1)
