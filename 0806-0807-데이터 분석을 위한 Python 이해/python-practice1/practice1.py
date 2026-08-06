# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [실습 1] 자료구조 집계·컴프리헨션·제너레이터 실습
#               1) 리스트/딕셔너리 컴프리헨션으로 고액 거래 필터링 및 지역별 총매출 계산
#               2) Counter로 지역별 거래 건수, defaultdict로 카테고리별 금액 그룹화
#               3) 제너레이터로 고액 거래를 순회하며 리스트 대비 메모리 사용량 비교
#               4) 컴프리헨션 + defaultdict로 월별·카테고리별 매출 집계
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, 문제 1~4 구현
# 2026-08-06, 가독성 개선, 함수/변수명을 의미 중심으로 리네이밍
# 2026-08-06, 검증 강화, verify_results에 generator_size < list_size assert 추가
# 2026-08-06, 문서화, 상세 주석 및 회고 추가
#
# ------------------------------------------------------------------
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path
from pprint import pprint
from types import GeneratorType


# 실습용 데이터 파일은 이 스크립트와 같은 폴더에 있다고 가정한다.
SALES_DATA_PATH = Path(__file__).with_name("Python_Practice1_Data.json")


def read_sales_records(file_path: Path) -> list[dict]:
    """JSON 파일에서 Sales 목록을 읽어 반환합니다."""
    with file_path.open("r", encoding="utf-8") as file:
        data = json.load(file)

    # 원본 JSON은 {"Sales": [...]} 구조이므로 "Sales" 키의 리스트만 꺼내온다.
    return data["Sales"]


# ===== 문제 1: 리스트/딕셔너리 컴프리헨션 =====
def select_sales_over_threshold(records: list[dict]) -> list[dict]:
    """amount가 1000 이상인 거래를 반환합니다. (리스트 컴프리헨션)"""
    # for 루프 + append 대신 리스트 컴프리헨션 한 줄로 필터링한다. (감점 대상 1 회피)
    return [record for record in records if record["amount"] >= 1000]


def compute_region_sales_total(records: list[dict]) -> dict[str, int]:
    """지역별 총매출을 딕셔너리 컴프리헨션으로 계산합니다."""
    # set 컴프리헨션으로 중복 없는 지역명을 먼저 뽑고 정렬해 출력 순서를 고정한다.
    region_names = sorted({record["region"] for record in records})

    # 지역마다 해당 지역 거래의 amount만 골라 합산하는 딕셔너리 컴프리헨션.
    # (region_total[region] = 0으로 초기화 후 += 하는 for 루프 방식 대신 사용)
    return {
        region: sum(
            record["amount"]
            for record in records
            if record["region"] == region
        )
        for region in region_names
    }


# ===== 문제 2: Counter + defaultdict =====
def tally_transactions_by_region(records: list[dict]) -> Counter:
    """Counter를 사용해 지역별 거래 건수를 계산합니다."""
    # 직접 카운터 dict를 만들며 +=1 하는 대신 Counter에 지역명 이터러블을 바로 넘긴다.
    # (감점 대상 4 회피: Counter 대신 직접 루프로 카운팅하지 않음)
    return Counter(record["region"] for record in records)


def collect_amounts_by_category(records: list[dict]) -> dict[str, list[int]]:
    """defaultdict를 사용해 카테고리별 amount 목록을 만듭니다."""
    # defaultdict(list)를 쓰면 "if key not in dict: dict[key] = []" 같은
    # 존재 여부 체크 없이 바로 append할 수 있다. (감점 대상 2 회피)
    category_amount_map = defaultdict(list)

    for record in records:
        category_amount_map[record["category"]].append(record["amount"])

    # 반환 타입을 일반 dict로 맞춰서 호출부가 defaultdict 내부 동작에 의존하지 않게 한다.
    return dict(category_amount_map)


# ===== 문제 3: 제너레이터 — 메모리 비교 =====
def iter_sales_over_threshold(records: list[dict]):
    """amount가 1000보다 큰 거래를 하나씩 yield하는 제너레이터입니다."""
    # 리스트를 통째로 만들지 않고 yield로 한 건씩 넘겨주므로,
    # 이 함수를 호출한 시점에는 아직 아무 계산도 일어나지 않는다(지연 평가).
    for record in records:
        if record["amount"] > 1000:
            yield record


# ===== 문제 4: 종합 - 월별 카테고리 매출 집계 =====
def compute_monthly_category_sales(
    records: list[dict],
) -> dict[str, dict[str, int]]:
    """월과 카테고리를 기준으로 총매출을 계산합니다. (컴프리헨션 + defaultdict)"""
    # 2단계로 중첩된 defaultdict: 월 -> 카테고리 -> 누적 합계.
    # defaultdict(int) 덕분에 최초 등장하는 (월, 카테고리) 조합도 바로 += 가능하다.
    monthly_category_sales = defaultdict(lambda: defaultdict(int))

    for record in records:
        month_key = record["month"]
        monthly_category_sales[month_key][record["category"]] += record["amount"]

    # 중첩 딕셔너리 컴프리헨션으로 defaultdict를 순수 dict로 변환해 반환한다.
    # (외부에서 존재하지 않는 월을 조회할 때 빈 defaultdict가 자동 생성되는 부작용 방지)
    return {
        month_key: dict(category_totals)
        for month_key, category_totals in monthly_category_sales.items()
    }


# 체크포인트에 없는 추가 기능: 월별 카테고리 매출 상위 3개 정렬
def rank_top_monthly_categories(
    monthly_category_sales: dict[str, dict[str, int]],
) -> list[tuple[str, str, int]]:
    """월과 카테고리 조합 중 매출 상위 3개를 반환합니다."""
    # 중첩 딕셔너리를 (월, 카테고리, 금액) 튜플의 평평한 리스트로 펼친다.
    flattened_entries = [
        (month_key, category, amount)
        for month_key, category_totals in monthly_category_sales.items()
        for category, amount in category_totals.items()
    ]

    # 금액(세 번째 요소) 기준 내림차순 정렬 후 상위 3개만 슬라이싱.
    return sorted(
        flattened_entries,
        key=lambda entry: entry[2],
        reverse=True,
    )[:3]


def verify_results(
    records: list[dict],
    sales_over_threshold: list[dict],
    region_sales_total: dict[str, int],
    region_transaction_count: Counter,
    category_amount_map: dict[str, list[int]],
    sales_generator,
    sales_list: list[dict],
    generator_size: int,
    list_size: int,
    monthly_category_sales: dict[str, dict[str, int]],
    top_categories: list[tuple[str, str, int]],
) -> None:
    """각 문제의 계산 결과가 기대값과 일치하는지 assert로 검증합니다.

    practice1_check.py의 검증 내용을 그대로 practice1.py 안에 옮겨,
    스크립트를 실행하는 것만으로 결과의 정확성까지 바로 확인할 수 있게 한다.
    """
    assert len(records) == 100

    # 문제 1: 필터링 및 지역별 총매출 검증
    assert len(sales_over_threshold) == 47
    assert all(record["amount"] >= 1000 for record in sales_over_threshold)
    assert region_sales_total == {
        "광주": 9620,
        "대구": 12660,
        "대전": 11140,
        "부산": 10930,
        "서울": 20060,
        "세종": 10820,
        "울산": 11700,
        "인천": 14530,
    }
    assert sum(region_sales_total.values()) == 101460

    # 문제 2: Counter 순서, defaultdict 그룹화 결과 검증
    assert region_transaction_count.most_common() == [
        ("서울", 14),
        ("부산", 13),
        ("대구", 13),
        ("인천", 12),
        ("광주", 12),
        ("대전", 12),
        ("울산", 12),
        ("세종", 12),
    ]
    assert sum(len(amounts) for amounts in category_amount_map.values()) == len(records)
    assert len(category_amount_map["전자"]) == 39

    # 문제 3: 제너레이터가 실제로 리스트보다 메모리를 덜 쓰는지 검증
    assert isinstance(sales_generator, GeneratorType)
    assert len(sales_list) == 47
    assert generator_size < list_size

    # 문제 4: 월별·카테고리별 집계 총합과 top3 순위 검증
    aggregated_total = sum(
        amount
        for category_totals in monthly_category_sales.values()
        for amount in category_totals.values()
    )
    assert aggregated_total == 101460
    assert top_categories == [
        ("2024-02", "전자", 15240),
        ("2024-04", "전자", 14010),
        ("2024-03", "전자", 13820),
    ]

    print("전체 검사를 통과했습니다.\n")


def main() -> None:
    # 데이터 로드: JSON -> list[dict]
    records = read_sales_records(SALES_DATA_PATH)

    # 문제 1: 컴프리헨션 기반 필터링 / 집계
    sales_over_threshold = select_sales_over_threshold(records)
    region_sales_total = compute_region_sales_total(records)

    # 문제 2: Counter / defaultdict 기반 집계
    region_transaction_count = tally_transactions_by_region(records)
    category_amount_map = collect_amounts_by_category(records)

    # 문제 3: 제너레이터 vs 리스트 메모리 비교
    # iter_sales_over_threshold(records)를 호출한 시점에는 아직 실행되지 않고,
    # sys.getsizeof는 "제너레이터 객체 자체"의 크기만 재기 때문에 원소 개수와 무관하게 작다.
    sales_generator = iter_sales_over_threshold(records)
    sales_list = [record for record in records if record["amount"] > 1000]
    generator_size = sys.getsizeof(sales_generator)
    list_size = sys.getsizeof(sales_list)

    # 문제 4: 월별 카테고리 매출 집계 + 추가 기능(top3)
    monthly_category_sales = compute_monthly_category_sales(records)
    top_categories = rank_top_monthly_categories(monthly_category_sales)

    # 위에서 계산한 모든 결과를 assert로 한 번에 검증한다.
    verify_results(
        records,
        sales_over_threshold,
        region_sales_total,
        region_transaction_count,
        category_amount_map,
        sales_generator,
        sales_list,
        generator_size,
        list_size,
        monthly_category_sales,
        top_categories,
    )

    # 이하는 결과를 사람이 읽기 좋은 형태로 출력하는 부분 (검증에는 영향 없음)
    print("1. 전체 거래 건수")
    print(len(records))

    print("\n2. amount >= 1000인 거래 건수")
    print(len(sales_over_threshold))

    print("\n3. 지역별 총매출")
    pprint(region_sales_total, sort_dicts=False)

    print("\n4. 지역별 거래 건수")
    pprint(region_transaction_count.most_common())

    print("\n5. 카테고리별 amount 목록")
    for category, amounts in category_amount_map.items():
        print(f"{category}: {amounts}")

    print("\n6. 리스트와 제너레이터 메모리 비교")
    print(f"리스트 크기: {list_size} bytes")
    print(f"제너레이터 크기: {generator_size} bytes")
    print("제너레이터 객체가 더 작은가:", generator_size < list_size)

    print("\n7. 월별 카테고리 매출")
    pprint(monthly_category_sales, sort_dicts=False)

    print("\n8. 월별 카테고리 매출 상위 3개")
    pprint(top_categories)


if __name__ == "__main__":
    main()


# ------------------------------------------------------------------
# 회고
#
# 1) 리스트/딕셔너리 컴프리헨션
#    - select_sales_over_threshold, compute_region_sales_total에서
#      for/append 없이 컴프리헨션만으로 필터링과 집계를 끝낼 수 있었다.
#    - 다만 compute_region_sales_total은 지역 수만큼 records를 반복 순회해
#      O(지역 수 × 거래 수)라는 점이 걸림. 데이터가 커지면 문제 2의
#      defaultdict 누적 방식(O(거래 수))이 더 유리하다는 걸 체감했다.
#
# 2) Counter + defaultdict
#    - Counter(제너레이터 표현식)만으로 지역별 카운트와 most_common() 정렬까지
#      한 번에 해결되어 직접 dict를 갱신하는 코드보다 훨씬 짧고 의도가 명확했다.
#    - defaultdict(list)는 "키가 없으면 만들고, 있으면 추가"라는 분기를
#      완전히 없애줘서 group-by 패턴에서 가장 먼저 떠올려야 할 도구라고 느꼈다.
#
# 3) 제너레이터 — 메모리 비교
#    - sys.getsizeof(제너레이터) < sys.getsizeof(리스트)를 실제로 확인하며,
#      제너레이터가 "원소를 미리 다 만들지 않고 필요할 때 하나씩 계산한다"는
#      개념을 수치로 체감할 수 있었다.
#    - 검증 시 제너레이터를 list()로 미리 변환해버리면 비교 자체가 무의미해지므로,
#      sys.getsizeof는 반드시 아직 소비하지 않은 제너레이터 객체에 호출해야 한다는
#      점을 practice1_check.py를 작성하며 다시 확인했다.
#
# 4) 종합 - 월별 카테고리 매출 집계
#    - 중첩 defaultdict(월 -> 카테고리 -> 합계)로 2단계 그룹화를 하고,
#      마지막에 dict 컴프리헨션으로 순수 dict로 바꿔 반환하는 패턴이
#      "집계는 defaultdict로, 반환은 dict로"라는 원칙을 지키기 좋았다.
#    - top3 정렬처럼 체크포인트에 없는 요구사항도 기존 함수들을 조합해
#      추가하기 쉬웠던 걸 보면, 각 함수가 한 가지 책임만 갖도록
#      나눈 설계가 확장에도 도움이 됐다.
# ------------------------------------------------------------------
