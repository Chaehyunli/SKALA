"""저장과 재로딩 검증 로직 테스트입니다."""

from pathlib import Path

import pandas as pd

from app.models import UserActivity
from app.storage import save_with_performance, verify_saved_data


def test_save_and_verify_with_mocked_parquet(
    tmp_path: Path,
    monkeypatch,
) -> None:
    """PyArrow 없이도 저장 함수의 연결 관계를 검사합니다."""
    records = [
        UserActivity(
            user_id=1,
            name="홍길동",
            email="hong@example.com",
            company="Example",
            post_count=2,
            todo_count=3,
            completed_todo_count=2,
            completion_rate=66.67,
        )
    ]

    csv_path = tmp_path / "result.csv"
    parquet_path = tmp_path / "result.parquet"
    performance_path = tmp_path / "performance.json"

    def fake_to_parquet(
        dataframe,
        path,
        **kwargs,
    ) -> None:
        dataframe.to_json(path, orient="records", force_ascii=False)

    def fake_read_parquet(
        path,
        **kwargs,
    ):
        return pd.read_json(path)

    monkeypatch.setattr(pd.DataFrame, "to_parquet", fake_to_parquet)
    monkeypatch.setattr(pd, "read_parquet", fake_read_parquet)

    performance = save_with_performance(
        records,
        csv_path,
        parquet_path,
        performance_path,
    )
    csv_rows, parquet_rows = verify_saved_data(
        csv_path,
        parquet_path,
    )

    assert performance["rows"] == 1
    assert csv_rows == 1
    assert parquet_rows == 1
    assert performance_path.exists()
