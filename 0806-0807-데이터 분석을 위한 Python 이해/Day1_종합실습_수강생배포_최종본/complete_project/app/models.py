"""API 응답과 최종 집계 결과의 Pydantic 모델입니다."""

from pydantic import BaseModel, ConfigDict, Field


class Company(BaseModel):
    """사용자 소속 회사입니다."""

    model_config = ConfigDict(strict=True)

    name: str = Field(min_length=1)


class User(BaseModel):
    """사용자 API 응답에서 실습에 필요한 필드만 검증합니다."""

    model_config = ConfigDict(strict=True)

    id: int = Field(gt=0)
    name: str = Field(min_length=1)
    email: str = Field(
        min_length=3,
        pattern=r"^[^@\s]+@[^@\s]+\.[^@\s]+$",
    )
    company: Company


class Post(BaseModel):
    """게시물 API 응답입니다."""

    model_config = ConfigDict(
        populate_by_name=True,
        strict=True,
    )

    user_id: int = Field(alias="userId", gt=0)
    id: int = Field(gt=0)
    title: str = Field(min_length=1)
    body: str = Field(min_length=1)


class Todo(BaseModel):
    """할 일 API 응답입니다."""

    model_config = ConfigDict(
        populate_by_name=True,
        strict=True,
    )

    user_id: int = Field(alias="userId", gt=0)
    id: int = Field(gt=0)
    title: str = Field(min_length=1)
    completed: bool


class UserActivity(BaseModel):
    """사용자별 게시물과 할 일 통계를 저장합니다."""

    model_config = ConfigDict(strict=True)

    user_id: int = Field(gt=0)
    name: str = Field(min_length=1)
    email: str
    company: str
    post_count: int = Field(ge=0)
    todo_count: int = Field(ge=0)
    completed_todo_count: int = Field(ge=0)
    completion_rate: float = Field(ge=0, le=100)
