"""프로젝트 제출용 ZIP을 운영체제와 관계없이 생성합니다."""

from __future__ import annotations

import argparse
import subprocess
import sys
import zipfile
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent.parent
EXCLUDED_NAMES = {
    ".venv",
    "__pycache__",
    ".pytest_cache",
    ".ruff_cache",
    ".DS_Store",
}
EXCLUDED_SUFFIXES = {
    ".pyc",
    ".pyo",
}


def should_exclude(path: Path) -> bool:
    """가상환경, 캐시, 기존 ZIP 파일을 압축에서 제외합니다."""
    relative = path.relative_to(PROJECT_ROOT)

    if any(part in EXCLUDED_NAMES for part in relative.parts):
        return True

    if path.suffix in EXCLUDED_SUFFIXES:
        return True

    return path.suffix == ".zip"


def verify_required_files() -> None:
    """실행 결과, Git 이력, PDF 보고서를 확인합니다."""
    required = [
        PROJECT_ROOT / "data" / "output" / "user_activity.csv",
        PROJECT_ROOT / "data" / "output" / "user_activity.parquet",
        PROJECT_ROOT / "data" / "output" / "performance_result.json",
        PROJECT_ROOT / "git_log.txt",
    ]

    missing = [
        str(path.relative_to(PROJECT_ROOT))
        for path in required
        if not path.exists()
    ]
    if missing:
        raise RuntimeError(
            "제출 필수 파일이 없습니다: " + ", ".join(missing)
        )

    pdf_files = list(
        (PROJECT_ROOT / "reports").glob("*.pdf")
    )
    if not pdf_files:
        raise RuntimeError(
            "reports 폴더에 실행 결과 PDF가 없습니다."
        )

    if not (PROJECT_ROOT / ".git").exists():
        raise RuntimeError(
            ".git 폴더가 없습니다. Git 커밋을 먼저 생성하세요."
        )


def create_archive(output_path: Path) -> None:
    """프로젝트 최상위 폴더를 포함해 ZIP을 생성합니다."""
    output_path.parent.mkdir(parents=True, exist_ok=True)

    if output_path.exists():
        output_path.unlink()

    root_name = PROJECT_ROOT.name

    with zipfile.ZipFile(
        output_path,
        mode="w",
        compression=zipfile.ZIP_DEFLATED,
    ) as archive:
        for path in sorted(PROJECT_ROOT.rglob("*")):
            if not path.is_file() or should_exclude(path):
                continue

            archive_name = Path(root_name) / path.relative_to(PROJECT_ROOT)
            archive.write(path, archive_name)

    print(f"[PASS] ZIP 생성: {output_path}")
    print(f"[PASS] ZIP 크기: {output_path.stat().st_size} bytes")


def main() -> None:
    """제출 전 검사 후 지정한 이름으로 ZIP을 생성합니다."""
    parser = argparse.ArgumentParser(
        description="Day 1 종합실습 제출 ZIP 생성",
    )
    parser.add_argument(
        "output_name",
        help="예: 서울_1반_홍길동_day1종합실습.zip",
    )
    args = parser.parse_args()

    output_name = args.output_name
    if not output_name.lower().endswith(".zip"):
        raise SystemExit("출력 파일명은 .zip으로 끝나야 합니다.")

    subprocess.run(
        [sys.executable, "scripts/preflight_check.py"],
        cwd=PROJECT_ROOT,
        check=True,
    )

    verify_required_files()

    output_path = PROJECT_ROOT.parent / output_name
    create_archive(output_path)


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, subprocess.CalledProcessError) as exc:
        print(f"[FAIL] {exc}")
        raise SystemExit(1) from exc
