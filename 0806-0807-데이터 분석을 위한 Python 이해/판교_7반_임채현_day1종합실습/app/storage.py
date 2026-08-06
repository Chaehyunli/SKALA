# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [Day 1 종합실습] 검증된 데이터를 CSV/Parquet로 저장하고 성능 측정
#               레코드 리스트를 CSV와 Parquet 두 형식으로 저장하면서 쓰기 시간과
#               파일 크기를 측정하고, 저장한 파일을 다시 읽어 건수/키 값이
#               일치하는지 검증한다.
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, save_json / save_with_performance / verify_saved_data 구현
#
# ------------------------------------------------------------------
"""집계 결과를 CSV와 Parquet로 저장하고 성능을 측정합니다."""

from __future__ import annotations

import json
from pathlib import Path
from time import perf_counter
from typing import Any

import pandas as pd
from pydantic import BaseModel


def save_json(data: Any, file_path: Path) -> None:
    """딕셔너리 또는 리스트를 JSON 파일로 저장합니다.

    ensure_ascii=False를 빠뜨리면 한글이 유니코드 이스케이프로 저장돼
    사람이 파일을 열었을 때 읽을 수 없게 되므로 반드시 명시한다.
    """
    file_path.parent.mkdir(parents=True, exist_ok=True)
    with file_path.open("w", encoding="utf-8") as file:
        json.dump(data, file, ensure_ascii=False, indent=2)


def save_with_performance(
    records: list[BaseModel],
    csv_path: Path,
    parquet_path: Path,
) -> dict[str, float | int]:
    """레코드 리스트를 CSV/Parquet로 각각 저장하고, 쓰기 시간·파일 크기를 측정합니다.

    반환값은 main.py에서 그대로 performance_result.json에 쌓이는 딕셔너리라서,
    "지금 몇 건을 얼마 만에 어떤 크기로 저장했는지"를 재실행 없이 파일로 남길 수 있다.
    """
    csv_path.parent.mkdir(parents=True, exist_ok=True)

    # Pydantic 모델 리스트 -> DataFrame. model_dump()로 각 레코드를 dict로 바꿔 pandas에 넘긴다.
    dataframe = pd.DataFrame([record.model_dump() for record in records])

    # perf_counter()는 벽시계 시간보다 정밀도가 높아 짧은 구간 측정에 적합하다.
    csv_start = perf_counter()
    dataframe.to_csv(csv_path, index=False, encoding="utf-8-sig")
    csv_seconds = perf_counter() - csv_start

    parquet_start = perf_counter()
    # Parquet는 열 지향 이진 포맷이라 텍스트 기반인 CSV보다 파일이 작고 읽기가
    # 빠른 경우가 많다(데이터 규모가 작으면 오히려 오버헤드가 더 클 수도 있다).
    dataframe.to_parquet(
        parquet_path,
        index=False,
        engine="pyarrow",
        compression="snappy",
    )
    parquet_seconds = perf_counter() - parquet_start

    return {
        "rows": len(dataframe),
        "csv_seconds": round(csv_seconds, 6),
        "parquet_seconds": round(parquet_seconds, 6),
        "csv_bytes": csv_path.stat().st_size,
        "parquet_bytes": parquet_path.stat().st_size,
    }


def verify_saved_data(
    csv_path: Path,
    parquet_path: Path,
    key_column: str | None = None,
) -> tuple[int, int]:
    """CSV와 Parquet를 다시 읽고 행 수(및 선택적으로 키 컬럼)를 비교합니다.

    key_column을 주면 해당 컬럼 값이 두 파일에서 완전히 같은 순서로 저장됐는지도 확인한다.
    (weather처럼 datetime 컬럼은 CSV/Parquet 왕복 시 문자열 표현이 달라질 수 있어
     key_column 비교를 생략하고 건수만 확인하도록 호출부에서 선택한다.)
    """
    # save_with_performance()가 방금 쓴 파일을 곧바로 다시 읽어 "왕복"이 온전한지 확인한다.
    csv_data = pd.read_csv(csv_path)
    parquet_data = pd.read_parquet(parquet_path, engine="pyarrow")

    csv_rows = len(csv_data)
    parquet_rows = len(parquet_data)

    # 건수부터 다르면 컬럼 비교는 의미가 없으므로 여기서 바로 중단한다.
    if csv_rows != parquet_rows:
        raise ValueError(f"CSV와 Parquet의 행 수가 다릅니다: {csv_path.name}")

    if key_column is not None:
        # tolist()로 비교하는 이유: pandas Series끼리 == 비교는 원소별 bool Series를
        # 반환해 그대로 if 조건에 못 쓰므로, 순수 파이썬 list로 바꿔 통째로 비교한다.
        if csv_data[key_column].tolist() != parquet_data[key_column].tolist():
            raise ValueError(f"CSV와 Parquet의 {key_column} 값이 다릅니다: {csv_path.name}")

    return csv_rows, parquet_rows
