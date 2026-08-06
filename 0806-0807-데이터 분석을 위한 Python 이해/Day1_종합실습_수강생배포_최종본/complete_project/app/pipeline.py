"""API 응답을 검증하고 사용자별 활동 통계로 집계합니다."""

from collections import Counter
from typing import Any

from pydantic import BaseModel, ValidationError

from app.models import Post, Todo, User, UserActivity


def validate_many[ModelT: BaseModel](
    model_class: type[ModelT],
    rows: list[dict[str, Any]],
    source_name: str,
) -> tuple[list[ModelT], list[dict[str, Any]]]:
    """여러 행을 검증하고 정상 목록과 오류 목록으로 분리합니다."""
    valid: list[ModelT] = []
    errors: list[dict[str, Any]] = []

    for index, row in enumerate(rows, start=1):
        try:
            valid.append(model_class.model_validate(row))
        except ValidationError as exc:
            errors.append(
                {
                    "source": source_name,
                    "index": index,
                    "errors": exc.errors(include_url=False),
                }
            )

    return valid, errors


def build_user_activity(
    users: list[User],
    posts: list[Post],
    todos: list[Todo],
) -> list[UserActivity]:
    """사용자별 게시물 수, 할 일 수, 완료율을 계산합니다."""
    post_counts = Counter(post.user_id for post in posts)
    todo_counts = Counter(todo.user_id for todo in todos)
    completed_counts = Counter(
        todo.user_id for todo in todos if todo.completed
    )

    result: list[UserActivity] = []

    for user in users:
        todo_count = todo_counts[user.id]
        completed_count = completed_counts[user.id]
        completion_rate = (
            round(completed_count / todo_count * 100, 2)
            if todo_count
            else 0.0
        )

        result.append(
            UserActivity(
                user_id=user.id,
                name=user.name,
                email=user.email,
                company=user.company.name,
                post_count=post_counts[user.id],
                todo_count=todo_count,
                completed_todo_count=completed_count,
                completion_rate=completion_rate,
            )
        )

    return result
