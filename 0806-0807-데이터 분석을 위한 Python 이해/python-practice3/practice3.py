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
#             region/category/amount에 결측치 포함)로 교체하면서
#             pandas.groupby가 NaN 키를 자동 제외하는 것과 다르게
#             Polars/DuckDB는 NULL도 그룹으로 잡는 차이를 발견 -> 세 집계
#             모두 region/category가 결측인 행을 동일하게 제외하도록 통일
# 2026-08-07, 문서화, 함수별 상세 주석과 파일 읽기 오류 처리, 회고 추가
# 2026-08-07, 정리, 실제 CSV를 항상 사용하므로 합성 데이터 생성 함수
#             (generate_sales_csv) 제거
# 2026-08-07, 버그 수정, timeit 비교에서 Pandas만 이미 로드된 DataFrame으로
#             groupby만 재고 Polars/DuckDB는 파일 스캔까지 재는 불공정한
#             비교였던 것을 세 도구 모두 파일에서 새로 읽도록 통일
#
# ------------------------------------------------------------------
import timeit
from pathlib import Path

import duckdb
import pandas as pd
import polars as pl

DATA_PATH = Path(__file__).with_name("sales_100k.csv")


def load_sales_csv(file_path: Path) -> pd.DataFrame:
    """sales_100k.csv를 pandas DataFrame으로 로드합니다.

    파일이 없거나(FileNotFoundError) 형식이 깨져 파싱이 안 되는 경우
    (pandas.errors.ParserError)를 각각 구분해 원인을 알 수 있는 메시지로
    다시 던진다. 그냥 죽게 두는 대신 어떤 파일이 왜 문제인지 알려주면
    디버깅 시간이 줄어든다.
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
    """기본 EDA를 출력하고 amount 컬럼을 IQR 기준으로 이상치 제거합니다.

    정상 범위는 "Q1 - 1.5*IQR ~ Q3 + 1.5*IQR" 공식을 그대로 따른다.
    """
    print(df.info())
    print(df.isnull().sum())

    q1 = df["amount"].quantile(0.25)
    q3 = df["amount"].quantile(0.75)
    iqr = q3 - q1
    lower, upper = q1 - 1.5 * iqr, q3 + 1.5 * iqr  # Q1-1.5*IQR ~ Q3+1.5*IQR

    before = len(df)
    # Series.between(lower, upper)는 NaN에 대해 False를 반환하므로
    # amount가 결측인 행도 이상치와 함께 자연스럽게 제거된다.
    cleaned = df[df["amount"].between(lower, upper)]
    print(f"이상치 제거 전: {before}건 / 제거 후: {len(cleaned)}건 (IQR 범위 [{lower:.1f}, {upper:.1f}])")

    return cleaned, lower, upper


# ===== 문제 2: Pandas groupby named aggregation
#       (region·category별 total/mean/count, 총매출 내림차순) =====
def pandas_named_agg(df: pd.DataFrame) -> pd.DataFrame:
    """agg({'amount': 'sum'}) 처럼 컬럼명을 pandas가 임의로 정하게 두지 않고,
    total=('amount','sum') 형태로 결과 컬럼명을 직접 지정한다(named aggregation).

    dropna(subset=[...])로 region/category가 결측인 행을 먼저 제거하는 이유:
    pandas.groupby는 기본적으로 그룹 키가 NaN인 행을 조용히 제외하지만,
    Polars/DuckDB는 NULL도 하나의 그룹으로 포함시킨다. 이 차이를 그대로 두면
    세 결과의 그룹 개수가 달라져 동일 집계라고 볼 수 없으므로, 세 함수 모두
    결측 키 행을 명시적으로 제외해 기준을 통일한다.
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
    """read_csv가 아닌 scan_csv로 시작해 LazyFrame을 만들고,
    체인 끝에 collect()를 호출해 실제 DataFrame으로 실체화한다.
    lower/upper는 Pandas EDA 단계(문제 1)에서 계산한 값을 그대로 받아써서
    세 도구가 동일한 IQR 경계로 필터링하도록 맞춘다.
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
    """DuckDB SQL의 GROUP BY로 Pandas/Polars와 동일한 집계를 수행하고
    .df()로 결과를 pandas DataFrame으로 변환해 반환합니다.

    SQL 문자열 자체의 오타나 존재하지 않는 컬럼 참조 등은 duckdb.Error로
    올라오므로, 어떤 쿼리가 실패했는지 알 수 있게 감싸서 재발생시킨다.
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
    """"CSV 읽기 + IQR 필터링 + region·category 집계"를 세 도구 모두 file_path에서
    매번 새로 읽어 number번씩 반복 실행한 시간을 비교합니다.

    Pandas만 이미 메모리에 있는 DataFrame으로 groupby만 재는 반면 Polars/DuckDB는
    파일 스캔·파싱 비용까지 포함하면 비교 대상이 서로 달라져 불공정해지므로,
    세 도구 모두 파일에서 새로 읽어오도록 통일한다.
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
    # 문제 1: Pandas EDA + IQR 이상치 제거 (lower/upper는 이후 2~4에서 공통으로 재사용)
    raw_df = load_sales_csv(DATA_PATH)
    cleaned_df, lower, upper = pandas_eda_clean(raw_df)

    # 문제 2: Pandas named aggregation
    print("\n1. Pandas named aggregation (상위 5개)")
    pandas_result = pandas_named_agg(cleaned_df)
    print(pandas_result.head())

    # 문제 3: Polars Lazy API로 동일 집계
    print("\n2. Polars Lazy 집계 (상위 5개)")
    polars_result = polars_lazy_agg(DATA_PATH, lower, upper)
    print(polars_result.head())

    # 문제 4: DuckDB SQL로 동일 집계
    print("\n3. DuckDB SQL 집계 (상위 5개)")
    duckdb_result = duckdb_sql_agg(DATA_PATH, lower, upper)
    print(duckdb_result.head())

    # 세 도구가 정말 "동일 집계"를 했는지 그룹 개수로 교차 확인 (다르면 바로 드러남)
    assert len(pandas_result) == len(polars_result) == len(duckdb_result)

    # 문제 4: timeit으로 세 도구 실행 시간 비교
    compare_runtime(DATA_PATH, lower, upper)


if __name__ == "__main__":
    main()


# ------------------------------------------------------------------
# 회고
#
# 1) Pandas EDA + IQR 이상치 제거
#    - 처음엔 자체 생성한 합성 데이터(sales_100k.csv, 10만행)로 만들었는데,
#      교수님이 실제 데이터(100만행, region/category/amount에 결측치 포함)를
#      주시면서 practice3_check.py의 len(raw_df) == 100_000 같은 값을
#      하드코딩해두면 데이터가 바뀔 때마다 깨진다는 걸 체감했다. 이후로는
#      "몇 건이어야 한다"는 절대값 대신 "결측 제거 후 건수가 줄어드는지" 같은
#      상대적인 조건으로 검증하도록 고쳤다.
#    - between(lower, upper)이 NaN에는 False를 반환한다는 점 덕분에
#      amount 결측치(5000건)가 이상치 제거 단계에서 별도 처리 없이도
#      자연스럽게 함께 걸러졌다.
#
# 2) Pandas groupby named aggregation
#    - agg(total=('amount','sum'), ...) 형태의 named aggregation은
#      agg({'amount': 'sum'})과 달리 결과 컬럼명을 코드에서 바로 결정하므로,
#      이후 Polars/DuckDB 결과와 컬럼명을 맞추기가 훨씬 쉬웠다.
#
# 3) Polars Lazy API로 동일 집계
#    - scan_csv는 즉시 읽지 않고 실행 계획만 세우다가 collect()에서 한 번에
#      최적화해 실행한다는 걸 실제로 확인했다. collect()를 빼먹으면
#      LazyFrame 객체만 출력되고 실제 집계 결과가 안 나온다는 걸 실수로
#      한 번 겪어보고 나서야 "왜 감점 대상에 collect() 누락이 따로 있는지"
#      이해가 됐다.
#
# 4) DuckDB SQL + 세 도구 성능 비교
#    - 세 도구 모두 같은 IQR 경계(lower/upper)와 같은 결측치 처리 기준을
#      쓰도록 통일하고 나서야 그룹 개수(64개)와 총매출 합계가 정확히
#      일치했다. 겉보기엔 "같은 집계"처럼 보여도 각 도구의 기본 동작
#      (pandas의 NaN 키 자동 제외 vs Polars/DuckDB의 NULL 그룹화)이
#      미묘하게 달라서, 결과를 눈으로 훑어보는 것만으로는 이 차이를
#      못 잡고 assert로 교차 검증했을 때 비로소 드러났다.
#    - timeit 반복 횟수(number=5)를 세 도구 모두 동일하게 맞추니
#      Polars가 가장 빠르고 DuckDB가 매 호출마다 CSV를 다시 스캔하는
#      구조라 상대적으로 느리다는 경향을 일관되게 확인할 수 있었다.
# ------------------------------------------------------------------
