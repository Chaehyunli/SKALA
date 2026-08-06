# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [실습 2] 파일 I/O, 예외 처리, Pydantic 검증 파이프라인 검증 스크립트
#               practice2.py의 문제 1~4 구현 결과를 assert로 검증
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, 문제 1~4 assert 검증 코드 작성 (자체 제작 CSV 기준)
# 2026-08-06, 데이터 소스 변경, Python_Practice1_Data.json + 가짜 불량 레코드 기준으로 수정
# 2026-08-06, 부작용 제거, tempfile로 실제 output 파일을 건드리지 않도록 수정
#
# ------------------------------------------------------------------
import tempfile
from pathlib import Path

from practice2 import (
    INPUT_JSON_PATH,
    INVALID_SAMPLE_RECORDS,
    load_csv_rows,
    safe_load_csv,
    save_error_records,
    save_valid_records,
    validate_sales_records,
)


def pass_message(message: str) -> None:
    print(f"[PASS] {message}")


def main() -> None:
    # ===== 문제 1: 예외 처리 + 파일 읽기 =====
    missing_result = safe_load_csv(Path(__file__).with_name("존재하지_않는_파일.json"))
    assert missing_result is None
    pass_message("safe_load_csv: 없는 파일 -> None 반환")

    raw_data = safe_load_csv(INPUT_JSON_PATH)
    assert raw_data is not None
    assert len(raw_data) == 100
    pass_message("safe_load_csv: Python_Practice1_Data.json 로드 (100건)")

    total_input_count = len(raw_data)
    raw_data = raw_data + INVALID_SAMPLE_RECORDS

    # ===== 문제 2, 3: Pydantic 검증 파이프라인 =====
    valid_records, error_records = validate_sales_records(raw_data)
    assert len(valid_records) == total_input_count
    assert len(error_records) == len(INVALID_SAMPLE_RECORDS)
    assert all("row" in item and "error" in item for item in error_records)
    pass_message(f"valid {len(valid_records)}건 / errors {len(error_records)}건 분리 검증")

    # ===== 문제 4: 결과 파일 저장 + 재로딩 확인 =====
    # 실제 valid_sales.csv / invalid_sales.json을 건드리지 않도록 임시 디렉터리에 저장한다.
    with tempfile.TemporaryDirectory() as temp_dir:
        valid_path = Path(temp_dir) / "valid_sales.csv"
        error_path = Path(temp_dir) / "invalid_sales.json"

        save_valid_records(valid_records, valid_path)
        save_error_records(error_records, error_path)

        reloaded = load_csv_rows(valid_path)
        assert len(reloaded) == total_input_count
        pass_message(f"저장 후 재로딩 건수 검증 (len(reloaded) == {total_input_count})")

    print("\n전체 검사를 통과했습니다.")


if __name__ == "__main__":
    main()
