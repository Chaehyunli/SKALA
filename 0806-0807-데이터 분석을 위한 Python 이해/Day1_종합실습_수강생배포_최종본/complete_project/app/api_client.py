"""HTTPX로 3개 API를 비동기로 수집합니다."""

import asyncio
from typing import Any

import httpx

from app.config import POSTS_ENDPOINT, TODOS_ENDPOINT, USERS_ENDPOINT


class ApiFetchError(RuntimeError):
    """API 요청이나 응답 형식에 문제가 있을 때 발생합니다."""


async def fetch_json(
    client: httpx.AsyncClient,
    endpoint: str,
) -> list[dict[str, Any]]:
    """한 개 API를 호출하고 JSON 배열을 반환합니다."""
    try:
        response = await client.get(endpoint)
        response.raise_for_status()
        data = response.json()
    except httpx.HTTPStatusError as exc:
        raise ApiFetchError(
            f"HTTP 오류: {exc.response.status_code} {endpoint}"
        ) from exc
    except httpx.RequestError as exc:
        raise ApiFetchError(f"네트워크 오류: {endpoint} - {exc}") from exc
    except ValueError as exc:
        raise ApiFetchError(f"JSON 변환 오류: {endpoint}") from exc

    if not isinstance(data, list):
        raise ApiFetchError(f"예상하지 못한 응답 형식: {endpoint}")

    return data


async def fetch_all_data(
    client: httpx.AsyncClient,
) -> dict[str, list[dict[str, Any]]]:
    """users, posts, todos API를 동시에 요청합니다."""
    users, posts, todos = await asyncio.gather(
        fetch_json(client, USERS_ENDPOINT),
        fetch_json(client, POSTS_ENDPOINT),
        fetch_json(client, TODOS_ENDPOINT),
    )

    return {
        "users": users,
        "posts": posts,
        "todos": todos,
    }
