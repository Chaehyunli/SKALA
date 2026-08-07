# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [실습 4] 시각화 4종 · 통계 검정 · sklearn Pipeline
#               1) Matplotlib+Seaborn 2x2 서브플롯으로 EDA 대시보드 작성
#                  (히스토그램+KDE / 박스플롯 / 월별 라인 / 상관 히트맵)
#               2) 서울 vs 부산 평균 매출 차이를 독립표본 t-test로 검정
#               3) 지역x카테고리 독립성을 카이제곱 검정으로 확인
#               4) ColumnTransformer + Pipeline(Ridge 회귀)로 amount 예측
#                  모델을 학습·평가하고 joblib으로 저장 후 재로딩 검증
#               5) 지역·카테고리별 총매출을 Plotly 인터랙티브 차트로 만들어
#                  HTML로 저장
#               python-practice3에서 만든 산출물 3종을 그대로 입력으로 쓴다:
#               sales_100k.csv(원본, 컬럼 검증용) / sales_100k_cleaned.csv
#               (IQR 이상치 제거본) / region_category_agg.csv(groupby 결과)
# 작성일      : 2026-08-07
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-07, 최초 작성, python-practice3 산출물을 입력으로 문제 1~4 구현
# 2026-08-07, 안정성 보강, 교수님 참고 코드(practice4_sales_analysis.py) 검토 후
#             headless 환경 대비 Agg 백엔드 고정, 한글 폰트 후보 목록 + 폴백,
#             main() 예외 처리, t-test/카이제곱 표본 크기 가드 추가
#
# ------------------------------------------------------------------
import sys
from pathlib import Path

import joblib
import matplotlib

matplotlib.use("Agg")  # 화면(디스플레이) 없는 환경에서도 그림을 파일로 저장할 수 있게 고정

import matplotlib.pyplot as plt
import pandas as pd
import plotly.express as px
import seaborn as sns
from matplotlib import font_manager
from scipy.stats import chi2_contingency, ttest_ind
from sklearn.compose import ColumnTransformer
from sklearn.linear_model import Ridge
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

# 이 스크립트와 같은 폴더의 파일들을 절대경로로 고정 (실행 위치가 달라져도 안전하게)
RAW_PATH = Path(__file__).with_name("sales_100k.csv")
CLEANED_PATH = Path(__file__).with_name("sales_100k_cleaned.csv")
AGG_PATH = Path(__file__).with_name("region_category_agg.csv")
DASHBOARD_OUTPUT_PATH = Path(__file__).with_name("eda_dashboard.png")
MODEL_OUTPUT_PATH = Path(__file__).with_name("ridge_pipeline.joblib")
PLOTLY_OUTPUT_PATH = Path(__file__).with_name("region_category_sales.html")

REQUIRED_COLUMNS = [
    "order_id", "order_date", "region", "category", "product_name",
    "quantity", "unit_price", "payment_method", "customer_age",
    "customer_gender", "amount",
]

# OS마다 설치된 한글 폰트 이름이 달라서 후보를 나열해두고 실제 설치된 것만 골라 쓴다
KOREAN_FONT_CANDIDATES = ["AppleGothic", "Malgun Gothic", "NanumGothic", "NanumBarunGothic", "Noto Sans CJK KR"]


def configure_korean_font() -> None:
    """그래프에 지역/카테고리명 같은 한글이 들어가므로, 설치된 한글 폰트가 있으면 적용한다.

    없는 폰트를 그냥 지정해버리면 matplotlib이 기본 폰트로 조용히 대체해서
    한글이 네모(□)로 깨지는데, 이 경우 실행 자체는 에러 없이 끝나서 결과
    이미지를 열어보기 전까진 문제를 알아채기 어렵다. 그래서 설치된 폰트
    목록에서 후보를 찾아 있는 것만 적용하고, 없으면 안내만 출력한다.
    """
    installed = {font.name for font in font_manager.fontManager.ttflist}
    selected = next((name for name in KOREAN_FONT_CANDIDATES if name in installed), None)
    if selected is None:
        print("[안내] 설치된 한글 폰트를 찾지 못해 그래프의 한글이 깨질 수 있습니다.")
        return
    plt.rcParams["font.family"] = selected
    plt.rcParams["axes.unicode_minus"] = False


def load_sales_data() -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    """원본/정제본/집계본 CSV 3개를 읽어서 반환.

    원본(sales_100k.csv)은 필수 컬럼이 다 있는지 확인하는 용도로만 쓰고,
    정제본(sales_100k_cleaned.csv)은 practice3에서 IQR로 amount 이상치까지는
    걸렀지만 region/category 결측치는 남아있어서 여기서 마저 제거한다.
    파일이 없으면 practice3을 먼저 돌려야 한다는 걸 바로 알 수 있게 메시지를 남긴다.
    """
    try:
        raw_df = pd.read_csv(RAW_PATH)
        cleaned_df = pd.read_csv(CLEANED_PATH)
        agg_df = pd.read_csv(AGG_PATH)
    except FileNotFoundError as error:
        raise FileNotFoundError(
            f"{error.filename}이(가) 없습니다. python-practice3/practice3.py를 먼저 실행해서 "
            "sales_100k.csv, sales_100k_cleaned.csv, region_category_agg.csv를 이 폴더로 복사하세요."
        ) from error

    missing_columns = [col for col in REQUIRED_COLUMNS if col not in raw_df.columns]
    if missing_columns:
        raise ValueError(f"{RAW_PATH.name}에 필수 컬럼이 없습니다: {missing_columns}")

    cleaned_df = cleaned_df.dropna(subset=["region", "category"])
    return raw_df, cleaned_df, agg_df


# ===== 문제 1: EDA 시각화 4종 (2x2 서브플롯) =====
def plot_eda_dashboard(df: pd.DataFrame, output_path: Path) -> None:
    """히스토그램+KDE / 박스플롯 / 월별 매출 라인 / 상관 히트맵을 2x2 서브플롯 하나에 그려 저장.

    plt.show() 대신 savefig를 쓰는 이유: 이 스크립트는 터미널에서 실행하는
    배치 스크립트라 화면에 띄우는 것보다 파일로 남기는 게 결과 확인이 편하다.
    """
    fig, axes = plt.subplots(2, 2, figsize=(12, 10))

    # (0,0) amount 분포 히스토그램 + KDE
    sns.histplot(df["amount"], kde=True, ax=axes[0, 0])
    axes[0, 0].set_title("매출(amount) 분포")

    # (0,1) 지역별 amount 박스플롯 (이상치는 이미 IQR로 제거된 상태)
    sns.boxplot(data=df, x="region", y="amount", ax=axes[0, 1])
    axes[0, 1].set_title("지역별 매출 분포")

    # (1,0) 월별 총매출 라인 (order_date의 연-월 단위로 집계)
    monthly_total = df.assign(month=pd.to_datetime(df["order_date"]).dt.to_period("M").astype(str)).groupby("month")["amount"].sum()
    axes[1, 0].plot(monthly_total.index, monthly_total.values, marker="o")
    axes[1, 0].set_title("월별 총매출")
    axes[1, 0].tick_params(axis="x", rotation=45)

    # (1,1) 수치형 컬럼 간 상관관계 히트맵
    numeric_cols = ["quantity", "unit_price", "customer_age", "amount"]
    sns.heatmap(df[numeric_cols].corr(), annot=True, fmt=".2f", cmap="coolwarm", ax=axes[1, 1])
    axes[1, 1].set_title("수치형 컬럼 상관관계")

    fig.tight_layout()
    fig.savefig(output_path)
    plt.close(fig)
    print(f"EDA 대시보드 저장 완료: {output_path.name}")


# ===== 문제 2-1: 서울 vs 부산 평균 매출 t-test =====
def run_ttest(df: pd.DataFrame) -> tuple[float, float]:
    """서울/부산 amount 표본으로 독립표본 t-test를 실행하고 t통계량·p-value를 반환."""
    seoul_amount = df.loc[df["region"] == "서울", "amount"]
    busan_amount = df.loc[df["region"] == "부산", "amount"]
    if len(seoul_amount) < 2 or len(busan_amount) < 2:
        raise ValueError("t-test에는 서울과 부산 데이터가 각각 2건 이상 필요합니다.")

    t_stat, p_value = ttest_ind(seoul_amount, busan_amount)
    verdict = "유의미한 차이가 있다" if p_value < 0.05 else "유의미한 차이가 없다"
    print(f"\n[t-test] 서울 vs 부산 평균 매출: t={t_stat:.4f}, p={p_value:.4f} -> {verdict} (p<0.05 기준)")
    return t_stat, p_value


# ===== 문제 2-2: 지역x카테고리 독립성 카이제곱 검정 =====
def run_chi2_test(agg_df: pd.DataFrame) -> tuple[float, float]:
    """practice3의 groupby 결과(region_category_agg.csv)를 분할표로 재구성해 카이제곱 검정.

    원본에서 crosstab을 다시 만드는 대신, 이미 region/category별 건수(count)가
    계산돼 있는 agg_df를 pivot해서 분할표로 쓴다.
    """
    contingency = agg_df.pivot(index="region", columns="category", values="count").fillna(0)
    if contingency.shape[0] < 2 or contingency.shape[1] < 2:
        raise ValueError("카이제곱 검정에는 지역과 카테고리가 각각 2개 이상 필요합니다.")

    chi2_stat, p_value, _, _ = chi2_contingency(contingency)
    verdict = "독립이 아니다(연관 있음)" if p_value < 0.05 else "독립이다(연관 없음)"
    print(f"[카이제곱] 지역x카테고리 독립성: chi2={chi2_stat:.4f}, p={p_value:.4f} -> {verdict} (p<0.05 기준)")
    return chi2_stat, p_value


# ===== 문제 3: ColumnTransformer + Pipeline(Ridge)로 amount 예측 모델 =====
def build_and_save_pipeline(df: pd.DataFrame, model_path: Path) -> float:
    """수치형은 표준화, 범주형은 원-핫 인코딩한 뒤 Ridge 회귀로 amount를 예측하는
    Pipeline을 학습·평가하고 joblib으로 저장, 다시 불러와 점수가 같은지 확인한다.
    """
    numeric_features = ["quantity", "unit_price", "customer_age"]
    categorical_features = ["region", "category", "payment_method", "customer_gender"]
    features = df[numeric_features + categorical_features]
    target = df["amount"]

    X_train, X_test, y_train, y_test = train_test_split(features, target, test_size=0.2, random_state=42)

    preprocessor = ColumnTransformer(
        [
            ("num", StandardScaler(), numeric_features),
            ("cat", OneHotEncoder(handle_unknown="ignore"), categorical_features),
        ]
    )
    pipeline = Pipeline([("preprocess", preprocessor), ("model", Ridge())])

    pipeline.fit(X_train, y_train)
    pipeline.predict(X_test)
    score = pipeline.score(X_test, y_test)
    print(f"\n[Pipeline] Ridge 회귀 R^2 score: {score:.4f}")

    joblib.dump(pipeline, model_path)
    reloaded_pipeline = joblib.load(model_path)
    reloaded_score = reloaded_pipeline.score(X_test, y_test)
    assert reloaded_score == score
    print(f"모델 저장/재로딩 확인 완료: {model_path.name} (재로딩 후 score 동일)")

    return score


# ===== 문제 4: 지역·카테고리별 총매출 Plotly 인터랙티브 차트 =====
def save_plotly_chart(agg_df: pd.DataFrame, output_path: Path) -> None:
    """region_category_agg.csv의 total을 그대로 Plotly 막대 차트로 그려 HTML로 저장."""
    fig = px.bar(
        agg_df,
        x="category",
        y="total",
        color="region",
        barmode="group",
        title="지역·카테고리별 총매출",
    )
    fig.write_html(output_path)
    print(f"Plotly 차트 저장 완료: {output_path.name}")


def main() -> None:
    configure_korean_font()

    raw_df, cleaned_df, agg_df = load_sales_data()
    print(f"원본 {len(raw_df)}건 / 결측치까지 정리한 분석용 데이터 {len(cleaned_df)}건")

    # 문제 1: EDA 시각화 4종
    plot_eda_dashboard(cleaned_df, DASHBOARD_OUTPUT_PATH)

    # 문제 2: 통계 검정 (t-test + 카이제곱)
    run_ttest(cleaned_df)
    run_chi2_test(agg_df)

    # 문제 3: sklearn Pipeline 구성 + 저장/재로딩
    build_and_save_pipeline(cleaned_df, MODEL_OUTPUT_PATH)

    # 문제 4: Plotly 인터랙티브 차트 저장
    save_plotly_chart(agg_df, PLOTLY_OUTPUT_PATH)


if __name__ == "__main__":
    try:
        main()
    except (FileNotFoundError, ValueError, RuntimeError, OSError, pd.errors.ParserError) as error:
        # 트레이스백 대신 원인만 간단히 알려주고 실패했다는 걸 종료 코드로도 남긴다
        print(f"\n[실행 오류] {error}", file=sys.stderr)
        raise SystemExit(1) from error


# ------------------------------------------------------------------
# 회고
#
# 1) EDA 시각화 4종
#    - 처음에 한글 폰트를 안 잡아줬더니 그래프 제목이랑 지역명이 다 네모로
#      깨져서 나왔다. rcParams["font.family"]에 AppleGothic을 지정하니 해결.
#    - plt.show() 대신 savefig로 바꾼 이유는 단순한데, 터미널 스크립트는
#      화면에 창을 띄워봐야 다음 사람이 확인할 방법이 없어서 파일로 남기는
#      게 훨씬 실용적이었다.
#
# 2) t-test / 카이제곱 검정
#    - 카이제곱 검정에서 원본 CSV로 crosstab을 다시 만들 수도 있었는데,
#      practice3에서 이미 region_category_agg.csv에 건수(count)까지
#      계산해둔 걸 pivot만 해서 바로 분할표로 썼다. 같은 집계를 두 번
#      할 필요가 없다는 걸 이번에 실습3-4 연계를 만들면서 체감했다.
#
# 3) sklearn Pipeline
#    - ColumnTransformer로 수치형/범주형 전처리를 한 객체에 몰아넣고
#      Ridge와 Pipeline으로 묶으니, fit 한 번으로 전처리부터 학습까지
#      끝나서 예측할 때도 원본 형태의 DataFrame을 그대로 넣을 수 있었다.
#      전처리를 따로 떼서 했으면 predict할 때도 스케일러/인코더를 매번
#      따로 적용해야 했을 텐데 그 번거로움이 없어졌다.
#    - joblib으로 저장한 뒤 다시 불러온 모델의 score가 저장 전과 정확히
#      같은지 assert로 확인해보니, Pipeline 객체가 전처리 상태(평균/분산,
#      인코딩 카테고리)까지 통째로 직렬화된다는 걸 눈으로 확인할 수 있었다.
#
# 4) Plotly 차트
#    - region_category_agg.csv를 그대로 px.bar에 넣을 수 있어서 원본
#      데이터를 다시 그룹화하는 코드가 필요 없었다. practice3 산출물을
#      그대로 재사용한 게 이번 실습에서 제일 크게 시간을 아낀 부분이었다.
# ------------------------------------------------------------------
