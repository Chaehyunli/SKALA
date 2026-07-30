/*
===============================================================================
 PostgreSQL FUNCTION 완전 실습
===============================================================================
 대상 버전 : PostgreSQL 14 이상
 실습 도구 : DBeaver, pgAdmin, psql
 선행 파일 : PostgreSQL_day04_프로시저_함수_샘플_스키마_DDL_DML.sql
 사용 스키마: proc_lab

 학습 목표
   1. SQL 함수와 PL/pgSQL 함수의 문법을 구분한다.
   2. 스칼라, 행, 여러 행, JSONB 반환 함수를 작성한다.
   3. IMMUTABLE / STABLE / VOLATILE의 의미를 이해한다.
   4. STRICT, 기본값, 변수, 조건문, 예외, 출력문을 사용한다.
   5. 함수에서 안전한 동적 SQL과 SECURITY DEFINER를 구성한다.
   6. 행마다 함수 반복 호출과 집합 기반 쿼리의 차이를 이해한다.

 중요한 사실
   - 함수는 주로 SELECT 문 안에서 호출하고 반드시 결과를 반환한다.
   - PostgreSQL 함수는 권한이 있으면 INSERT/UPDATE/DELETE도 수행할 수 있다.
   - DML을 수행한다고 IMMUTABLE이 되는 것이 아니다.
     DML 함수는 일반적으로 VOLATILE이어야 한다.
   - 함수에서는 COMMIT/ROLLBACK으로 호출자의 트랜잭션을 끝낼 수 없다.

 실행 안내
   - 위에서 아래로 실행할 수 있다.
   - 오류 확인용 문장은 기본 실행을 방해하지 않도록 주석 처리했다.
   - 데이터를 바꾸는 테스트는 BEGIN ... ROLLBACK으로 원상 복구한다.
===============================================================================
*/


-- ============================================================================
-- 00. 실습 환경 확인
-- ============================================================================

SET search_path TO proc_lab, public;

SELECT current_database() AS database_name,
       current_schema()   AS current_schema,
       current_user       AS current_user,
       version()          AS postgresql_version;

/*
  샘플 스키마가 설치되지 않았으면 즉시 이해하기 쉬운 오류를 발생시킨다.
  to_regclass()는 객체가 없을 때 오류 대신 NULL을 반환한다.
*/
DO $block$
BEGIN
    IF to_regclass('proc_lab.customers') IS NULL
       OR to_regclass('proc_lab.products') IS NULL
       OR to_regclass('proc_lab.orders') IS NULL
       OR to_regclass('proc_lab.order_items') IS NULL THEN
        RAISE EXCEPTION
            'proc_lab 샘플 테이블이 없습니다. 선행 DDL/DML 파일을 먼저 실행하세요.';
    END IF;
END;
$block$;


-- ============================================================================
-- 01. 함수 기본 구조와 호출 방법
-- ============================================================================

/*
  기본 구조

  CREATE OR REPLACE FUNCTION 스키마.함수명(매개변수명 자료형)
  RETURNS 반환자료형
  LANGUAGE sql 또는 plpgsql
  [IMMUTABLE | STABLE | VOLATILE]
  [STRICT]
  AS $function$
      함수 본문
  $function$;

  대표 호출 방법
    SELECT proc_lab.함수명(값);
    SELECT * FROM proc_lab.여러행반환함수(값);
    SELECT proc_lab.함수명(매개변수명 => 값);  -- 이름 기반 호출

  CREATE OR REPLACE로 바꿀 수 없는 항목
    - 함수 이름
    - 입력 매개변수의 자료형
    - 반환 자료형

  위 항목을 바꾸려면 기존 함수를 DROP한 뒤 다시 만들어야 한다.
*/


-- ============================================================================
-- 02. [기초] LANGUAGE SQL — 한 개의 계산값 반환
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_tax_amount(
    p_amount   numeric,
    p_tax_rate numeric DEFAULT 0.10
)
RETURNS numeric
LANGUAGE sql
IMMUTABLE
STRICT
PARALLEL SAFE
AS $function$
    SELECT round(p_amount * p_tax_rate, 2);
$function$;

COMMENT ON FUNCTION proc_lab.fn_tax_amount(numeric, numeric)
IS '금액과 세율을 입력받아 세액을 반환하는 순수 계산 함수';

/*
  IMMUTABLE
    같은 입력이면 데이터베이스 상태와 관계없이 항상 같은 결과를 반환한다.
    테이블 조회, 현재 시각, random() 등에 의존하면 안 된다.

  STRICT
    입력값 중 하나라도 NULL이면 함수 본문을 실행하지 않고 NULL을 반환한다.

  PARALLEL SAFE
    병렬 쿼리 작업자에서 실행해도 안전하다고 옵티마이저에 알린다.
*/

SELECT proc_lab.fn_tax_amount(100000)       AS tax_default_10_percent;
SELECT proc_lab.fn_tax_amount(100000, 0.08) AS tax_8_percent;
SELECT proc_lab.fn_tax_amount(
           p_tax_rate => 0.05,
           p_amount   => 200000
       ) AS named_arguments;
SELECT proc_lab.fn_tax_amount(NULL, 0.10) AS strict_returns_null;


-- ============================================================================
-- 03. [기초] LANGUAGE plpgsql — 변수, IF, 예외, RETURN
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_line_total(
    p_unit_price       numeric,
    p_quantity         integer,
    p_discount_percent numeric DEFAULT 0
)
RETURNS numeric
LANGUAGE plpgsql
IMMUTABLE
STRICT
PARALLEL SAFE
AS $function$
DECLARE
    v_subtotal numeric;
    v_result   numeric;
BEGIN
    IF p_unit_price < 0 THEN
        RAISE EXCEPTION '단가는 0 이상이어야 합니다: %', p_unit_price
            USING ERRCODE = '22003';
    END IF;

    IF p_quantity <= 0 THEN
        RAISE EXCEPTION '수량은 1 이상이어야 합니다: %', p_quantity
            USING ERRCODE = '22003';
    END IF;

    IF p_discount_percent NOT BETWEEN 0 AND 100 THEN
        RAISE EXCEPTION '할인율은 0~100 범위여야 합니다: %',
                        p_discount_percent
            USING ERRCODE = '22003';
    END IF;

    v_subtotal := p_unit_price * p_quantity;
    v_result   := v_subtotal * (1 - p_discount_percent / 100.0);

    RETURN round(v_result, 2);
END;
$function$;

/*
  연산 우선순위
    50000 * 2 * (1 - 0 / 100.0)
    = 100000 * (1 - 0)
    = 100000

  괄호 안에서도 나눗셈이 뺄셈보다 먼저 계산된다.
*/
SELECT proc_lab.fn_line_total(50000, 2)     AS no_discount;
SELECT proc_lab.fn_line_total(50000, 2, 10) AS discount_10_percent;

-- 오류 실습: 한 문장씩 주석을 해제하여 SQLSTATE와 메시지를 확인한다.
-- SELECT proc_lab.fn_line_total(-1000, 2, 0);
-- SELECT proc_lab.fn_line_total(1000, 0, 0);
-- SELECT proc_lab.fn_line_total(1000, 2, 120);


-- ============================================================================
-- 04. [기초] 함수 내부 변수값 출력 — RAISE NOTICE
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_line_total_debug(
    p_unit_price       numeric,
    p_quantity         integer,
    p_discount_percent numeric DEFAULT 0
)
RETURNS numeric
LANGUAGE plpgsql
IMMUTABLE
STRICT
AS $function$
DECLARE
    v_subtotal numeric;
    v_result   numeric;
BEGIN
    v_subtotal := p_unit_price * p_quantity;
    v_result   := v_subtotal * (1 - p_discount_percent / 100.0);

    RAISE NOTICE
        'unit_price=%, quantity=%, subtotal=%, discount=%퍼센트, result=%',
        p_unit_price,
        p_quantity,
        v_subtotal,
        p_discount_percent,
        v_result;

    RETURN round(v_result, 2);
END;
$function$;

/*
  RAISE의 %는 값 한 개를 출력하는 자리 표시자이다.

  숫자 뒤에 실제 % 기호를 출력하는 방법
    RAISE NOTICE '할인율=%퍼센트', p_discount_percent;

  또는
    RAISE NOTICE '%',
        format('할인율=%s%%', p_discount_percent);

  '할인율=%%'만 쓰면 %%는 "문자 %"일 뿐 값을 소비하지 않는다.
  이때 매개변수를 하나 더 전달하면 "too many parameters" 오류가 발생한다.
*/
SELECT proc_lab.fn_line_total_debug(50000, 2, 10) AS debug_result;


-- ============================================================================
-- 05. [중급] 테이블을 조회하여 단일 값 반환
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_customer_order_total(
    p_customer_id bigint
)
RETURNS numeric
LANGUAGE sql
STABLE
STRICT
AS $function$
    SELECT coalesce(sum(o.total_amount), 0)::numeric(14,2)
    FROM proc_lab.orders AS o
    WHERE o.customer_id = p_customer_id
      AND o.order_status <> 'CANCELLED';
$function$;

/*
  STABLE
    한 SQL 문을 실행하는 동안 같은 입력에 대해 결과가 안정적이라는 약속이다.
    테이블을 조회하는 함수에 주로 사용한다.
    다른 트랜잭션이 데이터를 바꾸면 다음 SQL 문에서는 결과가 달라질 수 있다.
*/
SELECT c.customer_id,
       c.customer_name,
       proc_lab.fn_customer_order_total(c.customer_id)
           AS cumulative_order_amount
FROM proc_lab.customers AS c
ORDER BY c.customer_id;


-- ============================================================================
-- 06. [중급] 여러 행 반환 — RETURNS TABLE + RETURN QUERY
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_orders_by_status(
    p_status text DEFAULT NULL
)
RETURNS TABLE (
    order_id      bigint,
    customer_name varchar,
    order_status  varchar,
    total_amount  numeric,
    ordered_at    timestamptz
)
LANGUAGE plpgsql
STABLE
AS $function$
BEGIN
    IF p_status IS NOT NULL
       AND p_status NOT IN
           ('PENDING', 'PAID', 'SHIPPING', 'COMPLETED', 'CANCELLED') THEN
        RAISE EXCEPTION '허용되지 않은 주문 상태입니다: %', p_status
            USING ERRCODE = '22023';
    END IF;

    RETURN QUERY
    SELECT o.order_id,
           c.customer_name,
           o.order_status,
           o.total_amount,
           o.ordered_at
    FROM proc_lab.orders AS o
    JOIN proc_lab.customers AS c
      ON c.customer_id = o.customer_id
    WHERE p_status IS NULL
       OR o.order_status = p_status
    ORDER BY o.ordered_at DESC;
END;
$function$;

-- 모든 주문
SELECT * FROM proc_lab.fn_orders_by_status();

-- PENDING 주문만 조회하고 반환 결과에 추가 조건 적용
SELECT *
FROM proc_lab.fn_orders_by_status('PENDING')
WHERE total_amount >= 50000
ORDER BY total_amount DESC;


-- ============================================================================
-- 07. [중급] %TYPE, %ROWTYPE, SELECT INTO STRICT
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_find_order(
    p_order_id bigint
)
RETURNS proc_lab.orders
LANGUAGE plpgsql
STABLE
STRICT
AS $function$
DECLARE
    /*
      %ROWTYPE은 테이블 한 행 전체의 구조를 그대로 사용한다.
      테이블 컬럼이 바뀌었을 때 별도의 복합 자료형 선언을 줄일 수 있다.
    */
    v_order proc_lab.orders%ROWTYPE;
BEGIN
    /*
      INTO STRICT
        0행  : NO_DATA_FOUND
        2행 이상: TOO_MANY_ROWS

      order_id는 PK이므로 실제로는 0행 또는 1행만 나온다.
    */
    SELECT o.*
      INTO STRICT v_order
      FROM proc_lab.orders AS o
     WHERE o.order_id = p_order_id;

    RETURN v_order;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE EXCEPTION '주문번호 %를 찾을 수 없습니다.', p_order_id
            USING ERRCODE = 'P0002',
                  HINT = 'proc_lab.orders의 order_id를 확인하세요.';
END;
$function$;

SELECT (proc_lab.fn_find_order(1)).*;

-- 오류 실습
-- SELECT (proc_lab.fn_find_order(999999)).*;


-- ============================================================================
-- 08. [중급] OUT 매개변수로 여러 값을 반환
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_product_stock_info(
    IN  p_product_id   bigint,
    OUT product_name   varchar,
    OUT stock_qty      integer,
    OUT stock_state    text
)
RETURNS record
LANGUAGE plpgsql
STABLE
STRICT
AS $function$
BEGIN
    SELECT p.product_name,
           p.stock_qty,
           CASE
               WHEN p.stock_qty = 0  THEN 'OUT_OF_STOCK'
               WHEN p.stock_qty < 50 THEN 'LOW'
               ELSE 'NORMAL'
           END
      INTO STRICT product_name, stock_qty, stock_state
      FROM proc_lab.products AS p
     WHERE p.product_id = p_product_id;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE EXCEPTION '상품번호 %를 찾을 수 없습니다.', p_product_id
            USING ERRCODE = 'P0002';
END;
$function$;

SELECT *
FROM proc_lab.fn_product_stock_info(3);


-- ============================================================================
-- 09. [중급] 함수도 DML 가능 — 원자적 재고 차감
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_reserve_stock(
    p_product_id bigint,
    p_quantity   integer
)
RETURNS integer
LANGUAGE plpgsql
VOLATILE
STRICT
PARALLEL UNSAFE
AS $function$
DECLARE
    v_remaining integer;
BEGIN
    IF p_quantity <= 0 THEN
        RAISE EXCEPTION '차감 수량은 1 이상이어야 합니다.'
            USING ERRCODE = '22003';
    END IF;

    /*
      조회 후 UPDATE를 따로 하면 동시 요청 사이에서 재고가 달라질 수 있다.
      조건부 UPDATE 한 문장으로 확인과 차감을 함께 수행하면 원자적이다.
    */
    UPDATE proc_lab.products
       SET stock_qty = stock_qty - p_quantity
     WHERE product_id = p_product_id
       AND active
       AND stock_qty >= p_quantity
    RETURNING stock_qty INTO v_remaining;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            '상품 %의 재고가 부족하거나 비활성 상태입니다.', p_product_id
            USING ERRCODE = 'P0001';
    END IF;

    RETURN v_remaining;
END;
$function$;

/*
  VOLATILE
    호출할 때마다 데이터베이스 상태가 달라질 수 있음을 뜻한다.
    INSERT/UPDATE/DELETE, random(), clock_timestamp() 사용 함수에 적합하다.

  IMMUTABLE은 "DB를 변경한다"는 의미가 아니다.
  오히려 같은 입력이면 언제나 같은 결과라는 매우 강한 약속이다.
*/

-- 테스트 후 데이터 복원
BEGIN;

SELECT product_id, product_name, stock_qty
FROM proc_lab.products
WHERE product_id = 1;

SELECT proc_lab.fn_reserve_stock(1, 2) AS remaining_stock;

SELECT product_id, product_name, stock_qty
FROM proc_lab.products
WHERE product_id = 1;

ROLLBACK;


-- ============================================================================
-- 10. [고급] JSONB 영수증 한 건 반환
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_order_receipt_json(
    p_order_id bigint
)
RETURNS jsonb
LANGUAGE plpgsql
STABLE
STRICT
AS $function$
DECLARE
    v_result jsonb;
BEGIN
    SELECT jsonb_build_object(
        'order_id',     o.order_id,
        'status',       o.order_status,
        'ordered_at',   o.ordered_at,
        'customer',     jsonb_build_object(
                            'customer_id', c.customer_id,
                            'name',        c.customer_name,
                            'email',       c.email
                        ),
        'total_amount', o.total_amount,
        'items',        coalesce(
            (
                SELECT jsonb_agg(
                    jsonb_build_object(
                        'product_id',   p.product_id,
                        'product_name', p.product_name,
                        'quantity',     oi.quantity,
                        'unit_price',   oi.unit_price,
                        'line_amount',  oi.line_amount
                    )
                    ORDER BY oi.order_item_id
                )
                FROM proc_lab.order_items AS oi
                JOIN proc_lab.products AS p
                  ON p.product_id = oi.product_id
                WHERE oi.order_id = o.order_id
            ),
            '[]'::jsonb
        )
    )
      INTO v_result
      FROM proc_lab.orders AS o
      JOIN proc_lab.customers AS c
        ON c.customer_id = o.customer_id
     WHERE o.order_id = p_order_id;

    IF v_result IS NULL THEN
        RAISE EXCEPTION '주문번호 %를 찾을 수 없습니다.', p_order_id
            USING ERRCODE = 'P0002';
    END IF;

    RETURN v_result;
END;
$function$;

SELECT jsonb_pretty(proc_lab.fn_order_receipt_json(1));


-- ============================================================================
-- 11. [고급] 기간 내 여러 영수증을 집합 기반으로 한 번에 반환
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_order_receipts_by_period(
    p_from timestamptz,
    p_to   timestamptz
)
RETURNS SETOF jsonb
LANGUAGE sql
STABLE
STRICT
AS $function$
    /*
      item_json CTE에서 주문별 품목 배열을 한 번에 집계한 뒤
      orders/customers와 결합한다.

      여러 주문에 대해 fn_order_receipt_json(order_id)를 반복 호출하면
      주문 수만큼 함수 내부 조회가 실행된다.
      이 함수는 대상 주문과 품목을 집합 단위로 처리한다.
    */
    WITH item_json AS (
        SELECT oi.order_id,
               jsonb_agg(
                   jsonb_build_object(
                       'product_id',   p.product_id,
                       'product_name', p.product_name,
                       'quantity',     oi.quantity,
                       'unit_price',   oi.unit_price,
                       'line_amount',  oi.line_amount
                   )
                   ORDER BY oi.order_item_id
               ) AS items
        FROM proc_lab.order_items AS oi
        JOIN proc_lab.products AS p
          ON p.product_id = oi.product_id
        GROUP BY oi.order_id
    )
    SELECT jsonb_build_object(
               'order_id',     o.order_id,
               'status',       o.order_status,
               'ordered_at',   o.ordered_at,
               'customer',     jsonb_build_object(
                                   'customer_id', c.customer_id,
                                   'name',        c.customer_name,
                                   'email',       c.email
                               ),
               'total_amount', o.total_amount,
               'items',        coalesce(i.items, '[]'::jsonb)
           )
    FROM proc_lab.orders AS o
    JOIN proc_lab.customers AS c
      ON c.customer_id = o.customer_id
    LEFT JOIN item_json AS i
      ON i.order_id = o.order_id
    WHERE o.ordered_at >= p_from
      AND o.ordered_at <  p_to
    ORDER BY o.order_id;
$function$;

-- 샘플 데이터가 생성된 현재 월의 영수증을 한 번에 조회한다.
SELECT jsonb_pretty(r.receipt)
FROM proc_lab.fn_order_receipts_by_period(
         date_trunc('month', current_timestamp),
         date_trunc('month', current_timestamp) + interval '1 month'
     ) AS r(receipt);

/*
  2026년 7월처럼 고정된 기간을 조회하려면 종료일을 미만(<) 조건으로 전달한다.
  이렇게 하면 7월 31일 23:59:59.999...를 직접 계산할 필요가 없다.

  SELECT jsonb_pretty(r.receipt)
  FROM proc_lab.fn_order_receipts_by_period(
           timestamptz '2026-07-01 00:00:00+09',
           timestamptz '2026-08-01 00:00:00+09'
       ) AS r(receipt);
*/


-- ============================================================================
-- 12. [고급] 안전한 동적 SQL — regclass + format() + USING
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_count_rows_dynamic(
    p_table  regclass,
    p_column name,
    p_value  text
)
RETURNS bigint
LANGUAGE plpgsql
STABLE
STRICT
AS $function$
DECLARE
    v_sql   text;
    v_count bigint;
BEGIN
    /*
      p_table은 regclass이므로 실제 존재하는 관계만 받을 수 있다.
      컬럼명은 pg_attribute에서 존재 여부를 먼저 확인한다.
    */
    IF NOT EXISTS (
        SELECT 1
        FROM pg_catalog.pg_attribute AS a
        WHERE a.attrelid = p_table
          AND a.attname = p_column
          AND a.attnum > 0
          AND NOT a.attisdropped
    ) THEN
        RAISE EXCEPTION '테이블 %에 컬럼 %가 없습니다.', p_table, p_column
            USING ERRCODE = '42703';
    END IF;

    /*
      식별자
        - 테이블: 검증된 regclass를 %s로 출력
        - 컬럼  : format()의 %I로 안전하게 인용

      값
        - 문자열에 직접 연결하지 않고 $1 자리와 USING으로 전달
        - SQL Injection처럼 보이는 문자열도 하나의 값으로 취급

      교육용 범용 예제라 컬럼을 text로 변환한다.
      운영 대용량 조회에서는 컬럼 타입별 정적 SQL이 인덱스에 더 유리하다.
    */
    v_sql := format(
        'SELECT count(*) FROM %s WHERE %I::text = $1',
        p_table,
        p_column
    );

    RAISE NOTICE '실행 SQL: % / 바인딩 값: %', v_sql, p_value;

    EXECUTE v_sql INTO v_count USING p_value;
    RETURN v_count;
END;
$function$;

SELECT proc_lab.fn_count_rows_dynamic(
           'proc_lab.orders'::regclass,
           'order_status',
           'PENDING'
       ) AS pending_count;

SELECT proc_lab.fn_count_rows_dynamic(
           'proc_lab.orders'::regclass,
           'order_status',
           $$PENDING' OR '1'='1$$
       ) AS injection_attempt_count;

-- 존재하지 않는 컬럼 오류 실습
-- SELECT proc_lab.fn_count_rows_dynamic(
--            'proc_lab.orders'::regclass, 'no_column', 'x'
--        );


-- ============================================================================
-- 13. [고급] 변동성·병렬 안전성 비교
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_grade_discount_rate(
    p_grade text
)
RETURNS numeric
LANGUAGE sql
IMMUTABLE
STRICT
PARALLEL SAFE
AS $function$
    SELECT CASE upper(p_grade)
               WHEN 'VIP'    THEN 0.15
               WHEN 'GOLD'   THEN 0.10
               WHEN 'SILVER' THEN 0.05
               ELSE 0
           END;
$function$;

SELECT customer_id,
       customer_name,
       customer_grade,
       proc_lab.fn_grade_discount_rate(customer_grade) AS discount_rate
FROM proc_lab.customers
ORDER BY customer_id;

/*
  변동성 요약

  IMMUTABLE
    - 동일 입력은 영원히 동일 결과
    - 순수 계산, 문자열 변환 등에 적합
    - 테이블 조회와 현재시간 의존 금지

  STABLE
    - 한 SQL 문 안에서 동일 입력은 동일 결과로 간주 가능
    - 테이블 조회 함수에 적합

  VOLATILE
    - 호출마다 결과나 DB 상태가 달라질 수 있음
    - DML, random(), nextval(), clock_timestamp() 등에 적합
    - 생략 시 기본값

  속성을 실제 동작보다 강하게 선언하면 옵티마이저가 잘못된 가정을 한다.
  EXPLAIN에서 차이가 항상 눈에 보이는 것은 아니지만 의미는 바뀌지 않는다.
*/


-- ============================================================================
-- 14. [고급] SECURITY DEFINER를 안전하게 구성
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_customer_is_active(
    p_customer_id bigint
)
RETURNS boolean
LANGUAGE sql
STABLE
STRICT
SECURITY DEFINER
SET search_path = pg_catalog, proc_lab, pg_temp
AS $function$
    SELECT EXISTS (
        SELECT 1
        FROM proc_lab.customers AS c
        WHERE c.customer_id = p_customer_id
          AND c.active
    );
$function$;

/*
  SECURITY INVOKER  : 호출자 권한으로 실행. 기본값.
  SECURITY DEFINER  : 함수 소유자 권한으로 실행.

  SECURITY DEFINER에서 search_path를 고정하지 않으면
  공격자가 쓰기 가능한 스키마에 같은 이름의 함수·테이블을 만들어
  소유자 권한으로 잘못된 객체를 실행하게 할 수 있다.

  안전 패턴
    1. search_path를 pg_catalog, 신뢰 스키마, pg_temp 순으로 고정
    2. 본문에서 테이블을 스키마까지 완전 수식
    3. PUBLIC의 기본 EXECUTE 권한 제거
    4. 필요한 역할에만 EXECUTE 부여
*/
REVOKE ALL
ON FUNCTION proc_lab.fn_customer_is_active(bigint)
FROM PUBLIC;

-- 함수 소유자는 PUBLIC 권한을 제거한 뒤에도 실행할 수 있다.
SELECT proc_lab.fn_customer_is_active(1) AS customer_1_active;

-- 운영 역할이 존재할 때만 역할명을 바꿔 실행한다.
-- GRANT EXECUTE
-- ON FUNCTION proc_lab.fn_customer_is_active(bigint)
-- TO app_user;


-- ============================================================================
-- 15. [성능] 행별 함수 호출과 집합 기반 쿼리
-- ============================================================================

/*
  아래 방식은 읽기 쉽지만 customers의 각 행마다 함수가 호출될 수 있다.

  EXPLAIN (ANALYZE, BUFFERS)
  SELECT c.customer_id,
         proc_lab.fn_customer_order_total(c.customer_id)
  FROM proc_lab.customers AS c;

  데이터가 작으면 문제가 없지만 고객 수가 많으면 함수 내부 쿼리가 반복된다.
  대량 조회는 다음처럼 먼저 집계하고 한 번에 조인하는 방식이 유리할 수 있다.
*/
WITH order_total AS (
    SELECT o.customer_id,
           sum(o.total_amount)::numeric(14,2) AS total_amount
    FROM proc_lab.orders AS o
    WHERE o.order_status <> 'CANCELLED'
    GROUP BY o.customer_id
)
SELECT c.customer_id,
       c.customer_name,
       coalesce(t.total_amount, 0)::numeric(14,2)
           AS cumulative_order_amount
FROM proc_lab.customers AS c
LEFT JOIN order_total AS t
  ON t.customer_id = c.customer_id
ORDER BY c.customer_id;


-- ============================================================================
-- 16. 함수 메타데이터 조회
-- ============================================================================

SELECT n.nspname AS function_schema,
       p.proname AS function_name,
       pg_catalog.pg_get_function_identity_arguments(p.oid)
           AS identity_arguments,
       pg_catalog.pg_get_function_result(p.oid) AS result_type,
       l.lanname AS language_name,
       CASE p.provolatile
           WHEN 'i' THEN 'IMMUTABLE'
           WHEN 's' THEN 'STABLE'
           WHEN 'v' THEN 'VOLATILE'
       END AS volatility,
       CASE p.proparallel
           WHEN 's' THEN 'SAFE'
           WHEN 'r' THEN 'RESTRICTED'
           WHEN 'u' THEN 'UNSAFE'
       END AS parallel_safety,
       p.proisstrict AS is_strict,
       p.prosecdef   AS security_definer,
       pg_catalog.pg_get_userbyid(p.proowner) AS owner_name
FROM pg_catalog.pg_proc AS p
JOIN pg_catalog.pg_namespace AS n
  ON n.oid = p.pronamespace
JOIN pg_catalog.pg_language AS l
  ON l.oid = p.prolang
WHERE n.nspname = 'proc_lab'
  AND p.prokind = 'f'
ORDER BY p.proname,
         pg_catalog.pg_get_function_identity_arguments(p.oid);

-- 함수 정의문 확인
SELECT pg_catalog.pg_get_functiondef(
           'proc_lab.fn_line_total(numeric,integer,numeric)'::regprocedure
       );


-- ============================================================================
-- 17. 함수 관리: 권한, 설명, 삭제
-- ============================================================================

/*
  권한
    GRANT EXECUTE ON FUNCTION 함수명(입력자료형...) TO 역할명;
    REVOKE EXECUTE ON FUNCTION 함수명(입력자료형...) FROM 역할명;

  설명
    COMMENT ON FUNCTION 함수명(입력자료형...) IS '설명';

  삭제
    DROP FUNCTION 함수명(입력자료형...);

  오버로딩된 함수가 있을 수 있으므로 DROP/GRANT/COMMENT 때는
  괄호 안의 입력 자료형까지 지정하는 습관이 중요하다.
*/

-- 필요할 때만 주석을 해제한다.
-- DROP FUNCTION proc_lab.fn_line_total(numeric, integer, numeric);


-- ============================================================================
-- 18. 최종 확인 및 복습
-- ============================================================================

SELECT 'fn_tax_amount' AS example,
       proc_lab.fn_tax_amount(100000, 0.1)::text AS result
UNION ALL
SELECT 'fn_line_total',
       proc_lab.fn_line_total(50000, 2, 10)::text
UNION ALL
SELECT 'fn_customer_order_total',
       proc_lab.fn_customer_order_total(1)::text
UNION ALL
SELECT 'fn_grade_discount_rate',
       proc_lab.fn_grade_discount_rate('GOLD')::text;

/*
  복습 질문
    1. IMMUTABLE 함수에서 orders 테이블을 조회하면 왜 잘못된 설계인가?
    2. STRICT 함수에 NULL이 전달되면 본문은 실행되는가?
    3. RETURNS TABLE 함수는 SELECT에서 어떻게 호출하는가?
    4. DML 함수가 VOLATILE이어야 하는 이유는 무엇인가?
    5. 여러 주문 영수증은 한 건 함수 반복 호출보다 왜 집합 기반이 유리한가?
    6. 동적 SQL에서 값은 왜 문자열 연결 대신 USING으로 전달해야 하는가?
    7. SECURITY DEFINER에서 search_path를 왜 고정해야 하는가?
*/

