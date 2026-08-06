# ------------------------------------------------------------------
# 작성자      : 임채현
# 작성목적    : [실습 1] 자료구조 집계·컴프리헨션·제너레이터 검증 스크립트
#               practice1.py의 문제 1~4 구현 결과를 assert로 검증
# 작성일      : 2026-08-06
# 변경사항 내역 (날짜, 변경목적, 변경내용 순으로 기입)
# 2026-08-06, 최초 작성, 문제 1~4 assert 검증 코드 작성
# 2026-08-06, 가독성 개선, practice1.py 리네이밍에 맞춰 import/변수명 동기화
#
# ------------------------------------------------------------------
import sys
from pathlib import Path
from types import GeneratorType

from practice1 import (
    collect_amounts_by_category,
    compute_monthly_category_sales,
    compute_region_sales_total,
    iter_sales_over_threshold,
    rank_top_monthly_categories,
    read_sales_records,
    select_sales_over_threshold,
    tally_transactions_by_region,
)


SALES_DATA_PATH = Path(__file__).with_name("Python_Practice1_Data.json")


def pass_message(message: str) -> None:
    print(f"[PASS] {message}")


def main() -> None:
    records = read_sales_records(SALES_DATA_PATH)

    assert len(records) == 100
    pass_message("JSON 데이터 로드")

    # ===== 문제 1: 리스트/딕셔너리 컴프리헨션 =====
    sales_over_threshold = select_sales_over_threshold(records)
    assert len(sales_over_threshold) == 47
    assert all(record["amount"] >= 1000 for record in sales_over_threshold)
    pass_message("amount >= 1000 거래 필터링")

    region_sales_total = compute_region_sales_total(records)
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
    pass_message("지역별 총매출 계산")

    # ===== 문제 2: Counter + defaultdict =====
    region_transaction_count = tally_transactions_by_region(records)
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
    pass_message("Counter 지역별 거래 건수")

    category_amount_map = collect_amounts_by_category(records)
    assert sum(len(amounts) for amounts in category_amount_map.values()) == len(records)
    assert len(category_amount_map["전자"]) == 39
    pass_message("defaultdict 카테고리별 금액 그룹화")

    # ===== 문제 3: 제너레이터 — 메모리 비교 =====
    sales_generator = iter_sales_over_threshold(records)
    sales_list = [record for record in records if record["amount"] > 1000]

    assert isinstance(sales_generator, GeneratorType)
    assert len(sales_list) == 47

    generator_size = sys.getsizeof(sales_generator)
    list_size = sys.getsizeof(sales_list)
    print(f"[INFO] 리스트 크기: {list_size} bytes")
    print(f"[INFO] 제너레이터 크기: {generator_size} bytes")
    print(
        "[INFO] 제너레이터 객체가 더 작은가:",
        generator_size < list_size,
    )

    generated_sales = list(iter_sales_over_threshold(records))
    assert generated_sales == sales_list
    pass_message("제너레이터 생성 및 결과 검증")

    # ===== 문제 4: 종합 - 월별 카테고리 매출 집계 =====
    monthly_category_sales = compute_monthly_category_sales(records)
    aggregated_total = sum(
        amount
        for category_totals in monthly_category_sales.values()
        for amount in category_totals.values()
    )
    assert aggregated_total == 101460
    pass_message("월별 카테고리 매출 집계")

    # 체크포인트에 없는 추가 기능: top3 정렬 검증
    top_categories = rank_top_monthly_categories(monthly_category_sales)
    assert top_categories == [
        ("2024-02", "전자", 15240),
        ("2024-04", "전자", 14010),
        ("2024-03", "전자", 13820),
    ]
    pass_message("상위 3개 내림차순 정렬")

    print("\n전체 검사를 통과했습니다.")


if __name__ == "__main__":
    main()
