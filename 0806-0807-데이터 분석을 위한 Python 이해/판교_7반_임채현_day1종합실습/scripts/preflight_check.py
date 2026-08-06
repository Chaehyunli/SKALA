# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [Day 1 종합실습] 제출 전 환경/결과파일/pytest/ruff/git 이력을 한 번에 검사
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, check_package_versions/check_output_files/check_ruff/
#             check_git_history를 순서대로 실행하는 main() 구현
#
# ------------------------------------------------------------------
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
WEATHER_CSV = OUTPUT_DIR / "weather.csv"
WEATHER_PARQUET = OUTPUT_DIR / "weather.parquet"
COUNTRY_CSV = OUTPUT_DIR / "country.csv"
COUNTRY_PARQUET = OUTPUT_DIR / "country.parquet"
IP_CSV = OUTPUT_DIR / "ip.csv"
IP_PARQUET = OUTPUT_DIR / "ip.parquet"
PERFORMANCE_FILE = OUTPUT_DIR / "performance_result.json"
GIT_LOG_FILE = PROJECT_ROOT / "git_log.txt"

# requirements.txt에 고정한 버전과 동일하게 맞춘다.
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
        raise RuntimeError(f"{title} 실패: 종료 코드 {result.returncode}")

    return result.stdout


def check_package_versions() -> None:
    """requirements.txt에 고정한 패키지 버전을 확인합니다."""
    for module_name, expected in EXPECTED_VERSIONS.items():
        module = importlib.import_module(module_name)
        actual = getattr(module, "__version__", None)

        if actual != expected:
            raise RuntimeError(f"{module_name} 버전 불일치: 기대 {expected}, 실제 {actual}")

        print(f"[PASS] {module_name}=={actual}")


def check_output_files() -> None:
    """weather/country/ip 각각의 CSV, Parquet, 성능 결과 JSON을 검사합니다."""
    required_files = [
        WEATHER_CSV,
        WEATHER_PARQUET,
        COUNTRY_CSV,
        COUNTRY_PARQUET,
        IP_CSV,
        IP_PARQUET,
        PERFORMANCE_FILE,
    ]

    missing = [
        str(path.relative_to(PROJECT_ROOT)) for path in required_files if not path.exists()
    ]
    if missing:
        raise RuntimeError("프로그램 실행 결과 파일이 없습니다: " + ", ".join(missing))

    weather_csv_rows = len(pd.read_csv(WEATHER_CSV))
    weather_parquet_rows = len(pd.read_parquet(WEATHER_PARQUET, engine="pyarrow"))
    if weather_csv_rows != weather_parquet_rows:
        raise RuntimeError("weather CSV와 Parquet의 행 수가 다릅니다.")

    country_csv = pd.read_csv(COUNTRY_CSV)
    country_parquet = pd.read_parquet(COUNTRY_PARQUET, engine="pyarrow")
    if country_csv["name"].tolist() != country_parquet["name"].tolist():
        raise RuntimeError("country CSV와 Parquet의 name 값이 일치하지 않습니다.")

    ip_csv = pd.read_csv(IP_CSV)
    ip_parquet = pd.read_parquet(IP_PARQUET, engine="pyarrow")
    if ip_csv["query"].tolist() != ip_parquet["query"].tolist():
        raise RuntimeError("ip CSV와 Parquet의 query 값이 일치하지 않습니다.")

    with PERFORMANCE_FILE.open("r", encoding="utf-8") as file:
        performance = json.load(file)

    required_keys = {"name", "rows", "csv_seconds", "parquet_seconds", "csv_bytes", "parquet_bytes"}
    for entry in performance:
        if not required_keys.issubset(entry):
            raise RuntimeError("performance_result.json의 필수 항목이 누락되었습니다.")

    print(f"[PASS] weather 재로딩: CSV={weather_csv_rows}건, Parquet={weather_parquet_rows}건")
    print("[PASS] country/ip 재로딩 및 CSV-Parquet 값 일치 확인")
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
    """커밋 이력을 확인하고 git_log.txt로 저장합니다.

    이 프로젝트는 더 큰 Git 저장소(SKALA) 하위 폴더로 존재할 수 있으므로,
    PROJECT_ROOT에 .git이 있는지 직접 확인하는 대신 `git log`가 성공하는지로 판단한다.
    """
    log_output = run_command(["git", "log", "--oneline", "-20"], "Git 커밋 이력").strip()

    if not log_output:
        raise RuntimeError("Git 커밋 이력이 없습니다.")

    GIT_LOG_FILE.write_text(log_output + "\n", encoding="utf-8")
    print("[PASS] git_log.txt 생성")


def main() -> None:
    """제출 전에 필요한 검사를 순서대로 실행합니다."""
    print("Day 1 종합실습 제출 전 검사")

    check_package_versions()
    check_output_files()

    run_command([sys.executable, "-m", "pytest", "-v"], "pytest 자동 테스트")
    check_ruff()
    check_git_history()

    print("\n전체 제출 전 검사를 통과했습니다.")


if __name__ == "__main__":
    try:
        main()
    except (ImportError, OSError, RuntimeError) as exc:
        print(f"\n[FAIL] {exc}")
        raise SystemExit(1) from exc
