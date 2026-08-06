# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [Day 1 종합실습] 실제 인터넷을 사용하지 않는 비동기 API 테스트
#               httpx.MockTransport로 3개 API 응답을 가짜로 만들어, 실제 네트워크
#               호출 없이 fetch_all_data()가 3개 URL을 모두 호출하는지 확인한다.
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, MockTransport 기반 test_fetch_all_data 작성
#
# ------------------------------------------------------------------
"""실제 인터넷을 사용하지 않는 비동기 API 테스트입니다."""

import httpx
import pytest

from app.api_client import ApiFetchError, fetch_all_data, fetch_json
from app.config import COUNTRY_URL, IP_URL, WEATHER_URL


def _make_handler():
    """호출된 host별로 최소한의 가짜 응답을 돌려주는 핸들러를 만든다."""
    called_hosts: list[str] = []

    def handler(request: httpx.Request) -> httpx.Response:
        called_hosts.append(request.url.host)

        if request.url.host == "api.open-meteo.com":
            payload = {
                "hourly": {
                    "time": ["2026-08-06T00:00"],
                    "temperature_2m": [28.5],
                    "precipitation_probability": [40],
                }
            }
        elif request.url.host == "countries.dev":
            payload = {"name": "Korea (Republic of)"}
        else:
            payload = {"query": "8.8.8.8"}

        return httpx.Response(status_code=200, json=payload)

    return handler, called_hosts


@pytest.mark.asyncio
async def test_fetch_all_data_calls_all_three_hosts() -> None:
    """weather/country/ip 3개 URL을 모두 호출하는지 확인합니다."""
    handler, called_hosts = _make_handler()
    transport = httpx.MockTransport(handler)

    async with httpx.AsyncClient(transport=transport) as client:
        result = await fetch_all_data(client)

    assert set(called_hosts) == {
        "api.open-meteo.com",
        "countries.dev",
        "ip-api.com",
    }
    assert result["weather"]["hourly"]["temperature_2m"] == [28.5]
    assert result["country"]["name"] == "Korea (Republic of)"
    assert result["ip"]["query"] == "8.8.8.8"


@pytest.mark.asyncio
async def test_fetch_json_raises_api_fetch_error_on_http_status_error() -> None:
    """4xx/5xx 응답이면 httpx 예외 대신 ApiFetchError로 통일해서 발생시켜야 한다."""

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(status_code=500, json={"error": "boom"})

    transport = httpx.MockTransport(handler)

    async with httpx.AsyncClient(transport=transport) as client:
        with pytest.raises(ApiFetchError):
            await fetch_json(client, WEATHER_URL)


@pytest.mark.asyncio
async def test_fetch_json_raises_api_fetch_error_on_non_dict_response() -> None:
    """응답이 dict가 아니면(JSON 배열 등) ApiFetchError를 발생시켜야 한다."""

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(status_code=200, json=[1, 2, 3])

    transport = httpx.MockTransport(handler)

    async with httpx.AsyncClient(transport=transport) as client:
        with pytest.raises(ApiFetchError):
            await fetch_json(client, COUNTRY_URL)


@pytest.mark.asyncio
async def test_fetch_json_success_returns_dict() -> None:
    """정상 200 응답이면 JSON을 dict 그대로 반환해야 한다."""

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(status_code=200, json={"query": "8.8.8.8"})

    transport = httpx.MockTransport(handler)

    async with httpx.AsyncClient(transport=transport) as client:
        data = await fetch_json(client, IP_URL)

    assert data == {"query": "8.8.8.8"}
