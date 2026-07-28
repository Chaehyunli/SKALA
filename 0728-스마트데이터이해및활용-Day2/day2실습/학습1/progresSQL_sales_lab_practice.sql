-- =====================================================================
-- PostgreSQL 판매관리 종합 실습 및 정답
-- 사전 실행: progresSQL_sales_ddl.sql
-- GROUP BY 5 / 서브쿼리 3 / CTE 2 / JOIN 5 / VIEW 2
-- MATERIALIZED VIEW 2 / CUBE 1 / ROLLUP 1
-- =====================================================================

SET search_path TO sales_lab, public;

-- 공통 판매금액 공식
-- quantity * unit_price * (1 - discount_rate)

-- =====================================================================
-- A. GROUP BY 5개
-- =====================================================================

-- [GROUP BY 1] 지역별 고객 수
-- 설명: 고객을 region으로 묶어 지역별 회원 규모를 확인합니다.
SELECT
    region,
    COUNT(*) AS customer_count
FROM customers
GROUP BY region
ORDER BY customer_count DESC, region;

-- [GROUP BY 2] 고객 등급별 고객 수와 가입일 범위
-- 설명: 등급별 인원뿐 아니라 최초·최근 가입일도 함께 집계합니다.
SELECT
    customer_grade,
    COUNT(*) AS customer_count,
    MIN(signup_date) AS first_signup_date,
    MAX(signup_date) AS last_signup_date
FROM customers
GROUP BY customer_grade
ORDER BY customer_count DESC, customer_grade;

-- [GROUP BY 3] 카테고리별 상품 수·평균 가격·총 재고
-- 설명: 카테고리에 상품이 없어도 표시하기 위해 LEFT JOIN을 사용합니다.
SELECT
    c.category_id,
    c.category_name,
    COUNT(p.product_id) AS product_count,
    ROUND(AVG(p.list_price), 2) AS avg_list_price,
    COALESCE(SUM(p.stock_qty), 0) AS total_stock
FROM categories c
LEFT JOIN products p ON p.category_id = c.category_id
GROUP BY c.category_id, c.category_name
ORDER BY c.category_id;

-- [GROUP BY 4] 월별 주문 수와 판매액
-- 설명: DATE_TRUNC로 주문일을 월 단위로 맞춘 후 집계합니다.
--       취소 주문은 판매 상세가 없으므로 금액은 0으로 처리됩니다.
SELECT
    DATE_TRUNC('month', o.order_date)::date AS order_month,
    COUNT(DISTINCT o.order_id) AS order_count,
    COALESCE(
        ROUND(SUM(s.quantity * s.unit_price * (1 - s.discount_rate)), 2), 0
    ) AS sales_amount
FROM orders o
LEFT JOIN sales s ON s.order_id = o.order_id
GROUP BY DATE_TRUNC('month', o.order_date)
ORDER BY order_month;

-- [GROUP BY 5] 판매액이 100만원 이상인 고객
-- 설명: HAVING은 GROUP BY로 계산된 고객별 판매액을 필터링합니다.
SELECT
    c.customer_id,
    c.customer_name,
    COUNT(DISTINCT o.order_id) AS order_count,
    ROUND(SUM(s.quantity * s.unit_price * (1 - s.discount_rate)), 2) AS sales_amount
FROM customers c
JOIN orders o ON o.customer_id = c.customer_id
JOIN sales s ON s.order_id = o.order_id
GROUP BY c.customer_id, c.customer_name
HAVING SUM(s.quantity * s.unit_price * (1 - s.discount_rate)) >= 1000000
ORDER BY sales_amount DESC;

-- =====================================================================
-- B. 서브쿼리 3개
-- =====================================================================

-- [서브쿼리 1] 전체 상품 평균 가격보다 비싼 상품
-- 설명: 단일 행 서브쿼리의 평균값과 각 상품 가격을 비교합니다.
SELECT
    product_id,
    product_name,
    list_price
FROM products
WHERE list_price > (SELECT AVG(list_price) FROM products)
ORDER BY list_price DESC;

-- [서브쿼리 2] 주문한 적이 있는 고객(Semi-Join)
-- 설명: EXISTS는 일치하는 주문의 존재 여부만 검사하므로 고객이 중복되지 않습니다.
SELECT
    c.customer_id,
    c.customer_name,
    c.customer_grade
FROM customers c
WHERE EXISTS (
    SELECT 1
    FROM orders o
    WHERE o.customer_id = c.customer_id
)
ORDER BY c.customer_id;

-- [서브쿼리 3] 한 번도 판매되지 않은 상품(Anti-Join)
-- 설명: NOT EXISTS로 판매 상세가 전혀 없는 상품을 찾습니다.
SELECT
    p.product_id,
    p.product_name,
    p.stock_qty,
    p.active
FROM products p
WHERE NOT EXISTS (
    SELECT 1
    FROM sales s
    WHERE s.product_id = p.product_id
)
ORDER BY p.product_id;

-- =====================================================================
-- C. CTE 2개
-- =====================================================================

-- [CTE 1] 주문별 금액을 먼저 계산한 후 고액 주문 조회
-- 설명: 복잡한 금액 계산을 CTE에 한 번 정의하여 메인 쿼리를 단순화합니다.
WITH order_totals AS (
    SELECT
        o.order_id,
        o.customer_id,
        o.order_date,
        o.order_status,
        COALESCE(
            ROUND(SUM(s.quantity * s.unit_price * (1 - s.discount_rate)), 2), 0
        ) AS order_amount
    FROM orders o
    LEFT JOIN sales s ON s.order_id = o.order_id
    GROUP BY o.order_id, o.customer_id, o.order_date, o.order_status
)
SELECT
    ot.order_id,
    c.customer_name,
    ot.order_date,
    ot.order_status,
    ot.order_amount
FROM order_totals ot
JOIN customers c ON c.customer_id = ot.customer_id
WHERE ot.order_amount >= 1000000
ORDER BY ot.order_amount DESC;

-- [CTE 2] 고객별 구매액과 구매등급 계산
-- 설명: 첫 번째 CTE에서 주문별, 두 번째 CTE에서 고객별로 단계적으로 집계합니다.
WITH order_totals AS (
    SELECT
        o.order_id,
        o.customer_id,
        SUM(s.quantity * s.unit_price * (1 - s.discount_rate)) AS order_amount
    FROM orders o
    JOIN sales s ON s.order_id = o.order_id
    GROUP BY o.order_id, o.customer_id
),
customer_totals AS (
    SELECT
        customer_id,
        COUNT(*) AS purchase_count,
        SUM(order_amount) AS total_amount
    FROM order_totals
    GROUP BY customer_id
)
SELECT
    c.customer_id,
    c.customer_name,
    ct.purchase_count,
    ROUND(ct.total_amount, 2) AS total_amount,
    CASE
        WHEN ct.total_amount >= 5000000 THEN '최우수 구매고객'
        WHEN ct.total_amount >= 2000000 THEN '우수 구매고객'
        ELSE '일반 구매고객'
    END AS purchase_grade
FROM customer_totals ct
JOIN customers c ON c.customer_id = ct.customer_id
ORDER BY ct.total_amount DESC;

-- =====================================================================
-- D. JOIN 5개: 내부·외부 JOIN 혼합
-- =====================================================================

-- [JOIN 1: INNER JOIN] 주문과 주문 고객
-- 설명: 고객이 존재하는 정상 주문만 고객 정보와 결합합니다.
SELECT
    o.order_id,
    o.order_date,
    c.customer_name,
    c.region,
    o.order_status
FROM orders o
INNER JOIN customers c ON c.customer_id = o.customer_id
ORDER BY o.order_date, o.order_id;

-- [JOIN 2: 다중 INNER JOIN] 판매 상품 상세
-- 설명: sales를 중심으로 orders, customers, products를 연결합니다.
SELECT
    s.sale_id,
    o.order_id,
    o.order_date,
    c.customer_name,
    p.product_name,
    s.quantity,
    s.unit_price,
    s.discount_rate,
    ROUND(s.quantity * s.unit_price * (1 - s.discount_rate), 2) AS sale_amount
FROM sales s
JOIN orders o ON o.order_id = s.order_id
JOIN customers c ON c.customer_id = o.customer_id
JOIN products p ON p.product_id = s.product_id
ORDER BY o.order_id, s.sale_id;

-- [JOIN 3: LEFT OUTER JOIN] 주문하지 않은 고객 포함
-- 설명: 왼쪽의 모든 고객을 유지하므로 주문이 없는 고객도 표시됩니다.
SELECT
    c.customer_id,
    c.customer_name,
    o.order_id,
    o.order_date,
    COALESCE(o.order_status, '주문 없음') AS order_status
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.customer_id
ORDER BY c.customer_id, o.order_date;

-- [JOIN 4: LEFT OUTER JOIN] 판매되지 않은 상품 포함
-- 설명: 모든 상품을 유지하고 판매 수량이 없으면 0으로 표시합니다.
SELECT
    p.product_id,
    p.product_name,
    c.category_name,
    COALESCE(SUM(s.quantity), 0) AS sold_quantity,
    COALESCE(ROUND(SUM(s.quantity * s.unit_price * (1 - s.discount_rate)), 2), 0)
        AS sales_amount
FROM products p
JOIN categories c ON c.category_id = p.category_id
LEFT JOIN sales s ON s.product_id = p.product_id
GROUP BY p.product_id, p.product_name, c.category_name
ORDER BY sold_quantity DESC, p.product_id;

-- [JOIN 5: FULL OUTER JOIN] 모든 주문과 모든 판매 상세
-- 설명: 판매 상세가 없는 취소 주문과, 대응 주문이 없는 데이터까지 양쪽 모두 보존합니다.
--       FK 때문에 현재는 '주문 없는 판매'가 없지만 FULL JOIN의 동작 학습에 적합합니다.
SELECT
    o.order_id,
    o.order_date,
    o.order_status,
    s.sale_id,
    s.product_id,
    s.quantity
FROM orders o
FULL OUTER JOIN sales s ON s.order_id = o.order_id
ORDER BY o.order_id NULLS LAST, s.sale_id;

-- =====================================================================
-- E. VIEW 2개
-- View는 SELECT 정의를 저장하며 원본 데이터 변경이 다음 조회에 즉시 반영됩니다.
-- =====================================================================

-- [VIEW 1] 주문 상세 View
DROP VIEW IF EXISTS v_order_sales_detail CASCADE;
CREATE VIEW v_order_sales_detail AS
SELECT
    o.order_id,
    o.order_date,
    o.order_status,
    c.customer_id,
    c.customer_name,
    c.region,
    p.product_id,
    p.product_name,
    cat.category_name,
    s.quantity,
    s.unit_price,
    s.discount_rate,
    ROUND(s.quantity * s.unit_price * (1 - s.discount_rate), 2) AS sale_amount
FROM sales s
JOIN orders o ON o.order_id = s.order_id
JOIN customers c ON c.customer_id = o.customer_id
JOIN products p ON p.product_id = s.product_id
JOIN categories cat ON cat.category_id = p.category_id;

SELECT * FROM v_order_sales_detail ORDER BY order_id, product_id;

-- [VIEW 2] 고객별 구매 요약 View
DROP VIEW IF EXISTS v_customer_purchase_summary CASCADE;
CREATE VIEW v_customer_purchase_summary AS
SELECT
    c.customer_id,
    c.customer_name,
    c.region,
    c.customer_grade,
    COUNT(DISTINCT o.order_id) AS order_count,
    MAX(o.order_date) AS last_order_date,
    COALESCE(
        ROUND(SUM(s.quantity * s.unit_price * (1 - s.discount_rate)), 2), 0
    ) AS total_purchase_amount
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.customer_id
LEFT JOIN sales s ON s.order_id = o.order_id
GROUP BY c.customer_id, c.customer_name, c.region, c.customer_grade;

SELECT * FROM v_customer_purchase_summary
ORDER BY total_purchase_amount DESC, customer_id;

-- =====================================================================
-- F. MATERIALIZED VIEW 2개
-- 결과를 실제 저장하므로 원본 변경 후 REFRESH가 필요합니다.
-- =====================================================================

-- [Materialized View 1] 월별 판매 요약
DROP MATERIALIZED VIEW IF EXISTS mv_monthly_sales_summary CASCADE;
CREATE MATERIALIZED VIEW mv_monthly_sales_summary AS
SELECT
    DATE_TRUNC('month', o.order_date)::date AS sales_month,
    COUNT(DISTINCT o.order_id) AS order_count,
    COUNT(DISTINCT o.customer_id) AS customer_count,
    SUM(s.quantity) AS sold_quantity,
    ROUND(SUM(s.quantity * s.unit_price * (1 - s.discount_rate)), 2) AS sales_amount
FROM orders o
JOIN sales s ON s.order_id = o.order_id
GROUP BY DATE_TRUNC('month', o.order_date)
WITH DATA;

CREATE UNIQUE INDEX ux_mv_monthly_sales_summary
    ON mv_monthly_sales_summary(sales_month);

SELECT * FROM mv_monthly_sales_summary ORDER BY sales_month;

-- 원본 변경 후 최신화
-- REFRESH MATERIALIZED VIEW mv_monthly_sales_summary;
-- UNIQUE INDEX가 있으므로 트랜잭션 밖에서 동시 새로고침도 가능합니다.
-- REFRESH MATERIALIZED VIEW CONCURRENTLY mv_monthly_sales_summary;

-- [Materialized View 2] 상품별 판매 요약
DROP MATERIALIZED VIEW IF EXISTS mv_product_sales_summary CASCADE;
CREATE MATERIALIZED VIEW mv_product_sales_summary AS
SELECT
    p.product_id,
    p.product_name,
    c.category_name,
    COALESCE(SUM(s.quantity), 0) AS sold_quantity,
    COALESCE(
        ROUND(SUM(s.quantity * s.unit_price * (1 - s.discount_rate)), 2), 0
    ) AS sales_amount
FROM products p
JOIN categories c ON c.category_id = p.category_id
LEFT JOIN sales s ON s.product_id = p.product_id
GROUP BY p.product_id, p.product_name, c.category_name
WITH DATA;

CREATE UNIQUE INDEX ux_mv_product_sales_summary
    ON mv_product_sales_summary(product_id);

SELECT * FROM mv_product_sales_summary
ORDER BY sales_amount DESC, product_id;

-- REFRESH MATERIALIZED VIEW mv_product_sales_summary;

-- =====================================================================
-- G. CUBE 1개
-- CUBE(A,B)는 (A,B), A 소계, B 소계, 전체 합계를 모두 생성합니다.
-- =====================================================================

-- [CUBE 1] 지역과 카테고리별 판매 분석
SELECT
    CASE WHEN GROUPING(c.region) = 1
         THEN '모든 지역' ELSE c.region END AS region,
    CASE WHEN GROUPING(cat.category_name) = 1
         THEN '모든 카테고리' ELSE cat.category_name END AS category_name,
    SUM(s.quantity) AS sold_quantity,
    ROUND(SUM(s.quantity * s.unit_price * (1 - s.discount_rate)), 2) AS sales_amount
FROM sales s
JOIN orders o ON o.order_id = s.order_id
JOIN customers c ON c.customer_id = o.customer_id
JOIN products p ON p.product_id = s.product_id
JOIN categories cat ON cat.category_id = p.category_id
GROUP BY CUBE(c.region, cat.category_name)
ORDER BY GROUPING(c.region), c.region,
         GROUPING(cat.category_name), cat.category_name;

-- =====================================================================
-- H. ROLLUP 1개
-- ROLLUP(A,B)는 (A,B), A 소계, 전체 합계를 계층적으로 생성합니다.
-- =====================================================================

-- [ROLLUP 1] 연도 → 월별 주문 및 판매 집계
SELECT
    CASE
        WHEN GROUPING(EXTRACT(YEAR FROM o.order_date)) = 1 THEN '전체 연도'
        ELSE EXTRACT(YEAR FROM o.order_date)::integer::text
    END AS sales_year,
    CASE
        WHEN GROUPING(EXTRACT(MONTH FROM o.order_date)) = 1 THEN '연도 소계'
        ELSE EXTRACT(MONTH FROM o.order_date)::integer::text || '월'
    END AS sales_month,
    COUNT(DISTINCT o.order_id) AS order_count,
    ROUND(SUM(s.quantity * s.unit_price * (1 - s.discount_rate)), 2) AS sales_amount
FROM orders o
JOIN sales s ON s.order_id = o.order_id
GROUP BY ROLLUP(
    EXTRACT(YEAR FROM o.order_date),
    EXTRACT(MONTH FROM o.order_date)
)
ORDER BY EXTRACT(YEAR FROM o.order_date) NULLS LAST,
         EXTRACT(MONTH FROM o.order_date) NULLS LAST;

-- =====================================================================
-- 생성 객체 확인
-- =====================================================================
SELECT table_schema, table_name
FROM information_schema.views
WHERE table_schema = 'sales_lab'
ORDER BY table_name;

SELECT schemaname, matviewname, ispopulated
FROM pg_matviews
WHERE schemaname = 'sales_lab'
ORDER BY matviewname;



-- =====================================================================
-- ROW_NUMBER, RANK, DENSE_RANK, NTILE처럼 OVER()와 함께 사용하는 함수를 Window Function(윈도우 함수) 또는 분석 함수라고 합니다.
-- =====================================================================
--윈도우 함수는 GROUP BY와 달리 원래 행을 유지하면서 순위·누계·평균·이전 값 등을 계산합니다.
--`sales.unit_price`에는 같은 가격이 여러 건 있어서 순위 함수의 차이를 확인하기 좋습니다.
--
--```sql
SET search_path TO sales_lab, public;

SELECT
    s.sale_id,
    s.order_id,
    p.product_name,
    s.unit_price,
    s.quantity,
    -- 같은 가격이어도 고유한 순번 부여
    ROW_NUMBER() OVER (
        ORDER BY s.unit_price DESC, s.sale_id
    ) AS row_number_rank,
    -- 같은 가격은 같은 순위, 다음 순위는 건너뜀
    RANK() OVER (
        ORDER BY s.unit_price DESC
    ) AS rank_rank,
    -- 같은 가격은 같은 순위, 다음 순위를 건너뛰지 않음
    DENSE_RANK() OVER (
        ORDER BY s.unit_price DESC
    ) AS dense_rank_rank,
    -- 가격 내림차순으로 전체 데이터를 4개 그룹으로 분할
    NTILE(4) OVER (
        ORDER BY s.unit_price DESC
    ) AS price_quartile
FROM sales s
JOIN products p
    ON p.product_id = s.product_id
ORDER BY
    s.unit_price DESC,
    s.sale_id;
---
--
--## 결과에서 확인할 부분
--
--예를 들어 `unit_price = 2,290,000`인 판매가 2건이라면 다음과 같이 나타납니다.
--
--| 상품          |        단가 | ROW_NUMBER | RANK | DENSE_RANK |
--| ----------- | --------: | ---------: | ---: | ---------: |
--| 게이밍 노트북 16  | 2,290,000 |          1 |    1 |          1 |
--| 게이밍 노트북 16  | 2,290,000 |          2 |    1 |          1 |
--| 비즈니스 노트북 15 | 1,780,000 |          3 |    3 |          2 |
--| 울트라북 14     | 1,450,000 |          4 |    4 |          3 |
--| 울트라북 14     | 1,450,000 |          5 |    4 |          3 |
--| 스마트폰 Pro    | 1,390,000 |          6 |    6 |          4 |
--
--차이는 다음과 같습니다.
--
--* `ROW_NUMBER`: 같은 단가라도 `1, 2`처럼 서로 다른 번호
--* `RANK`: 공동 1위 다음은 2위를 건너뛰고 3위
--* `DENSE_RANK`: 공동 1위 다음은 바로 2위
--* `NTILE(4)`: 순위가 아니라 전체 행을 약 4등분한 그룹 번호
--
--## NTILE을 등급으로 표시하기

--```sql
WITH price_ranking AS (
    SELECT
        s.sale_id,
        s.order_id,
        p.product_name,
        s.unit_price,
        s.quantity,
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
        ) AS price_quartile
    FROM sales s
    JOIN products p
        ON p.product_id = s.product_id
)
SELECT
    sale_id,
    order_id,
    product_name,
    unit_price,
    quantity,
    row_number_rank,
    rank_rank,
    dense_rank_rank,
    price_quartile,
    CASE price_quartile
        WHEN 1 THEN '고가 그룹'
        WHEN 2 THEN '중고가 그룹'
        WHEN 3 THEN '중저가 그룹'
        WHEN 4 THEN '저가 그룹'
    END AS price_group
FROM price_ranking
ORDER BY unit_price DESC, sale_id;
--```

--주의할 점은 `NTILE(4)`은 같은 가격이라도 데이터 경계에 걸리면 서로 다른 그룹으로 나뉠 수 있다는 것입니다. 
--`NTILE`은 같은 값에 동일 순위를 주는 함수가 아니라, **전체 행 개수를 비슷하게 4개로 나누는 함수**이기 때문입니다.
