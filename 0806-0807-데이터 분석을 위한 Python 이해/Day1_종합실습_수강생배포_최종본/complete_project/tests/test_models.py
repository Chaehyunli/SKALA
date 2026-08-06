"""Pydantic 모델 검증 테스트입니다."""

import pytest
from pydantic import ValidationError

from app.models import User


def test_user_rejects_invalid_email() -> None:
    """이메일 형식이 잘못되면 ValidationError가 발생합니다."""
    raw_user = {
        "id": 1,
        "name": "홍길동",
        "email": "wrong-email",
        "company": {"name": "Example"},
    }

    with pytest.raises(ValidationError):
        User.model_validate(raw_user)


def test_user_rejects_string_id() -> None:
    """엄격 모드에서는 문자열 ID를 정수로 자동 변환하지 않습니다."""
    raw_user = {
        "id": "1",
        "name": "홍길동",
        "email": "hong@example.com",
        "company": {"name": "Example"},
    }

    with pytest.raises(ValidationError):
        User.model_validate(raw_user)
