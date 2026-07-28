--**Window Function(윈도우 함수)** 또는 **분석 함수**라고 합니다.
--`ROW_NUMBER`, `RANK`, `DENSE_RANK`, `NTILE`처럼 `OVER()`와 함께 사용하는 함수
--윈도우 함수는 순위, 누적 매출, 전월 대비 증감률, 이동평균, 고객별 최근 주문 분석에 특히 많이 사용됩니다.
--윈도우 함수는 `GROUP BY`와 달리 원래 행을 유지하면서 순위·누계·평균·이전 값 등을 계산합니다.
--
--## 1. 기본 구조
--
--```sql
--윈도우함수() OVER (
--    PARTITION BY 그룹기준
--    ORDER BY 정렬기준
--    ROWS BETWEEN 계산범위
--)
--```
--
--* `PARTITION BY`: 계산 그룹을 나눕니다.
--* `ORDER BY`: 그룹 안에서 계산 순서를 정합니다.
--* `ROWS BETWEEN`: 현재 행을 기준으로 계산 범위를 지정합니다.
--
-----
--
--# 자주 사용하는 Window Function
--
--## 2. 순위 함수
--
--| 함수             | 용도                 |
--| -------------- | ------------------ |
--| `ROW_NUMBER()` | 중복 없이 순번 부여        |
--| `RANK()`       | 공동 순위, 다음 순위 건너뜀   |
--| `DENSE_RANK()` | 공동 순위, 다음 순위 이어짐   |
--| `NTILE(n)`     | 전체 데이터를 n개 그룹으로 분할 |
--
--### 상품 단가 순위 비교
--
--```sql
SET search_path TO sales_lab, public;

SELECT
    s.sale_id,
    p.product_name,
    s.unit_price,
    ROW_NUMBER() OVER (
        ORDER BY s.unit_price DESC, s.sale_id
    ) AS row_number_rank,
    RANK() OVER (
        ORDER BY s.unit_price DESC
    ) AS rank_rank,
    DENSE_RANK() OVER (
        ORDER BY s.unit_price DESC
    ) AS dense_rank_rank,
    NTILE(4) OVER (
        ORDER BY s.unit_price DESC
    ) AS price_group
FROM sales s
JOIN products p
    ON p.product_id = s.product_id
ORDER BY s.unit_price DESC, s.sale_id;
--```

---

--## 3. 그룹별 순위
--
--`PARTITION BY`를 사용하면 카테고리별로 순위가 다시 1위부터 시작됩니다.
--
--### 카테고리별 상품 가격 순위
--
--```sql
SELECT
    c.category_name,
    p.product_name,
    p.list_price,
    RANK() OVER (
        PARTITION BY c.category_id
        ORDER BY p.list_price DESC
    ) AS category_price_rank
FROM products p
JOIN categories c
    ON c.category_id = p.category_id
ORDER BY c.category_name, category_price_rank;
--```
--
--예상 형태:
--
--| 카테고리 | 상품           |        가격 | 카테고리 순위 |
--| ---- | ------------ | --------: | ------: |
--| 노트북  | 게이밍 노트북 16   | 2,290,000 |       1 |
--| 노트북  | 비즈니스 노트북 15  | 1,780,000 |       2 |
--| 노트북  | 울트라북 14      | 1,450,000 |       3 |
--| 모니터  | 32인치 4K 모니터  |   690,000 |       1 |
--| 모니터  | 27인치 QHD 모니터 |   390,000 |       2 |

---
--
--## 4. `SUM()` 누적 합계
--
--`SUM()`도 `OVER()`를 붙이면 윈도우 함수가 됩니다.
--
--### 날짜순 누적 매출
--
--```sql
WITH daily_sales AS (
    SELECT
        o.order_date,
        ROUND(
            SUM(
                s.quantity
                * s.unit_price
                * (1 - s.discount_rate)
            ),2) AS daily_sales
    FROM orders o
    JOIN sales s
        ON s.order_id = o.order_id
    GROUP BY o.order_date
)
SELECT
    order_date,
    daily_sales,
    SUM(daily_sales) OVER (
        ORDER BY order_date
        ROWS BETWEEN UNBOUNDED PRECEDING
                 AND CURRENT ROW
    ) AS cumulative_sales
FROM daily_sales
ORDER BY order_date;
--```

--```sql
--ROWS BETWEEN UNBOUNDED PRECEDING
--         AND CURRENT ROW
--```
--의 의미는 다음과 같습니다.
--> 첫 번째 행부터 현재 행까지 모두 더한다.

---

--## 5. `AVG()` 이동평균
--
--이동평균은 일별·월별 매출의 추세를 부드럽게 확인할 때 사용합니다.
--
--### 최근 3개 판매일 이동평균

--```sql
WITH daily_sales AS (
    SELECT
        o.order_date,
        ROUND(
            SUM(
                s.quantity
                * s.unit_price
                * (1 - s.discount_rate)
            ), 2 ) AS daily_sales
    FROM orders o
    JOIN sales s
        ON s.order_id = o.order_id
    GROUP BY o.order_date
)
SELECT
    order_date,
    daily_sales,
    ROUND(
        AVG(daily_sales) OVER (
            ORDER BY order_date
            ROWS BETWEEN 2 PRECEDING
                     AND CURRENT ROW
        ), 2 ) AS moving_avg_3_days
FROM daily_sales
ORDER BY order_date;
--```
--계산 범위:
--```text
--2 PRECEDING + CURRENT ROW
--```
--즉, 현재 행을 포함한 최근 3개 행의 평균입니다.

---

--## 6. `COUNT()` 그룹별 전체 개수
--
--### 고객별 주문 목록과 고객의 전체 주문 수
--
--```sql
SELECT
    c.customer_id,
    c.customer_name,
    o.order_id,
    o.order_date,
    COUNT(*) OVER (
        PARTITION BY c.customer_id
    ) AS customer_order_count
FROM customers c
JOIN orders o
    ON o.customer_id = c.customer_id
ORDER BY c.customer_id, o.order_date;
--```
--`GROUP BY`를 사용하면 고객별 한 행으로 줄어들지만, 윈도우 함수를 사용하면 개별 주문 행이 유지됩니다.
-----

--## 7. `LAG()` 이전 행 가져오기
--
--`LAG()`는 이전 행의 값을 가져옵니다. 전월 대비 매출 증감 분석에 자주 사용됩니다.
--
--### 전월 매출과 증감액
--
--```sql
WITH monthly_sales AS (
    SELECT
        DATE_TRUNC('month', o.order_date)::date AS sales_month,
        ROUND(
            SUM(
                s.quantity
                * s.unit_price
                * (1 - s.discount_rate)
            ),
            2
        ) AS sales_amount
    FROM orders o
    JOIN sales s
        ON s.order_id = o.order_id
    GROUP BY DATE_TRUNC('month', o.order_date)
),
sales_comparison AS (
    SELECT
        sales_month,
        sales_amount,
        LAG(sales_amount) OVER (
            ORDER BY sales_month
        ) AS previous_month_sales
    FROM monthly_sales
)
SELECT
    sales_month,
    sales_amount,
    previous_month_sales,
    sales_amount - previous_month_sales AS sales_difference,
    ROUND(
        (sales_amount - previous_month_sales)
        / NULLIF(previous_month_sales, 0)
        * 100,
        2
    ) AS growth_rate
FROM sales_comparison
ORDER BY sales_month;
--```
--
--`NULLIF(previous_month_sales, 0)`은 0으로 나누는 오류를 방지합니다.

---
--
--## 8. `LEAD()` 다음 행 가져오기
--
--`LEAD()`는 다음 행의 값을 가져옵니다.
--
--### 현재 주문과 다음 주문 날짜 비교
--
--```sql
SELECT
    c.customer_name,
    o.order_id,
    o.order_date,
    LEAD(o.order_date) OVER (
        PARTITION BY c.customer_id
        ORDER BY o.order_date
    ) AS next_order_date,
    LEAD(o.order_date) OVER (
        PARTITION BY c.customer_id
        ORDER BY o.order_date
    ) - o.order_date AS days_until_next_order
FROM customers c
JOIN orders o
    ON o.customer_id = c.customer_id
ORDER BY c.customer_id, o.order_date;
--```
--
--고객별 마지막 주문은 다음 주문이 없으므로 `NULL`이 나옵니다.

---
--
--## 9. `FIRST_VALUE()` 그룹의 첫 번째 값
--
--### 카테고리별 최고가 상품과 비교
--
--```sql
SELECT
    c.category_name,
    p.product_name,
    p.list_price,
    FIRST_VALUE(p.product_name) OVER (
        PARTITION BY c.category_id
        ORDER BY p.list_price DESC
    ) AS highest_price_product,
    FIRST_VALUE(p.list_price) OVER (
        PARTITION BY c.category_id
        ORDER BY p.list_price DESC
    ) AS highest_price
FROM products p
JOIN categories c
    ON c.category_id = p.category_id
ORDER BY c.category_name, p.list_price DESC;
--```

---
--
--## 10. `LAST_VALUE()` 그룹의 마지막 값
--
--### 카테고리별 최저가 상품과 비교
--
--```sql
SELECT
    c.category_name,
    p.product_name,
    p.list_price,
    LAST_VALUE(p.product_name) OVER (
        PARTITION BY c.category_id
        ORDER BY p.list_price DESC
        ROWS BETWEEN UNBOUNDED PRECEDING
                 AND UNBOUNDED FOLLOWING
    ) AS lowest_price_product,
    LAST_VALUE(p.list_price) OVER (
        PARTITION BY c.category_id
        ORDER BY p.list_price DESC
        ROWS BETWEEN UNBOUNDED PRECEDING
                 AND UNBOUNDED FOLLOWING
    ) AS lowest_price
FROM products p
JOIN categories c
    ON c.category_id = p.category_id
ORDER BY c.category_name, p.list_price DESC;
--```
--
--`LAST_VALUE()`에서는 다음 범위를 지정하는 것이 중요합니다.
--
--```sql
--ROWS BETWEEN UNBOUNDED PRECEDING
--         AND UNBOUNDED FOLLOWING
--```
--
--지정하지 않으면 기본 계산 범위가 현재 행까지만 적용되어, 예상한 마지막 값이 나오지 않을 수 있습니다.

---

--## 11. `PERCENT_RANK()` 상대 순위
--
--최상위 행을 `0`, 최하위 행을 `1`에 가깝게 표현합니다.
--
--```sql
SELECT
    product_name,
    list_price,
    RANK() OVER (
        ORDER BY list_price DESC
    ) AS price_rank,
    ROUND(
        PERCENT_RANK() OVER (
            ORDER BY list_price DESC
        )::numeric,
        3
    ) AS percent_rank
FROM products
ORDER BY list_price DESC;
--```
--
--계산 개념은 다음과 같습니다.
--
--```text
--현재 순위 - 1
--──────────────
--전체 행 수 - 1
--```

---

--## 12. `CUME_DIST()` 누적 분포
--
--현재 값까지 전체 데이터의 몇 퍼센트가 포함되는지 계산합니다.
--
--```sql
SELECT
    product_name,
    list_price,
    ROUND(
        CUME_DIST() OVER (
            ORDER BY list_price DESC
        )::numeric,
        3
    ) AS cumulative_distribution
FROM products
ORDER BY list_price DESC;
--```
--
--`PERCENT_RANK()`와 `CUME_DIST()`는 비슷해 보이지만 동점 처리와 계산 기준이 다릅니다.
--
--| 함수               | 계산 의미               |
--| ---------------- | ------------------- |
--| `PERCENT_RANK()` | 현재 순위의 상대적인 위치      |
--| `CUME_DIST()`    | 현재 값까지 포함된 행의 누적 비율 |

---

--## 13. 전체 평균과 개별 판매 비교
--
--집계 함수에 `OVER()`만 붙이면 전체 평균을 각 행 옆에 표시할 수 있습니다.
--
--```sql
SELECT
    s.sale_id,
    p.product_name,
    s.unit_price,
    ROUND(
        AVG(s.unit_price) OVER (),
        2
    ) AS overall_avg_price,
    s.unit_price
        - ROUND(AVG(s.unit_price) OVER (), 2)
        AS difference_from_avg
FROM sales s
JOIN products p
    ON p.product_id = s.product_id
ORDER BY s.unit_price DESC;
--```
--
--`OVER()` 안이 비어 있으면 전체 조회 결과를 하나의 그룹으로 계산합니다.

---

--## 자주 사용하는 함수 정리
--
--| 분류    | 함수               | 주요 용도            |
--| ----- | ---------------- | ---------------- |
--| 순위    | `ROW_NUMBER()`   | 고유 순번            |
--| 순위    | `RANK()`         | 공동 순위, 다음 순위 건너뜀 |
--| 순위    | `DENSE_RANK()`   | 공동 순위, 순위 연속     |
--| 구간    | `NTILE(n)`       | 데이터를 n개 그룹으로 분할  |
--| 집계    | `SUM()`          | 누적 합계            |
--| 집계    | `AVG()`          | 이동평균·그룹 평균       |
--| 집계    | `COUNT()`        | 그룹별 행 개수         |
--| 이전 값  | `LAG()`          | 전월·이전 주문 비교      |
--| 다음 값  | `LEAD()`         | 다음 주문·다음 날짜 비교   |
--| 첫 값   | `FIRST_VALUE()`  | 그룹의 첫 번째 값       |
--| 마지막 값 | `LAST_VALUE()`   | 그룹의 마지막 값        |
--| 상대 순위 | `PERCENT_RANK()` | 순위의 상대적 위치       |
--| 누적 분포 | `CUME_DIST()`    | 현재 값까지의 누적 비율    |
--
--핵심 차이는 다음과 같습니다.

--```sql
---- GROUP BY: 여러 행을 한 행으로 축소
--SELECT region, SUM(sales_amount)
--FROM ...
--GROUP BY region;
--```
--
--```sql
---- Window Function: 원래 행을 유지하면서 계산 결과 추가
--SELECT
--    region,
--    sales_amount,
--    SUM(sales_amount) OVER (PARTITION BY region)
--FROM ...;
--```

--따라서 윈도우 함수는 **순위, 누적 매출, 전월 대비 증감률, 이동평균, 고객별 최근 주문 분석**에 특히 많이 사용됩니다.
