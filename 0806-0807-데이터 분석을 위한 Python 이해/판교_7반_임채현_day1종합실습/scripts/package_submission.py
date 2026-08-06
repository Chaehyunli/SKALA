# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [Day 1 종합실습] 프로젝트 제출용 ZIP을 운영체제와 관계없이 생성
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, preflight_check 실행 후 필수 파일 확인 -> ZIP 생성
#
# ------------------------------------------------------------------
"""프로젝트 제출용 ZIP을 운영체제와 관계없이 생성합니다."""

from __future__ import annotations

import argparse
import subprocess
import sys
import zipfile
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
# venv/캐시는 용량만 크고 제출물엔 불필요 + 환경마다 절대경로가 달라 그대로 옮기면
# 오히려 문제가 된다(과거에 .venv를 폴더째 옮겼다가 activate 스크립트가 깨진 적이 있었다).
EXCLUDED_NAMES = {
    ".venv",
    "__pycache__",
    ".pytest_cache",
    ".ruff_cache",
    ".DS_Store",
}
EXCLUDED_SUFFIXES = {".pyc", ".pyo"}


def should_exclude(path: Path) -> bool:
    """가상환경, 캐시, 기존 ZIP 파일을 압축에서 제외합니다."""
    relative = path.relative_to(PROJECT_ROOT)

    # 경로의 "어느 한 조각"이라도 제외 대상 이름과 같으면 스킵한다.
    # 예: data/output/__pycache__/x.pyc 처럼 하위 폴더 안에 있어도 걸러낸다.
    if any(part in EXCLUDED_NAMES for part in relative.parts):
        return True

    if path.suffix in EXCLUDED_SUFFIXES:
        return True

    # 과거에 만든 제출 zip 파일이 프로젝트 폴더 안에 남아 있어도 재귀적으로
    # 다시 압축에 포함되지 않도록 막는다("zip 안에 zip" 방지).
    return path.suffix == ".zip"


def verify_required_files() -> None:
    """실행 결과, Git 이력, PDF 보고서를 확인합니다."""
    output_dir = PROJECT_ROOT / "data" / "output"
    required = [
        output_dir / "weather.csv",
        output_dir / "weather.parquet",
        output_dir / "country.csv",
        output_dir / "country.parquet",
        output_dir / "ip.csv",
        output_dir / "ip.parquet",
        output_dir / "performance_result.json",
        PROJECT_ROOT / "git_log.txt",
    ]

    missing = [str(path.relative_to(PROJECT_ROOT)) for path in required if not path.exists()]
    if missing:
        raise RuntimeError("제출 필수 파일이 없습니다: " + ", ".join(missing))

    pdf_files = list((PROJECT_ROOT / "reports").glob("*.pdf"))
    if not pdf_files:
        raise RuntimeError("reports 폴더에 실행 결과 PDF가 없습니다.")


def create_archive(output_path: Path) -> None:
    """프로젝트 최상위 폴더를 포함해 ZIP을 생성합니다."""
    output_path.parent.mkdir(parents=True, exist_ok=True)

    if output_path.exists():
        output_path.unlink()  # 이전에 만든 동명 zip이 있으면 지우고 새로 만든다 (덮어쓰기 아님).

    root_name = PROJECT_ROOT.name

    with zipfile.ZipFile(output_path, mode="w", compression=zipfile.ZIP_DEFLATED) as archive:
        # sorted()로 순회 순서를 고정해, 실행할 때마다 zip 내부 파일 순서가
        # 달라지지 않게 한다 (재현 가능한 빌드).
        for path in sorted(PROJECT_ROOT.rglob("*")):
            if not path.is_file() or should_exclude(path):
                continue

            # zip 안에서도 "프로젝트 폴더명/..." 구조를 유지해, 압축을 풀면
            # 바로 같은 이름의 폴더가 나오게 한다 (제출 규칙: "폴더 포함 전체 코드").
            archive_name = Path(root_name) / path.relative_to(PROJECT_ROOT)
            archive.write(path, archive_name)

    print(f"[PASS] ZIP 생성: {output_path}")
    print(f"[PASS] ZIP 크기: {output_path.stat().st_size} bytes")


def main() -> None:
    """제출 전 검사 후 지정한 이름으로 ZIP을 생성합니다."""
    parser = argparse.ArgumentParser(description="Day 1 종합실습 제출 ZIP 생성")
    parser.add_argument("output_name", help="예: 판교_7반_임채현_day1종합실습.zip")
    args = parser.parse_args()

    output_name = args.output_name
    if not output_name.lower().endswith(".zip"):
        raise SystemExit("출력 파일명은 .zip으로 끝나야 합니다.")

    # zip을 만들기 전에 preflight_check.py를 먼저 통째로 재실행한다.
    # check=True라서 preflight가 하나라도 실패하면 여기서 CalledProcessError로
    # 즉시 중단되고, 미완성 상태의 zip이 만들어지는 걸 막는다.
    subprocess.run(
        [sys.executable, "scripts/preflight_check.py"],
        cwd=PROJECT_ROOT,
        check=True,
    )

    verify_required_files()

    # zip은 프로젝트 폴더 "안"이 아니라 "옆"(부모 디렉터리)에 만든다.
    # 프로젝트 폴더 안에 만들면 다음 실행 때 zip 자기 자신을 다시 순회 대상으로
    # 잡을 위험이 있어서다 (should_exclude가 막아주긴 하지만, 애초에 피하는 게 안전).
    output_path = PROJECT_ROOT.parent / output_name
    create_archive(output_path)


if __name__ == "__main__":
    try:
        main()
    except (OSError, RuntimeError, subprocess.CalledProcessError) as exc:
        print(f"[FAIL] {exc}")
        raise SystemExit(1) from exc
