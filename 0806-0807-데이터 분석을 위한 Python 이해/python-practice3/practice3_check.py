# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [실습 3] Pandas EDA / Polars Lazy / DuckDB SQL 비교 검증 스크립트
#               practice3.py의 문제 1~4 구현 결과를 assert로 검증
# 작성일      : 2026-08-07
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-07, 최초 작성, 문제 1~4 assert 검증 코드 작성
#
# ------------------------------------------------------------------
from practice3 import (
    DATA_PATH,
    duckdb_sql_agg,
    load_sales_csv,
    pandas_eda_clean,
    pandas_named_agg,
    polars_lazy_agg,
)


def pass_message(message: str) -> None:
    print(f"[PASS] {message}")


def main() -> None:
    # ===== 문제 1: EDA + IQR 이상치 제거 =====
    raw_df = load_sales_csv(DATA_PATH)
    assert len(raw_df) > 0
    cleaned_df, lower, upper = pandas_eda_clean(raw_df)
    assert lower < upper
    assert len(cleaned_df) < len(raw_df)
    assert cleaned_df["amount"].between(lower, upper).all()
    pass_message(f"IQR 이상치 제거: {len(raw_df)}건 -> {len(cleaned_df)}건")

    # ===== 문제 2: Pandas named aggregation =====
    pandas_result = pandas_named_agg(cleaned_df)
    assert list(pandas_result.columns) == ["region", "category", "total", "mean", "count"]
    assert pandas_result["total"].is_monotonic_decreasing
    pass_message(f"Pandas named aggregation: {len(pandas_result)}개 그룹, 총매출 내림차순")

    # ===== 문제 3: Polars Lazy 집계 =====
    polars_result = polars_lazy_agg(DATA_PATH, lower, upper)
    assert set(polars_result.columns) == {"region", "category", "total", "mean", "count"}
    assert polars_result["total"].is_sorted(descending=True)
    pass_message(f"Polars Lazy 집계: {len(polars_result)}개 그룹, 총매출 내림차순")

    # ===== 문제 4: DuckDB SQL 집계 =====
    duckdb_result = duckdb_sql_agg(DATA_PATH, lower, upper)
    assert set(duckdb_result.columns) == {"region", "category", "total", "mean", "count"}
    assert duckdb_result["total"].is_monotonic_decreasing
    pass_message(f"DuckDB SQL 집계: {len(duckdb_result)}개 그룹, 총매출 내림차순")

    # 세 결과가 동일 집계인지 (그룹 수 + 총매출 합계 일치) 교차 확인
    assert len(pandas_result) == len(polars_result) == len(duckdb_result)
    pandas_total_sum = round(pandas_result["total"].sum(), 2)
    polars_total_sum = round(polars_result["total"].sum(), 2)
    duckdb_total_sum = round(duckdb_result["total"].sum(), 2)
    assert pandas_total_sum == polars_total_sum == duckdb_total_sum
    pass_message("Pandas/Polars/DuckDB 집계 결과 총합 일치 확인")

    print("\n전체 검사를 통과했습니다.")


if __name__ == "__main__":
    main()
