# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [실습 2] 파일 I/O, 예외 처리, Pydantic 검증 파이프라인
#               1) try-except 기반 안전 로딩 (safe_load_csv)
#                  - 교수님 안내: Python_Practice1_Data.json을 재사용하므로
#                    함수명은 safe_load_csv를 유지하되 내부는 json.load 사용
#               2) Pydantic v2로 SalesRecord 스키마 정의 및 필드 검증
#               3) raw_data를 SalesRecord로 변환하며 valid/errors 분리
#               4) valid는 CSV, errors는 JSON으로 저장 후 재로딩 검증
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, 문제 1~4 구현 (자체 제작 CSV 사용)
# 2026-08-06, 데이터 소스 변경, Python_Practice1_Data.json을 json.load로 직접 로드하도록 수정
#             (교수님 Q&A 답변 반영: CSV 변환 불필요, 함수명만 safe_load_csv 유지)
# 2026-08-06, 스키마/에러 처리 강화, amount를 float로, category 공백을 None으로 정규화하는
#             field_validator 추가, 에러 정보를 str 대신 exc.errors()로 구조화해 저장
# 2026-08-06, 문서화, 상세 주석 및 회고 추가
#
# ------------------------------------------------------------------
import csv
import json
import logging
from pathlib import Path
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, ValidationError, field_validator


# 문제 1의 logger.error/logger.info 요구사항을 만족시키기 위한 기본 로거 설정.
# print 대신 logging을 쓰면 레벨(INFO/ERROR)이 로그에 함께 남아 운영 환경에서도 필터링이 쉽다.
logging.basicConfig(level=logging.INFO, format="[%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)

# 이 스크립트와 같은 폴더의 파일들을 절대경로로 고정한다.
# (실행 위치=cwd가 달라져도 항상 practice2.py 옆의 파일을 찾도록)
INPUT_JSON_PATH = Path(__file__).with_name("Python_Practice1_Data.json")
VALID_OUTPUT_PATH = Path(__file__).with_name("valid_sales.csv")
ERRORS_OUTPUT_PATH = Path(__file__).with_name("invalid_sales.json")

# 검증 파이프라인(문제 3)의 오류 처리 경로를 실제로 보여주기 위해 추가한 가짜 불량 레코드.
# Python_Practice1_Data.json의 100건은 전부 정상값이라 이 레코드 없이는 ValidationError가
# 한 번도 발생하지 않는다. 실무였다면 사용자 업로드/외부 API처럼 신뢰할 수 없는 입력에서
# 이런 형태의 결측·범위 오류가 자연스럽게 나온다고 가정한 테스트 픽스처다.
INVALID_SAMPLE_RECORDS = [
    {"month": "", "region": "서울", "category": "전자", "amount": 1000},  # month 누락
    {"month": "2024-05", "region": "", "category": "식품", "amount": 500},  # region 누락
    {"month": "2024-05", "region": "부산", "category": "의류", "amount": -200},  # amount 0 이하
]


# ===== 문제 1: 예외 처리 + 파일 읽기 =====
def safe_load_csv(file_path: Path) -> list[dict] | None:
    """JSON 파일을 읽어 Sales 목록을 dict 리스트로 반환합니다.

    파일이 없으면 None을 반환하고 logger.error로 남기며,
    성공하면 읽은 건수를 logger.info로 남깁니다.
    finally 블록은 성공/실패 여부와 무관하게 항상 "로딩 종료"를 출력합니다.
    """
    try:
        # 존재하지 않는 경로를 열면 FileNotFoundError가 발생 -> except에서 잡는다.
        with file_path.open("r", encoding="utf-8") as file:
            data = json.load(file)
            # 원본 JSON은 {"Sales": [...]} 구조이므로 "Sales" 키의 리스트만 꺼낸다.
            rows = data["Sales"]
    except FileNotFoundError:
        logger.error("파일을 찾을 수 없습니다: %s", file_path)
        return None
    else:
        # try 블록이 예외 없이 끝났을 때만 실행된다 (return을 try 안에 두지 않기 위한 else).
        logger.info("%s에서 %d건을 읽었습니다.", file_path.name, len(rows))
        return rows
    finally:
        # return/예외 발생 여부와 무관하게 항상 마지막에 실행된다.
        print("로딩 종료")


# ===== 문제 2: Pydantic v2 스키마 정의 =====
class SalesRecord(BaseModel):
    """판매 레코드 스키마. month/region은 필수(비어있으면 안 됨),
    amount는 0 초과, category는 없어도 된다."""

    # 문자열 필드에 앞뒤 공백이 섞여 들어와도 자동으로 strip한다.
    model_config = ConfigDict(str_strip_whitespace=True)

    month: str = Field(min_length=1)  # 빈 문자열이면 ValidationError
    region: str = Field(min_length=1)  # 빈 문자열이면 ValidationError
    category: str | None = None  # 없어도 되는 선택 필드
    amount: float = Field(gt=0)  # 0 이하이면 ValidationError

    @field_validator("category", mode="before")
    @classmethod
    def blank_category_to_none(cls, value: Any) -> str | None:
        """빈 문자열 category를 선택값인 None으로 변환합니다.

        mode="before"라서 Pydantic이 타입 검증을 하기 전, 즉 원본 입력값(raw value)에
        먼저 적용된다. CSV/JSON에서 "" 빈 문자열로 들어와도 실제 "값 없음"과 동일하게
        취급하기 위함이다.
        """
        if value is None:
            return None
        text = str(value).strip()
        return text or None


# ===== 문제 3: 검증 파이프라인 (valid / errors 분리) =====
def validate_sales_records(
    raw_data: list[dict],
) -> tuple[list[SalesRecord], list[dict]]:
    """raw_data의 각 행을 SalesRecord로 변환을 시도해 valid/errors로 나눕니다."""
    valid: list[SalesRecord] = []
    errors: list[dict] = []

    for row in raw_data:
        try:
            # dict 언패킹으로 SalesRecord(month=..., region=..., ...) 형태 생성 시도.
            record = SalesRecord(**row)
        except ValidationError as error:
            # 체크포인트: ValidationError 발생 시 오류 내용을 출력한다.
            print(f"[VALIDATION ERROR] {row} -> {error}")
            # error.errors()는 pydantic이 구조화해서 주는 오류 목록(type/loc/msg/input).
            # str(error)로 통짜 텍스트를 저장하는 대신 이걸 쓰면 나중에 JSON에서
            # 어떤 필드가 왜 실패했는지 프로그램적으로 파싱하기 쉽다.
            errors.append({"row": row, "error": error.errors(include_url=False)})
        else:
            valid.append(record)

    return valid, errors


# ===== 문제 4: 결과 파일 저장 + 재로딩 확인 =====
def save_valid_records(records: list[SalesRecord], file_path: Path) -> None:
    """valid 레코드를 CSV로 저장합니다. (model_dump()로 필드를 얻어 그대로 기록)"""
    fieldnames = ["month", "region", "category", "amount"]

    with file_path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames)
        writer.writeheader()
        for record in records:
            # model_dump()는 SalesRecord를 {"month": ..., "region": ..., ...} dict로 바꿔준다.
            # 필드를 하나씩 손으로 꺼내 dict를 새로 만드는 대신 이 메서드를 쓰는 게
            # 스키마가 바뀌어도 자동으로 따라가서 유지보수에 유리하다. (감점 대상 회피)
            writer.writerow(record.model_dump())


def save_error_records(errors: list[dict], file_path: Path) -> None:
    """errors를 JSON으로 저장합니다. ensure_ascii=False로 한글이 깨지지 않게 한다."""
    with file_path.open("w", encoding="utf-8") as file:
        # ensure_ascii=False를 빼먹으면 한글이 "서울" 같은 유니코드 이스케이프로
        # 저장돼 파일을 열었을 때 사람이 읽을 수 없게 된다. (감점 대상 회피)
        json.dump(errors, file, ensure_ascii=False, indent=2)


def load_csv_rows(file_path: Path) -> list[dict]:
    """저장된 valid_sales.csv를 다시 읽어 건수를 검증하기 위한 보조 함수."""
    with file_path.open("r", encoding="utf-8", newline="") as file:
        return list(csv.DictReader(file))


def main() -> None:
    # 문제 1: 존재하지 않는 파일은 None + logger.error + finally("로딩 종료")를 확인
    missing_result = safe_load_csv(Path(__file__).with_name("존재하지_않는_파일.json"))
    assert missing_result is None

    # 실제 입력 데이터 로드 (practice1에서 만든 100건짜리 JSON을 그대로 재사용)
    raw_data = safe_load_csv(INPUT_JSON_PATH)
    assert raw_data is not None
    total_input_count = len(raw_data)

    # 오류 처리 경로 시연을 위해 가짜 불량 레코드를 raw_data 뒤에 덧붙인다.
    # (실제 파일을 건드리지 않고 메모리 상의 리스트에만 추가하는 것이므로
    #  Python_Practice1_Data.json 자체는 그대로 100건 정상 데이터로 남는다.)
    raw_data = raw_data + INVALID_SAMPLE_RECORDS

    # 문제 2, 3: SalesRecord 검증 파이프라인
    valid_records, error_records = validate_sales_records(raw_data)
    assert len(valid_records) == total_input_count
    assert len(error_records) == len(INVALID_SAMPLE_RECORDS)

    # 문제 4: 결과 저장 및 재로딩 검증
    save_valid_records(valid_records, VALID_OUTPUT_PATH)
    save_error_records(error_records, ERRORS_OUTPUT_PATH)

    # 방금 저장한 CSV를 다시 읽어, 저장 전후로 건수가 달라지지 않았는지 확인한다.
    # (저장 로직에 버그가 있으면 여기서 assert가 실패해 바로 드러난다.)
    reloaded = load_csv_rows(VALID_OUTPUT_PATH)
    assert len(reloaded) == total_input_count

    print("전체 검사를 통과했습니다.\n")

    # 이하는 결과를 사람이 읽기 좋은 형태로 출력하는 부분 (검증에는 영향 없음)
    print("1. valid 레코드 수")
    print(len(valid_records))

    print("\n2. errors 레코드 수")
    print(len(error_records))

    print("\n3. errors 상세")
    for error in error_records:
        print(error)

    print("\n4. 저장 후 재로딩한 valid 레코드 수")
    print(len(reloaded))


if __name__ == "__main__":
    main()


# ------------------------------------------------------------------
# 회고
#
# 1) 예외 처리 + 파일 읽기 (safe_load_csv)
#    - try/except/else/finally를 각자 역할대로 나눠 쓰니 의도가 뚜렷해졌다.
#      try에는 "실패할 수 있는 코드"만, else에는 "성공했을 때만 할 일"을,
#      finally에는 "결과와 무관하게 항상 할 일"을 넣는 구분이 명확해서
#      나중에 로직을 추가할 때도 어디에 넣어야 할지 헷갈리지 않았다.
#    - 교수님 Q&A를 통해 "함수명은 safe_load_csv지만 실제로는 json.load를 쓴다"는
#      점을 배우면서, 함수 이름이 항상 구현을 100% 설명해주지는 않는다는 것과
#      docstring이 왜 중요한지를 다시 느꼈다.
#
# 2) Pydantic v2 스키마 정의
#    - Field(min_length=1), Field(gt=0) 같은 선언적 제약 조건만으로
#      "비어있으면 안 된다", "0보다 커야 한다" 같은 검증 로직을 코드 몇 줄로 끝낼 수 있었다.
#      직접 if not value: raise ... 를 여러 필드마다 반복했다면 훨씬 길고 실수하기 쉬웠을 것.
#    - field_validator(mode="before")로 "빈 문자열 -> None" 변환을 스키마 안에 캡슐화하니,
#      호출하는 쪽(validate_sales_records)은 이런 전처리를 전혀 신경 쓸 필요가 없어졌다.
#      검증 규칙과 호출 코드가 분리되는 게 왜 좋은지 체감했다.
#
# 3) 검증 파이프라인 (valid / errors 분리)
#    - 처음엔 str(error)로 오류를 저장했는데, error.errors(include_url=False)로 바꾸니
#      "어떤 필드(loc)가 어떤 이유(type/msg)로 실패했는지"가 구조화된 데이터로 남아
#      나중에 이 JSON을 다시 파싱해서 통계를 내거나 필터링하기 쉬워졌다.
#      사람이 읽기 좋은 로그(print)와 기계가 읽기 좋은 저장 포맷(구조화된 dict)은
#      다르게 설계해야 한다는 걸 배웠다.
#
# 4) 결과 파일 저장 + 재로딩 확인
#    - "저장하고 끝"이 아니라 저장한 파일을 다시 읽어 건수를 assert로 확인하는 과정에서,
#      DictWriter의 fieldnames 순서가 record.model_dump()의 키 순서와 안 맞으면
#      조용히 잘못된 열에 값이 들어갈 수 있다는 위험을 알게 됐다(이번엔 순서를 맞춰 회피).
#    - ensure_ascii=False를 빠뜨리면 한글이 깨진다는 감점 포인트를 직접 확인해보면서,
#      "동작은 하지만 사람이 못 읽는 결과물"도 결국 버그라는 인식을 갖게 됐다.
# ------------------------------------------------------------------
