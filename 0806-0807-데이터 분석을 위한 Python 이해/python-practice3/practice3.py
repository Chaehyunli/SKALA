# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [실습 3] Pandas EDA / Polars Lazy / DuckDB SQL 비교
#               1) Pandas EDA (df.info, isnull().sum()) + IQR 이상치 제거
#               2) Pandas groupby named aggregation (region·category별
#                  total/mean/count, 총매출 내림차순)
#               3) 2)와 동일한 집계를 Polars Lazy API로 재작성
#                  (scan_csv -> filter -> group_by -> agg -> sort -> collect)
#               4) 2)와 동일한 집계를 DuckDB SQL(GROUP BY)로 작성하고
#                  timeit으로 Pandas/Polars/DuckDB 실행 시간 비교
# 작성일      : 2026-08-07
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-07, 최초 작성, sales_100k.csv 자동 생성 스크립트 + 문제 1~4 구현
# 2026-08-07, 실데이터 반영, 교수님이 주신 실제 sales_100k.csv(100만행,
#             region/category/amount에 결측치 포함)로 교체. pandas.groupby는
#             NaN 키를 자동으로 빼는데 Polars/DuckDB는 NULL도 그룹으로 잡길래
#             세 집계 모두 결측 행을 동일하게 제외하도록 맞춤
# 2026-08-07, 문서화, 함수 설명이랑 파일 읽기 오류 처리, 회고 추가
# 2026-08-07, 정리, 실제 CSV만 쓰니까 합성 데이터 생성 함수(generate_sales_csv) 제거
# 2026-08-07, 버그 수정, timeit 비교에서 Pandas는 이미 읽어둔 DataFrame으로
#             groupby만 재고 Polars/DuckDB는 파일 스캔까지 재고 있어서 불공정했음.
#             세 도구 모두 파일부터 새로 읽도록 통일
# 2026-08-07, 연계 준비, python-practice4가 별도 폴더/가상환경이라 import 대신
#             파일로 넘기기 위해 이상치 제거된 DataFrame과 집계 결과를 CSV로 저장
#
# ------------------------------------------------------------------
import timeit
from pathlib import Path

import duckdb
import pandas as pd
import polars as pl

DATA_PATH = Path(__file__).with_name("sales_100k.csv")
CLEANED_OUTPUT_PATH = Path(__file__).with_name("sales_100k_cleaned.csv")
AGG_OUTPUT_PATH = Path(__file__).with_name("region_category_agg.csv")


def load_sales_csv(file_path: Path) -> pd.DataFrame:
    """sales_100k.csv를 읽어서 DataFrame으로 반환.

    파일이 없는 경우(FileNotFoundError)랑 형식이 깨진 경우(ParserError)를
    구분해서 무슨 파일이 문제인지 메시지로 남긴다.
    """
    try:
        return pd.read_csv(file_path)
    except FileNotFoundError as error:
        raise FileNotFoundError(
            f"{file_path.name}을(를) 찾을 수 없습니다. 같은 폴더에 파일이 있는지 확인하세요."
        ) from error
    except pd.errors.ParserError as error:
        raise pd.errors.ParserError(f"{file_path.name} 파싱에 실패했습니다: {error}") from error


# ===== 문제 1: Pandas EDA (df.info, isnull().sum()) + IQR 이상치 제거 =====
def pandas_eda_clean(df: pd.DataFrame) -> tuple[pd.DataFrame, float, float]:
    """EDA 출력하고 amount를 IQR 기준으로 이상치 제거.

    정상 범위는 Q1 - 1.5*IQR ~ Q3 + 1.5*IQR.
    """
    print(df.info())
    print(df.isnull().sum())

    q1 = df["amount"].quantile(0.25)
    q3 = df["amount"].quantile(0.75)
    iqr = q3 - q1
    lower, upper = q1 - 1.5 * iqr, q3 + 1.5 * iqr

    before = len(df)
    # between()은 NaN이면 False라서 amount 결측치도 여기서 같이 걸러진다
    cleaned = df[df["amount"].between(lower, upper)]
    print(f"이상치 제거 전: {before}건 / 제거 후: {len(cleaned)}건 (IQR 범위 [{lower:.1f}, {upper:.1f}])")

    return cleaned, lower, upper


# ===== 문제 2: Pandas groupby named aggregation
#       (region·category별 total/mean/count, 총매출 내림차순) =====
def pandas_named_agg(df: pd.DataFrame) -> pd.DataFrame:
    """region/category별 named aggregation. 컬럼명을 total=('amount','sum')처럼
    직접 지정해서 agg({'amount': 'sum'})의 자동 생성 컬럼명에 의존하지 않는다.

    region/category 결측 행은 dropna로 먼저 뺀다. pandas.groupby는 그룹 키가
    NaN이면 자동으로 빠지는데 Polars/DuckDB는 NULL도 그룹으로 잡아서, 그냥 두면
    세 결과의 그룹 수가 달라진다.
    """
    return (
        df.dropna(subset=["region", "category"])
        .groupby(["region", "category"])
        .agg(total=("amount", "sum"), mean=("amount", "mean"), count=("amount", "count"))
        .reset_index()
        .sort_values("total", ascending=False)
    )


# ===== 문제 3: 문제 2와 동일한 집계를 Polars Lazy API로 재작성
#       (scan_csv -> filter -> group_by -> agg -> sort -> collect) =====
def polars_lazy_agg(file_path: Path, lower: float, upper: float) -> pl.DataFrame:
    """scan_csv로 LazyFrame을 만들고 collect()까지 체이닝.
    lower/upper는 문제 1에서 구한 값을 그대로 받아써서 세 도구가 같은 IQR
    경계로 필터링하도록 맞춘다.
    """
    return (
        pl.scan_csv(file_path)
        .filter(
            pl.col("amount").is_between(lower, upper)
            & pl.col("region").is_not_null()
            & pl.col("category").is_not_null()
        )
        .group_by(["region", "category"])
        .agg(
            total=pl.col("amount").sum(),
            mean=pl.col("amount").mean(),
            count=pl.col("amount").count(),
        )
        .sort("total", descending=True)
        .collect()
    )


# ===== 문제 4: 문제 2와 동일한 집계를 DuckDB SQL(GROUP BY)로 작성 =====
def duckdb_sql_agg(file_path: Path, lower: float, upper: float) -> pd.DataFrame:
    """DuckDB GROUP BY로 동일 집계를 실행하고 .df()로 pandas DataFrame으로 반환.

    쿼리 오타나 없는 컬럼 참조는 duckdb.Error로 올라오는데, 어떤 쿼리가
    실패했는지 알 수 있게 메시지를 붙여 다시 던진다.
    """
    query = f"""
        SELECT region, category,
               SUM(amount) AS total,
               AVG(amount) AS mean,
               COUNT(*) AS count
        FROM read_csv_auto('{file_path}')
        WHERE amount BETWEEN {lower} AND {upper}
          AND region IS NOT NULL AND category IS NOT NULL
        GROUP BY region, category
        ORDER BY total DESC
    """
    try:
        return duckdb.sql(query).df()
    except duckdb.Error as error:
        raise duckdb.Error(f"DuckDB 집계 쿼리 실행에 실패했습니다: {error}") from error


# ===== timeit으로 Pandas/Polars/DuckDB 실행 시간 비교 =====
def compare_runtime(file_path: Path, lower: float, upper: float, number: int = 5) -> None:
    """CSV 읽기 + IQR 필터링 + region·category 집계를 세 도구 모두 파일에서
    매번 새로 읽어 number번씩 반복한 시간을 비교.

    Pandas만 이미 읽어둔 DataFrame으로 groupby만 재면 Polars/DuckDB의 파일
    스캔 비용이 통째로 빠져서 비교가 안 맞으니, 세 도구 다 파일부터 새로 읽는다.
    """

    def pandas_pipeline() -> pd.DataFrame:
        raw = pd.read_csv(file_path)
        return pandas_named_agg(raw[raw["amount"].between(lower, upper)])

    pandas_time = timeit.timeit(pandas_pipeline, number=number)
    polars_time = timeit.timeit(lambda: polars_lazy_agg(file_path, lower, upper), number=number)
    duckdb_time = timeit.timeit(lambda: duckdb_sql_agg(file_path, lower, upper), number=number)

    print(f"\n[실행 시간 비교] (반복 {number}회 합산, 초)")
    print(f"Pandas : {pandas_time:.4f}")
    print(f"Polars : {polars_time:.4f}")
    print(f"DuckDB : {duckdb_time:.4f}")


def main() -> None:
    # 문제 1: Pandas EDA + IQR 이상치 제거 (lower/upper는 2~4에서 계속 재사용)
    raw_df = load_sales_csv(DATA_PATH)
    cleaned_df, lower, upper = pandas_eda_clean(raw_df)

    # 문제 2: Pandas named aggregation
    print("\n1. Pandas named aggregation (상위 5개)")
    pandas_result = pandas_named_agg(cleaned_df)
    print(pandas_result.head())

    # practice4에서 재사용할 수 있게 이상치 제거된 DataFrame과 집계 결과를
    # 파일로 저장 (practice4는 별도 폴더/가상환경이라 import 대신 파일로 넘긴다)
    cleaned_df.to_csv(CLEANED_OUTPUT_PATH, index=False)
    pandas_result.to_csv(AGG_OUTPUT_PATH, index=False)

    # 문제 3: Polars Lazy API로 동일 집계
    print("\n2. Polars Lazy 집계 (상위 5개)")
    polars_result = polars_lazy_agg(DATA_PATH, lower, upper)
    print(polars_result.head())

    # 문제 4: DuckDB SQL로 동일 집계
    print("\n3. DuckDB SQL 집계 (상위 5개)")
    duckdb_result = duckdb_sql_agg(DATA_PATH, lower, upper)
    print(duckdb_result.head())

    # 세 결과가 진짜 같은 집계인지 그룹 개수로 한번 교차 확인
    assert len(pandas_result) == len(polars_result) == len(duckdb_result)

    # 문제 4: timeit으로 세 도구 실행 시간 비교
    compare_runtime(DATA_PATH, lower, upper)


if __name__ == "__main__":
    main()


# ------------------------------------------------------------------
# 회고
#
# 1) Pandas EDA + IQR 이상치 제거
#    - 처음엔 직접 만든 합성 데이터(10만행)로 시작했는데, 교수님이 실제
#      데이터(100만행, 결측치 포함)를 주시면서 practice3_check.py에
#      len(raw_df) == 100_000 같은 값을 그대로 박아둔 게 문제가 됐다.
#      "몇 건이어야 한다"보다 "결측 제거 후 줄어들었는지" 같은 상대적인
#      조건으로 검증하는 게 데이터가 바뀌어도 안 깨진다.
#    - between()이 NaN을 False로 처리해줘서 amount 결측치(5000건)도
#      이상치 제거 단계에서 따로 손댈 필요 없이 같이 빠졌다.
#
# 2) Pandas groupby named aggregation
#    - total=('amount','sum') 식으로 쓰면 결과 컬럼명이 코드에 바로
#      드러나서, 이후 Polars/DuckDB 결과랑 컬럼명 맞추기가 쉬웠다.
#
# 3) Polars Lazy API로 동일 집계
#    - scan_csv는 바로 안 읽고 실행 계획만 세우다가 collect()에서 한번에
#      돌아간다. collect() 빼먹으면 LazyFrame 객체만 나오고 결과가 안
#      찍힌다는 걸 한 번 실수하고 나서야 체감했다.
#
# 4) DuckDB SQL + 세 도구 성능 비교
#    - 같은 IQR 경계와 같은 결측치 기준을 쓰도록 맞추고 나서야 그룹
#      개수(64개)랑 총매출 합계가 정확히 맞았다. pandas는 NaN 키를
#      자동으로 빼고 Polars/DuckDB는 NULL도 그룹으로 잡는 차이를 눈으로는
#      못 알아채고 assert로 교차 검증하다가 발견했다.
#    - 처음엔 DuckDB가 Pandas보다 느리게 나와서 이상하다 싶었는데, 알고
#      보니 pandas_time만 이미 읽어둔 DataFrame으로 groupby를 재고 있었다.
#      세 도구 다 파일에서 새로 읽도록 고치니 Pandas 2.67초, Polars 0.21초,
#      DuckDB 0.61초로 나와서 원래 예상대로 Polars/DuckDB가 훨씬 빨랐다.
#      timeit 벤치마크는 "뭘 재고 있는지"부터 맞춰야 숫자가 의미 있다는 걸
#      배웠다.
# ------------------------------------------------------------------
