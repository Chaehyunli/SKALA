"""API 주소와 출력 경로를 관리합니다."""

from pathlib import Path

BASE_URL = "https://jsonplaceholder.typicode.com"
USERS_ENDPOINT = "/users"
POSTS_ENDPOINT = "/posts"
TODOS_ENDPOINT = "/todos"

PROJECT_ROOT = Path(__file__).resolve().parent.parent
OUTPUT_DIR = PROJECT_ROOT / "data" / "output"
CSV_OUTPUT = OUTPUT_DIR / "user_activity.csv"
PARQUET_OUTPUT = OUTPUT_DIR / "user_activity.parquet"
PERFORMANCE_OUTPUT = OUTPUT_DIR / "performance_result.json"
VALIDATION_ERROR_OUTPUT = OUTPUT_DIR / "validation_errors.json"
