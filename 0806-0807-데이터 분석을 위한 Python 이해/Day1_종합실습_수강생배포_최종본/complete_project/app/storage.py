"""집계 결과를 CSV와 Parquet로 저장하고 성능을 측정합니다."""

import json
from pathlib import Path
from time import perf_counter
from typing import Any

import pandas as pd

from app.models import UserActivity


def save_json(data: Any, file_path: Path) -> None:
    """딕셔너리 또는 리스트를 JSON 파일로 저장합니다."""
    file_path.parent.mkdir(parents=True, exist_ok=True)
    with file_path.open("w", encoding="utf-8") as file:
        json.dump(data, file, ensure_ascii=False, indent=2)


def save_with_performance(
    records: list[UserActivity],
    csv_path: Path,
    parquet_path: Path,
    performance_path: Path,
) -> dict[str, float | int]:
    """CSV와 Parquet 저장 시간, 파일 크기를 측정합니다."""
    csv_path.parent.mkdir(parents=True, exist_ok=True)

    dataframe = pd.DataFrame(
        [record.model_dump() for record in records]
    )

    csv_start = perf_counter()
    dataframe.to_csv(csv_path, index=False, encoding="utf-8-sig")
    csv_seconds = perf_counter() - csv_start

    parquet_start = perf_counter()
    dataframe.to_parquet(
        parquet_path,
        index=False,
        engine="pyarrow",
        compression="snappy",
    )
    parquet_seconds = perf_counter() - parquet_start

    performance: dict[str, float | int] = {
        "rows": len(dataframe),
        "csv_seconds": round(csv_seconds, 6),
        "parquet_seconds": round(parquet_seconds, 6),
        "csv_bytes": csv_path.stat().st_size,
        "parquet_bytes": parquet_path.stat().st_size,
    }

    save_json(performance, performance_path)
    return performance


def verify_saved_data(
    csv_path: Path,
    parquet_path: Path,
) -> tuple[int, int]:
    """CSV와 Parquet를 다시 읽고 행 수와 사용자 번호를 비교합니다."""
    csv_data = pd.read_csv(csv_path)
    parquet_data = pd.read_parquet(
        parquet_path,
        engine="pyarrow",
    )

    csv_rows = len(csv_data)
    parquet_rows = len(parquet_data)

    if csv_rows != parquet_rows:
        raise ValueError("CSV와 Parquet의 행 수가 다릅니다.")

    if csv_data["user_id"].tolist() != parquet_data["user_id"].tolist():
        raise ValueError("CSV와 Parquet의 사용자 번호가 다릅니다.")

    return csv_rows, parquet_rows
