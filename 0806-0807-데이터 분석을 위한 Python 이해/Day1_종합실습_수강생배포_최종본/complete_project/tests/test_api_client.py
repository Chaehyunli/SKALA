"""실제 인터넷을 사용하지 않는 비동기 API 테스트입니다."""

import httpx
import pytest

from app.api_client import fetch_all_data
from app.config import BASE_URL


@pytest.mark.asyncio
async def test_fetch_all_data() -> None:
    """users, posts, todos 경로를 모두 호출하는지 확인합니다."""
    payloads = {
        "/users": [{"id": 1}],
        "/posts": [{"id": 1}],
        "/todos": [{"id": 1}],
    }
    called_paths: list[str] = []

    def handler(request: httpx.Request) -> httpx.Response:
        called_paths.append(request.url.path)
        return httpx.Response(
            status_code=200,
            json=payloads[request.url.path],
        )

    transport = httpx.MockTransport(handler)

    async with httpx.AsyncClient(
        base_url=BASE_URL,
        transport=transport,
    ) as client:
        result = await fetch_all_data(client)

    assert set(called_paths) == {"/users", "/posts", "/todos"}
    assert result["users"] == [{"id": 1}]
    assert result["posts"] == [{"id": 1}]
    assert result["todos"] == [{"id": 1}]
