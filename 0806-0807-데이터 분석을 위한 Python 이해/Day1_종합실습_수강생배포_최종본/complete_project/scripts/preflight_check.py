"""제출 전 환경, 실행 결과, 테스트, Ruff, Git 이력을 검사합니다."""

from __future__ import annotations

import importlib
import json
import shutil
import subprocess
import sys
from pathlib import Path

import pandas as pd


PROJECT_ROOT = Path(__file__).resolve().parent.parent
OUTPUT_DIR = PROJECT_ROOT / "data" / "output"
CSV_FILE = OUTPUT_DIR / "user_activity.csv"
PARQUET_FILE = OUTPUT_DIR / "user_activity.parquet"
PERFORMANCE_FILE = OUTPUT_DIR / "performance_result.json"
GIT_LOG_FILE = PROJECT_ROOT / "git_log.txt"

EXPECTED_VERSIONS = {
    "httpx": "0.28.1",
    "pydantic": "2.13.4",
    "pandas": "3.0.5",
    "pyarrow": "25.0.0",
    "pytest": "9.1.1",
    "pytest_asyncio": "1.4.0",
}


def run_command(command: list[str], title: str) -> str:
    """명령을 실행하고 실패하면 검사 과정을 중단합니다."""
    print(f"\n=== {title} ===")
    result = subprocess.run(
        command,
        cwd=PROJECT_ROOT,
        text=True,
        capture_output=True,
        check=False,
    )

    if result.stdout:
        print(result.stdout.rstrip())
    if result.stderr:
        print(result.stderr.rstrip())

    if result.returncode != 0:
        raise RuntimeError(
            f"{title} 실패: 종료 코드 {result.returncode}"
        )

    return result.stdout


def check_python_version() -> None:
    """Python 3.13 계열인지 확인합니다."""
    version = sys.version_info
    if (version.major, version.minor) != (3, 13):
        raise RuntimeError(
            "Python 3.13 계열이 필요합니다. "
            f"현재 버전: {version.major}.{version.minor}.{version.micro}"
        )

    print(
        "[PASS] Python:",
        f"{version.major}.{version.minor}.{version.micro}",
    )


def check_package_versions() -> None:
    """requirements.txt에 고정한 패키지 버전을 확인합니다."""
    for module_name, expected in EXPECTED_VERSIONS.items():
        module = importlib.import_module(module_name)
        actual = getattr(module, "__version__", None)

        if actual != expected:
            raise RuntimeError(
                f"{module_name} 버전 불일치: "
                f"기대 {expected}, 실제 {actual}"
            )

        print(f"[PASS] {module_name}=={actual}")


def check_output_files() -> None:
    """CSV, Parquet, 성능 결과 JSON을 검사합니다."""
    required_files = [
        CSV_FILE,
        PARQUET_FILE,
        PERFORMANCE_FILE,
    ]

    missing = [
        str(path.relative_to(PROJECT_ROOT))
        for path in required_files
        if not path.exists()
    ]
    if missing:
        raise RuntimeError(
            "프로그램 실행 결과 파일이 없습니다: "
            + ", ".join(missing)
        )

    csv_data = pd.read_csv(CSV_FILE)
    parquet_data = pd.read_parquet(
        PARQUET_FILE,
        engine="pyarrow",
    )

    if len(csv_data) != 10 or len(parquet_data) != 10:
        raise RuntimeError(
            "CSV와 Parquet는 각각 10건이어야 합니다."
        )

    if csv_data["user_id"].tolist() != parquet_data["user_id"].tolist():
        raise RuntimeError(
            "CSV와 Parquet의 user_id 값이 일치하지 않습니다."
        )

    with PERFORMANCE_FILE.open("r", encoding="utf-8") as file:
        performance = json.load(file)

    required_keys = {
        "rows",
        "csv_seconds",
        "parquet_seconds",
        "csv_bytes",
        "parquet_bytes",
    }
    if not required_keys.issubset(performance):
        raise RuntimeError(
            "performance_result.json의 필수 항목이 누락되었습니다."
        )

    if performance["rows"] != 10:
        raise RuntimeError(
            "performance_result.json의 rows는 10이어야 합니다."
        )

    print("[PASS] CSV 재로딩: 10건")
    print("[PASS] Parquet 재로딩: 10건")
    print("[PASS] CSV와 Parquet user_id 일치")
    print("[PASS] 성능 결과 JSON 구조 확인")


def check_ruff() -> None:
    """설치된 Ruff로 프로젝트 전체를 검사합니다."""
    ruff_path = shutil.which("ruff")
    if ruff_path:
        command = [ruff_path, "check", "."]
    else:
        command = [sys.executable, "-m", "ruff", "check", "."]

    run_command(command, "Ruff 코드 검사")


def check_git_history() -> None:
    """커밋 이력을 확인하고 git_log.txt로 저장합니다."""
    if not (PROJECT_ROOT / ".git").exists():
        raise RuntimeError(
            "Git 저장소가 없습니다. git init과 commit을 먼저 실행하세요."
        )

    log_output = run_command(
        ["git", "log", "--oneline"],
        "Git 커밋 이력",
    ).strip()

    if not log_output:
        raise RuntimeError("Git 커밋 이력이 없습니다.")

    GIT_LOG_FILE.write_text(
        log_output + "\n",
        encoding="utf-8",
    )
    print("[PASS] git_log.txt 생성")


def main() -> None:
    """제출 전에 필요한 검사를 순서대로 실행합니다."""
    print("Day 1 종합실습 제출 전 검사")

    check_python_version()
    check_package_versions()
    check_output_files()

    run_command(
        [sys.executable, "-m", "pytest", "-v"],
        "pytest 자동 테스트",
    )
    check_ruff()
    check_git_history()

    print("\n전체 제출 전 검사를 통과했습니다.")


if __name__ == "__main__":
    try:
        main()
    except (ImportError, OSError, RuntimeError) as exc:
        print(f"\n[FAIL] {exc}")
        raise SystemExit(1) from exc
