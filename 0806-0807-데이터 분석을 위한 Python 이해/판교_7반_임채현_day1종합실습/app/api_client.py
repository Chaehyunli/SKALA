# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [Day 1 종합실습] HTTPX로 3개 API를 비동기로 수집
#               httpx.AsyncClient + asyncio.gather()로 Open-Meteo/Countries.dev/
#               ip-api를 동시에 호출하고, HTTP 오류/네트워크 오류를 ApiFetchError로
#               통일해서 상위 계층(main.py)이 한 가지 예외만 처리하면 되게 한다.
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, fetch_json + fetch_all_data 구현
#
# ------------------------------------------------------------------
"""HTTPX로 3개 API를 비동기로 수집합니다."""

from __future__ import annotations

import asyncio
from typing import Any

import httpx

from app.config import COUNTRY_URL, IP_URL, REQUEST_TIMEOUT_SECONDS, WEATHER_URL


class ApiFetchError(RuntimeError):
    """API 요청이나 응답 형식에 문제가 있을 때 발생합니다."""


async def fetch_json(client: httpx.AsyncClient, url: str) -> dict[str, Any]:
    """한 개 API를 호출하고 JSON을 dict로 반환합니다.

    httpx.HTTPStatusError(4xx/5xx)와 httpx.RequestError(타임아웃, 연결 실패 등)를
    구분해서 각각 다른 메시지로 ApiFetchError를 발생시킨다. 이렇게 하나의
    예외 타입으로 통일하면, 호출하는 쪽(fetch_all_data/main)은 ApiFetchError만
    잡으면 되고 httpx 내부 예외 클래스를 알 필요가 없다.
    """
    try:
        response = await client.get(url, timeout=REQUEST_TIMEOUT_SECONDS)
        response.raise_for_status()  # 4xx/5xx면 HTTPStatusError를 발생시킴
        data = response.json()
    except httpx.HTTPStatusError as exc:
        raise ApiFetchError(f"HTTP 오류: {exc.response.status_code} {url}") from exc
    except httpx.RequestError as exc:
        raise ApiFetchError(f"네트워크 오류: {url} - {exc}") from exc
    except ValueError as exc:
        raise ApiFetchError(f"JSON 변환 오류: {url}") from exc

    if not isinstance(data, dict):
        raise ApiFetchError(f"예상하지 못한 응답 형식: {url}")

    return data


async def fetch_all_data(client: httpx.AsyncClient) -> dict[str, dict[str, Any]]:
    """날씨/국가/IP 3개 API를 asyncio.gather()로 동시에 요청합니다.

    gather()에 코루틴 3개를 동시에 넘기면, 하나가 응답을 기다리는 동안
    다른 요청도 병렬로 진행된다 (순차 실행 대비 총 대기 시간이 크게 줄어듦).
    """
    weather, country, ip = await asyncio.gather(
        fetch_json(client, WEATHER_URL),
        fetch_json(client, COUNTRY_URL),
        fetch_json(client, IP_URL),
    )

    return {"weather": weather, "country": country, "ip": ip}
