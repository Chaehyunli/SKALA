"""사용자 활동 집계 테스트입니다."""

from app.models import Company, Post, Todo, User
from app.pipeline import build_user_activity


def test_build_user_activity() -> None:
    """게시물 수와 할 일 완료율을 정확히 계산합니다."""
    users = [
        User(
            id=1,
            name="홍길동",
            email="hong@example.com",
            company=Company(name="Example"),
        )
    ]
    posts = [
        Post(userId=1, id=1, title="A", body="본문"),
        Post(userId=1, id=2, title="B", body="본문"),
    ]
    todos = [
        Todo(userId=1, id=1, title="할 일 1", completed=True),
        Todo(userId=1, id=2, title="할 일 2", completed=True),
        Todo(userId=1, id=3, title="할 일 3", completed=False),
    ]

    result = build_user_activity(users, posts, todos)

    assert result[0].post_count == 2
    assert result[0].todo_count == 3
    assert result[0].completed_todo_count == 2
    assert result[0].completion_rate == 66.67
