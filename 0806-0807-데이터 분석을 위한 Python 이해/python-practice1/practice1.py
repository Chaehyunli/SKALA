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
#
# ------------------------------------------------------------------
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path
from pprint import pprint


SALES_DATA_PATH = Path(__file__).with_name("Python_Practice1_Data.json")


def read_sales_records(file_path: Path) -> list[dict]:
    """JSON 파일에서 Sales 목록을 읽어 반환합니다."""
    with file_path.open("r", encoding="utf-8") as file:
        data = json.load(file)

    return data["Sales"]


# ===== 문제 1: 리스트/딕셔너리 컴프리헨션 =====
def select_sales_over_threshold(records: list[dict]) -> list[dict]:
    """amount가 1000 이상인 거래를 반환합니다. (리스트 컴프리헨션)"""
    return [record for record in records if record["amount"] >= 1000]


def compute_region_sales_total(records: list[dict]) -> dict[str, int]:
    """지역별 총매출을 딕셔너리 컴프리헨션으로 계산합니다."""
    region_names = sorted({record["region"] for record in records})

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
    return Counter(record["region"] for record in records)


def collect_amounts_by_category(records: list[dict]) -> dict[str, list[int]]:
    """defaultdict를 사용해 카테고리별 amount 목록을 만듭니다."""
    category_amount_map = defaultdict(list)

    for record in records:
        category_amount_map[record["category"]].append(record["amount"])

    return dict(category_amount_map)


# ===== 문제 3: 제너레이터 — 메모리 비교 =====
def iter_sales_over_threshold(records: list[dict]):
    """amount가 1000보다 큰 거래를 하나씩 yield하는 제너레이터입니다."""
    for record in records:
        if record["amount"] > 1000:
            yield record


# ===== 문제 4: 종합 - 월별 카테고리 매출 집계 =====
def compute_monthly_category_sales(
    records: list[dict],
) -> dict[str, dict[str, int]]:
    """월과 카테고리를 기준으로 총매출을 계산합니다. (컴프리헨션 + defaultdict)"""
    monthly_category_sales = defaultdict(lambda: defaultdict(int))

    for record in records:
        month_key = record["month"]
        monthly_category_sales[month_key][record["category"]] += record["amount"]

    return {
        month_key: dict(category_totals)
        for month_key, category_totals in monthly_category_sales.items()
    }


# 체크포인트에 없는 추가 기능: 월별 카테고리 매출 상위 3개 정렬
def rank_top_monthly_categories(
    monthly_category_sales: dict[str, dict[str, int]],
) -> list[tuple[str, str, int]]:
    """월과 카테고리 조합 중 매출 상위 3개를 반환합니다."""
    flattened_entries = [
        (month_key, category, amount)
        for month_key, category_totals in monthly_category_sales.items()
        for category, amount in category_totals.items()
    ]

    return sorted(
        flattened_entries,
        key=lambda entry: entry[2],
        reverse=True,
    )[:3]


def main() -> None:
    records = read_sales_records(SALES_DATA_PATH)

    sales_over_threshold = select_sales_over_threshold(records)
    region_sales_total = compute_region_sales_total(records)
    region_transaction_count = tally_transactions_by_region(records)
    category_amount_map = collect_amounts_by_category(records)

    # 문제 3: 제너레이터 vs 리스트 메모리 비교
    sales_generator = iter_sales_over_threshold(records)
    sales_list = [record for record in records if record["amount"] > 1000]
    generator_size = sys.getsizeof(sales_generator)
    list_size = sys.getsizeof(sales_list)

    monthly_category_sales = compute_monthly_category_sales(records)
    top_categories = rank_top_monthly_categories(monthly_category_sales)

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
