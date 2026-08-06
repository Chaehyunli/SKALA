# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [Day 1 종합실습] app/storage.py의 저장/재로딩 검증 로직 테스트
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, save_with_performance + verify_saved_data 테스트 작성
#
# ------------------------------------------------------------------
"""저장과 재로딩 검증 로직 테스트입니다."""

from pathlib import Path

import pytest

from app.models import CountryInfo
from app.storage import save_json, save_with_performance, verify_saved_data


def _sample_country() -> CountryInfo:
    return CountryInfo(
        name="Korea (Republic of)",
        capital="Seoul",
        region="Asia",
        subregion="Eastern Asia",
        population=51780579,
        area=100210,
        timezones=["UTC+09:00"],
    )


def test_save_with_performance_writes_csv_and_parquet(tmp_path: Path) -> None:
    """CSV/Parquet 파일이 실제로 생성되고, 성능 딕셔너리에 필요한 키가 모두 있어야 한다."""
    csv_path = tmp_path / "country.csv"
    parquet_path = tmp_path / "country.parquet"

    performance = save_with_performance([_sample_country()], csv_path, parquet_path)

    assert csv_path.exists()
    assert parquet_path.exists()
    assert performance["rows"] == 1
    assert {"csv_seconds", "parquet_seconds", "csv_bytes", "parquet_bytes"} <= performance.keys()


def test_verify_saved_data_matches_key_column(tmp_path: Path) -> None:
    """저장 직후 재로딩하면 행 수와 key_column 값이 원본과 일치해야 한다."""
    csv_path = tmp_path / "country.csv"
    parquet_path = tmp_path / "country.parquet"
    save_with_performance([_sample_country()], csv_path, parquet_path)

    csv_rows, parquet_rows = verify_saved_data(csv_path, parquet_path, key_column="name")

    assert csv_rows == 1
    assert parquet_rows == 1


def test_verify_saved_data_raises_when_row_counts_differ(tmp_path: Path) -> None:
    """CSV와 Parquet의 행 수가 다르면 ValueError를 발생시켜야 한다."""
    csv_path = tmp_path / "broken.csv"
    parquet_path = tmp_path / "broken.parquet"
    save_with_performance([_sample_country(), _sample_country()], csv_path, parquet_path)

    # CSV만 한 줄(헤더+1건)로 손상시켜 Parquet(2건)와 행 수가 어긋나게 만든다.
    lines = csv_path.read_text(encoding="utf-8-sig").splitlines()
    csv_path.write_text("\n".join(lines[:2]) + "\n", encoding="utf-8-sig")

    with pytest.raises(ValueError, match="행 수가 다릅니다"):
        verify_saved_data(csv_path, parquet_path)


def test_save_json_preserves_korean_text(tmp_path: Path) -> None:
    """ensure_ascii=False로 저장해 한글이 유니코드 이스케이프 없이 그대로 남아야 한다."""
    file_path = tmp_path / "errors.json"

    save_json([{"source": "country", "message": "이름이 비어 있습니다"}], file_path)

    content = file_path.read_text(encoding="utf-8")
    assert "이름이 비어 있습니다" in content
