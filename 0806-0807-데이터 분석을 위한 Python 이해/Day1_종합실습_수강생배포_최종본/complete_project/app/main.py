"""Day 1 종합실습 전체 파이프라인을 실행합니다."""

import asyncio

import httpx

from app.api_client import ApiFetchError, fetch_all_data
from app.config import (
    BASE_URL,
    CSV_OUTPUT,
    PARQUET_OUTPUT,
    PERFORMANCE_OUTPUT,
    VALIDATION_ERROR_OUTPUT,
)
from app.models import Post, Todo, User
from app.pipeline import build_user_activity, validate_many
from app.storage import (
    save_json,
    save_with_performance,
    verify_saved_data,
)


async def run_pipeline() -> None:
    """수집, 검증, 집계, 저장을 순서대로 실행합니다."""
    print("=== 1. API 3개 동시 수집 ===")

    async with httpx.AsyncClient(
        base_url=BASE_URL,
        timeout=15.0,
        follow_redirects=True,
    ) as client:
        raw_data = await fetch_all_data(client)

    print(
        "수집 완료:",
        f"users={len(raw_data['users'])},",
        f"posts={len(raw_data['posts'])},",
        f"todos={len(raw_data['todos'])}",
    )

    print("\n=== 2. Pydantic v2 검증 ===")
    users, user_errors = validate_many(
        User,
        raw_data["users"],
        "users",
    )
    posts, post_errors = validate_many(
        Post,
        raw_data["posts"],
        "posts",
    )
    todos, todo_errors = validate_many(
        Todo,
        raw_data["todos"],
        "todos",
    )

    validation_errors = user_errors + post_errors + todo_errors
    if validation_errors:
        save_json(validation_errors, VALIDATION_ERROR_OUTPUT)
        raise RuntimeError(
            "검증 오류가 발생했습니다. validation_errors.json을 확인하세요."
        )

    if VALIDATION_ERROR_OUTPUT.exists():
        VALIDATION_ERROR_OUTPUT.unlink()

    print(
        "검증 완료:",
        f"users={len(users)},",
        f"posts={len(posts)},",
        f"todos={len(todos)}",
    )

    print("\n=== 3. 사용자 활동 집계 ===")
    activity = build_user_activity(users, posts, todos)
    print(f"집계 완료: {len(activity)}건")

    print("\n=== 4. CSV / Parquet 저장 및 성능 측정 ===")
    performance = save_with_performance(
        activity,
        CSV_OUTPUT,
        PARQUET_OUTPUT,
        PERFORMANCE_OUTPUT,
    )

    print(f"CSV 저장 시간: {performance['csv_seconds']}초")
    print(f"Parquet 저장 시간: {performance['parquet_seconds']}초")
    print(f"CSV 파일 크기: {performance['csv_bytes']} bytes")
    print(f"Parquet 파일 크기: {performance['parquet_bytes']} bytes")

    print("\n=== 5. 저장 결과 재로딩 검증 ===")
    csv_rows, parquet_rows = verify_saved_data(
        CSV_OUTPUT,
        PARQUET_OUTPUT,
    )
    print(f"재로딩 완료: CSV={csv_rows}건, Parquet={parquet_rows}건")

    print("\n=== 6. 완료 ===")
    print(f"CSV: {CSV_OUTPUT}")
    print(f"Parquet: {PARQUET_OUTPUT}")
    print(f"성능 결과: {PERFORMANCE_OUTPUT}")


def main() -> None:
    """예외를 사용자에게 알기 쉬운 메시지로 출력합니다."""
    try:
        asyncio.run(run_pipeline())
    except ApiFetchError as exc:
        print(f"[API 오류] {exc}")
        raise SystemExit(1) from exc
    except ImportError as exc:
        print(
            "[의존성 오류] requirements.txt를 다시 설치하세요."
        )
        raise SystemExit(1) from exc
    except (OSError, RuntimeError, ValueError) as exc:
        print(f"[실행 오류] {exc}")
        raise SystemExit(1) from exc


if __name__ == "__main__":
    main()
