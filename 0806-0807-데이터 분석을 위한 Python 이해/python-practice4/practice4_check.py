# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [실습 4] 시각화 4종 / 통계 검정 / sklearn Pipeline 검증 스크립트
#               practice4.py의 문제 1~4 구현 결과를 assert로 검증
# 작성일      : 2026-08-07
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-07, 최초 작성, 문제 1~4 assert 검증 코드 작성
#
# ------------------------------------------------------------------
import joblib

from practice4 import (
    AGG_PATH,
    DASHBOARD_OUTPUT_PATH,
    MODEL_OUTPUT_PATH,
    PLOTLY_OUTPUT_PATH,
    build_and_save_pipeline,
    load_sales_data,
    run_chi2_test,
    run_ttest,
)


def pass_message(message: str) -> None:
    print(f"[PASS] {message}")


def main() -> None:
    # ===== 데이터 로드 + 필수 컬럼 검증 =====
    raw_df, cleaned_df, agg_df = load_sales_data()
    assert len(raw_df) > 0
    assert len(cleaned_df) < len(raw_df)
    assert cleaned_df[["region", "category"]].isnull().sum().sum() == 0
    pass_message(f"데이터 로드: 원본 {len(raw_df)}건 -> 정제 후 {len(cleaned_df)}건")

    # ===== 문제 1: EDA 대시보드 파일 생성 확인 =====
    assert DASHBOARD_OUTPUT_PATH.exists()
    pass_message(f"EDA 대시보드 파일 생성 확인: {DASHBOARD_OUTPUT_PATH.name}")

    # ===== 문제 2: t-test / 카이제곱 검정 결과가 유효한 p-value인지 확인 =====
    _, ttest_p = run_ttest(cleaned_df)
    assert 0.0 <= ttest_p <= 1.0
    pass_message(f"t-test p-value 범위 확인: {ttest_p:.4f}")

    _, chi2_p = run_chi2_test(agg_df)
    assert 0.0 <= chi2_p <= 1.0
    pass_message(f"카이제곱 p-value 범위 확인: {chi2_p:.4f}")

    # ===== 문제 3: Pipeline 학습/저장/재로딩 확인 =====
    score = build_and_save_pipeline(cleaned_df, MODEL_OUTPUT_PATH)
    assert MODEL_OUTPUT_PATH.exists()
    reloaded_pipeline = joblib.load(MODEL_OUTPUT_PATH)
    assert hasattr(reloaded_pipeline, "predict")
    pass_message(f"Pipeline 저장/재로딩 확인: R^2={score:.4f}")

    # ===== 문제 4: Plotly 차트 HTML 저장 확인 =====
    assert PLOTLY_OUTPUT_PATH.exists()
    pass_message(f"Plotly 차트 파일 생성 확인: {PLOTLY_OUTPUT_PATH.name}")

    print("\n전체 검사를 통과했습니다.")


if __name__ == "__main__":
    main()
