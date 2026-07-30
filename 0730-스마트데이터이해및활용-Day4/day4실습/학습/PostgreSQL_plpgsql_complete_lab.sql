/*
===============================================================================
 PostgreSQL PL/pgSQL 완전 실습
===============================================================================
 대상 버전 : PostgreSQL 14 이상
 실습 도구 : DBeaver, pgAdmin, psql
 선행 파일 : PostgreSQL_day04_프로시저_함수_샘플_스키마_DDL_DML.sql
 사용 스키마: proc_lab

 PL/pgSQL이란?
   - Procedural Language/PostgreSQL의 약자이다.
   - PostgreSQL에 SQL의 데이터 처리 능력과 변수, 조건문, 반복문,
     예외 처리 같은 절차형 프로그래밍 기능을 추가한 언어이다.
   - 독립적으로 호출하는 객체 이름이 아니라 함수, 프로시저,
     트리거 함수, 익명 DO 블록의 본문을 작성하는 언어이다.

 학습 목표
   1. PL/pgSQL 블록의 DECLARE / BEGIN / EXCEPTION / END 구조를 이해한다.
   2. 변수, 상수, %TYPE, %ROWTYPE, record를 사용한다.
   3. IF, CASE, LOOP, WHILE, FOR, FOREACH를 사용한다.
   4. SELECT INTO, PERFORM, FOUND, ROW_COUNT를 구분한다.
   5. RETURN, RETURN NEXT, RETURN QUERY를 사용한다.
   6. RAISE와 GET STACKED DIAGNOSTICS로 오류를 처리한다.
   7. EXECUTE, format(), USING으로 안전한 동적 SQL을 작성한다.
   8. 커서, 트랜잭션, 트리거 함수, 보안, 성능 주의점을 이해한다.

 실행 안내
   - 위에서 아래로 순서대로 실행할 수 있다.
   - 오류 확인용 문장은 기본 실행을 방해하지 않도록 주석 처리했다.
   - 데이터를 변경하는 실습은 BEGIN ... ROLLBACK으로 원상 복구한다.
   - 이 파일은 FUNCTION/PROCEDURE 완전 실습 파일과 독립적으로 실행된다.
===============================================================================
*/


-- ============================================================================
-- 00. 실습 환경과 PL/pgSQL 설치 확인
-- ============================================================================

SET search_path TO proc_lab, public;

SELECT current_database() AS database_name,
       current_schema()   AS current_schema,
       current_user       AS current_user,
       version()          AS postgresql_version;

/*
  PL/pgSQL은 PostgreSQL에 기본 설치되는 신뢰 언어이다.
  pg_language에서 사용 가능 여부를 확인할 수 있다.
*/
SELECT lanname       AS language_name,
       lanpltrusted  AS trusted_language,
       laninline <> 0::oid AS supports_do_block
FROM pg_catalog.pg_language
WHERE lanname = 'plpgsql';

/*
  선행 샘플 스키마가 없으면 이해하기 쉬운 오류를 발생시킨다.
*/
DO $block$
BEGIN
    IF to_regclass('proc_lab.customers') IS NULL
       OR to_regclass('proc_lab.products') IS NULL
       OR to_regclass('proc_lab.orders') IS NULL
       OR to_regclass('proc_lab.order_items') IS NULL
       OR to_regclass('proc_lab.routine_error_log') IS NULL THEN
        RAISE EXCEPTION
            'proc_lab 샘플 테이블이 없습니다. 선행 DDL/DML 파일을 먼저 실행하세요.';
    END IF;
END;
$block$;


-- ============================================================================
-- 01. PL/pgSQL 블록의 기본 구조
-- ============================================================================

/*
  완전한 블록 구조

  [<<블록라벨>>]
  [DECLARE
      변수 선언;]
  BEGIN
      실행문;
  [EXCEPTION
      WHEN 오류조건 THEN
          오류 처리;]
  END [블록라벨];

  핵심 규칙
    - 문장 끝에는 세미콜론(;)을 붙인다.
    - DECLARE는 변수가 있을 때만 작성한다.
    - EXCEPTION은 오류 처리가 필요할 때만 작성한다.
    - BEGIN ... END는 PL/pgSQL 코드 블록을 의미한다.
    - 함수와 프로시저 본문은 보통 달러 인용문으로 감싼다.

  달러 인용문을 사용하는 이유
    - 본문 안의 작은따옴표를 계속 이스케이프하지 않아도 된다.
    - $function$, $procedure$, $do$처럼 의미 있는 태그를 사용할 수 있다.
*/


-- ============================================================================
-- 02. [기초] DO — 객체를 만들지 않고 PL/pgSQL 즉시 실행
-- ============================================================================

DO $do$
BEGIN
    RAISE NOTICE 'PL/pgSQL 익명 블록이 실행되었습니다.';
    RAISE NOTICE '현재 사용자: %, 현재 시각: %',
                 current_user,
                 clock_timestamp();
END;
$do$;

/*
  DO 블록
    - 이름 없는 일회성 함수처럼 동작한다.
    - 값을 외부로 RETURN하지 않는다.
    - 데이터 정리, 관리 작업, 문법 연습에 적합하다.
    - 실행할 때마다 다시 수행되며 데이터베이스 객체로 저장되지 않는다.

  저장하고 반복 호출해야 한다면 FUNCTION 또는 PROCEDURE로 만든다.
*/


-- ============================================================================
-- 03. [기초] 변수 선언, 초기값, 상수, NOT NULL
-- ============================================================================

DO $do$
DECLARE
    -- 자료형만 선언하면 초기값은 NULL이다.
    v_customer_name varchar(100);

    -- := 또는 DEFAULT로 초기값을 지정할 수 있다.
    v_quantity      integer := 2;
    v_unit_price    numeric DEFAULT 50000;

    -- CONSTANT는 선언 후 값을 바꿀 수 없다.
    c_tax_rate      constant numeric := 0.10;

    -- NOT NULL 변수에는 반드시 NULL이 아닌 초기값이 필요하다.
    v_message       text NOT NULL := '계산 시작';

    v_subtotal      numeric;
    v_tax_amount    numeric;
BEGIN
    v_customer_name := '김민준';
    v_subtotal      := v_unit_price * v_quantity;
    v_tax_amount    := round(v_subtotal * c_tax_rate, 2);

    RAISE NOTICE '%: 고객=%, 공급가액=%, 세액=%',
                 v_message,
                 v_customer_name,
                 v_subtotal,
                 v_tax_amount;

    -- 아래 문장은 상수 값을 바꾸므로 오류가 발생한다.
    -- c_tax_rate := 0.08;
END;
$do$;

/*
  대입 연산자
    v_value := 10;  -- PL/pgSQL에서 권장
    v_value = 10;   -- 대입문에서는 허용되지만 :=가 더 명확하다.

  비교 연산자
    IF v_value = 10 THEN ...

  대입과 비교를 혼동하지 않도록 대입에는 :=를 사용하는 습관이 좋다.
*/


-- ============================================================================
-- 04. [기초] %TYPE — 테이블 컬럼과 같은 자료형 사용
-- ============================================================================

DO $do$
DECLARE
    /*
      %TYPE은 컬럼의 현재 자료형을 그대로 사용한다.
      products.unit_price가 numeric(12,2)이므로 변수도 해당 형식을 따른다.
    */
    v_product_name proc_lab.products.product_name%TYPE;
    v_unit_price   proc_lab.products.unit_price%TYPE;
    v_stock_qty    proc_lab.products.stock_qty%TYPE;
BEGIN
    SELECT p.product_name,
           p.unit_price,
           p.stock_qty
      INTO v_product_name,
           v_unit_price,
           v_stock_qty
      FROM proc_lab.products AS p
     WHERE p.product_id = 1;

    RAISE NOTICE '상품=%, 단가=%, 재고=%',
                 v_product_name,
                 v_unit_price,
                 v_stock_qty;
END;
$do$;

/*
  장점
    - 테이블 컬럼 자료형을 코드에 중복 작성하지 않는다.
    - 컬럼 자료형이 변경되면 루틴을 다시 생성할 때 새 자료형이 반영된다.

  주의
    - NOT NULL, CHECK 같은 컬럼 제약조건까지 변수에 복사되는 것은 아니다.
*/


-- ============================================================================
-- 05. [기초] %ROWTYPE과 record — 한 행 전체 저장
-- ============================================================================

DO $do$
DECLARE
    -- 특정 테이블의 고정된 행 구조
    v_customer proc_lab.customers%ROWTYPE;

    -- 실행되는 SELECT 결과에 맞춰 구조가 결정되는 범용 레코드
    v_order_summary record;
BEGIN
    SELECT c.*
      INTO STRICT v_customer
      FROM proc_lab.customers AS c
     WHERE c.customer_id = 1;

    RAISE NOTICE '고객 ID=%, 이름=%, 등급=%',
                 v_customer.customer_id,
                 v_customer.customer_name,
                 v_customer.customer_grade;

    SELECT o.order_id,
           o.order_status,
           o.total_amount,
           count(oi.order_item_id) AS item_count
      INTO v_order_summary
      FROM proc_lab.orders AS o
      LEFT JOIN proc_lab.order_items AS oi
        ON oi.order_id = o.order_id
     WHERE o.order_id = 1
     GROUP BY o.order_id, o.order_status, o.total_amount;

    RAISE NOTICE '주문=%, 상태=%, 금액=%, 품목수=%',
                 v_order_summary.order_id,
                 v_order_summary.order_status,
                 v_order_summary.total_amount,
                 v_order_summary.item_count;
END;
$do$;

/*
  %ROWTYPE
    - 테이블 또는 뷰의 컬럼 구조가 미리 정해져 있다.
    - v_row.column_name 형태로 접근한다.

  record
    - 선언 시점에는 컬럼 구조가 정해져 있지 않다.
    - SELECT INTO, FOR 쿼리, FETCH가 실행되면서 구조가 정해진다.
    - 값을 넣기 전에 record의 필드에 접근하면 오류가 발생한다.
*/


-- ============================================================================
-- 06. [기초] SELECT INTO, STRICT, FOUND
-- ============================================================================

DO $do$
DECLARE
    v_product proc_lab.products%ROWTYPE;
BEGIN
    /*
      PL/pgSQL의 SELECT ... INTO 변수는 조회 결과를 변수에 저장한다.
      SQL의 CREATE TABLE ... AS SELECT와 관련된 SELECT INTO TABLE과 다르다.
    */
    SELECT p.*
      INTO v_product
      FROM proc_lab.products AS p
     WHERE p.product_id = 1;

    IF FOUND THEN
        RAISE NOTICE '상품 조회 성공: %', v_product.product_name;
    ELSE
        RAISE NOTICE '상품을 찾지 못했습니다.';
    END IF;

    /*
      STRICT를 붙이면 조회 행 수를 엄격하게 검사한다.
        0행     : NO_DATA_FOUND
        2행 이상: TOO_MANY_ROWS
        정확히 1행: 정상
    */
    SELECT p.*
      INTO STRICT v_product
      FROM proc_lab.products AS p
     WHERE p.product_id = 2;

    RAISE NOTICE 'STRICT 조회 성공: %', v_product.product_name;
END;
$do$;

/*
  STRICT가 없는 SELECT INTO가 여러 행을 반환하면
  ORDER BY가 없다면 어떤 행이 선택될지 보장되지 않는다.
  한 행만 필요하다면 PK 조건, 집계, ORDER BY ... LIMIT 1 등을 명시한다.
*/


-- ============================================================================
-- 07. [기초] PERFORM — 결과가 필요 없는 SELECT 실행
-- ============================================================================

DO $do$
BEGIN
    /*
      PL/pgSQL에서 결과를 받지 않는 SELECT는 그대로 쓸 수 없다.
      조회 결과는 필요 없고 실행 또는 존재 여부만 필요하면 PERFORM을 쓴다.
    */
    PERFORM 1
    FROM proc_lab.customers AS c
    WHERE c.customer_id = 1
      AND c.active;

    IF FOUND THEN
        RAISE NOTICE '활성 고객이 존재합니다.';
    ELSE
        RAISE NOTICE '활성 고객이 존재하지 않습니다.';
    END IF;
END;
$do$;

/*
  잘못된 예
    SELECT 1 FROM proc_lab.customers WHERE customer_id = 1;

  올바른 예
    PERFORM 1 FROM proc_lab.customers WHERE customer_id = 1;

  결과값이 필요하면 SELECT ... INTO를 사용한다.
*/


-- ============================================================================
-- 08. [기초] IF / ELSIF / ELSE
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_pl_stock_grade(
    p_stock_qty integer
)
RETURNS text
LANGUAGE plpgsql
IMMUTABLE
STRICT
PARALLEL SAFE
AS $function$
BEGIN
    IF p_stock_qty < 0 THEN
        RAISE EXCEPTION '재고는 0 이상이어야 합니다: %', p_stock_qty
            USING ERRCODE = '22003';
    ELSIF p_stock_qty = 0 THEN
        RETURN 'OUT_OF_STOCK';
    ELSIF p_stock_qty < 50 THEN
        RETURN 'LOW';
    ELSIF p_stock_qty < 100 THEN
        RETURN 'NORMAL';
    ELSE
        RETURN 'ENOUGH';
    END IF;
END;
$function$;

SELECT p.product_id,
       p.product_name,
       p.stock_qty,
       proc_lab.fn_pl_stock_grade(p.stock_qty) AS stock_grade
FROM proc_lab.products AS p
ORDER BY p.product_id;


-- ============================================================================
-- 09. [기초] CASE 문과 CASE 표현식
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_pl_status_korean(
    p_status text
)
RETURNS text
LANGUAGE plpgsql
IMMUTABLE
STRICT
PARALLEL SAFE
AS $function$
DECLARE
    v_result text;
BEGIN
    /*
      CASE 문
        각 WHEN에서 여러 PL/pgSQL 문장을 실행할 수 있다.
    */
    CASE upper(p_status)
        WHEN 'PENDING' THEN
            v_result := '주문 대기';
        WHEN 'PAID' THEN
            v_result := '결제 완료';
        WHEN 'SHIPPING' THEN
            v_result := '배송 중';
        WHEN 'COMPLETED' THEN
            v_result := '주문 완료';
        WHEN 'CANCELLED' THEN
            v_result := '주문 취소';
        ELSE
            RAISE EXCEPTION '알 수 없는 주문 상태입니다: %', p_status
                USING ERRCODE = '22023';
    END CASE;

    RETURN v_result;
END;
$function$;

SELECT order_id,
       order_status,
       proc_lab.fn_pl_status_korean(order_status) AS status_korean
FROM proc_lab.orders
ORDER BY order_id;

/*
  SQL의 CASE 표현식은 하나의 값을 계산한다.

  SELECT CASE order_status
             WHEN 'PENDING' THEN '주문 대기'
             WHEN 'PAID' THEN '결제 완료'
             ELSE '기타'
         END
  FROM proc_lab.orders;

  PL/pgSQL CASE 문은 조건에 따라 대입, INSERT, RAISE 등 여러 문장을 실행한다.
*/


-- ============================================================================
-- 10. [중급] 기본 LOOP + EXIT + CONTINUE + 라벨
-- ============================================================================

DO $do$
DECLARE
    v_number integer := 0;
    v_sum    integer := 0;
BEGIN
    <<number_loop>>
    LOOP
        v_number := v_number + 1;

        -- 짝수는 아래 계산을 건너뛴다.
        CONTINUE number_loop WHEN v_number % 2 = 0;

        v_sum := v_sum + v_number;
        RAISE NOTICE '홀수=%, 현재합계=%', v_number, v_sum;

        EXIT number_loop WHEN v_number >= 9;
    END LOOP number_loop;

    RAISE NOTICE '1~9 홀수 합계=%', v_sum;
END;
$do$;

/*
  무한 반복 방지
    LOOP는 자동 종료 조건이 없다.
    반드시 EXIT 또는 RETURN 등 종료 경로가 있어야 한다.

  CONTINUE
    현재 반복의 나머지 문장을 건너뛰고 다음 반복으로 이동한다.

  라벨
    중첩 반복문에서 어떤 LOOP를 EXIT/CONTINUE할지 명확하게 지정한다.
*/


-- ============================================================================
-- 11. [중급] WHILE 반복문
-- ============================================================================

DO $do$
DECLARE
    v_countdown integer := 3;
BEGIN
    WHILE v_countdown > 0 LOOP
        RAISE NOTICE '카운트다운: %', v_countdown;
        v_countdown := v_countdown - 1;
    END LOOP;

    RAISE NOTICE '실행!';
END;
$do$;

/*
  WHILE 조건 LOOP
      반복문;
  END LOOP;

  조건이 처음부터 false이면 한 번도 실행하지 않는다.
*/


-- ============================================================================
-- 12. [중급] 정수 범위 FOR 반복문
-- ============================================================================

DO $do$
DECLARE
    v_total integer := 0;
BEGIN
    FOR i IN 1..5 LOOP
        v_total := v_total + i;
        RAISE NOTICE 'i=%, total=%', i, v_total;
    END LOOP;

    RAISE NOTICE '1부터 5까지 합계=%', v_total;

    -- REVERSE 사용 시 큰 값에서 작은 값으로 반복한다.
    FOR i IN REVERSE 5..1 BY 2 LOOP
        RAISE NOTICE '역순 BY 2: %', i;
    END LOOP;
END;
$do$;

/*
  FOR의 i는 별도로 DECLARE하지 않아도 자동으로 integer 변수로 만들어진다.
  반복 범위의 시작값과 종료값은 반복 시작 시 한 번 평가된다.
*/


-- ============================================================================
-- 13. [중급] 쿼리 결과 FOR 반복문
-- ============================================================================

DO $do$
DECLARE
    v_product record;
BEGIN
    FOR v_product IN
        SELECT p.product_id,
               p.product_name,
               p.unit_price,
               p.stock_qty
        FROM proc_lab.products AS p
        WHERE p.active
        ORDER BY p.unit_price DESC
    LOOP
        RAISE NOTICE '상품번호=%, 상품명=%, 가격=%, 재고=%',
                     v_product.product_id,
                     v_product.product_name,
                     v_product.unit_price,
                     v_product.stock_qty;
    END LOOP;
END;
$do$;

/*
  FOR record IN SELECT ... LOOP
    - 쿼리 결과를 한 행씩 record에 담아 처리한다.
    - 명시적으로 OPEN/FETCH/CLOSE를 작성하지 않아도 된다.

  단순히 모든 행에 같은 UPDATE를 적용할 목적이라면
  반복문보다 집합 기반 UPDATE 한 문장이 일반적으로 더 효율적이다.
*/


-- ============================================================================
-- 14. [중급] FOREACH — 배열 원소 반복
-- ============================================================================

DO $do$
DECLARE
    v_order_ids bigint[] := ARRAY[1, 3, 5];
    v_order_id  bigint;
    v_status    proc_lab.orders.order_status%TYPE;
BEGIN
    FOREACH v_order_id IN ARRAY v_order_ids LOOP
        SELECT o.order_status
          INTO v_status
          FROM proc_lab.orders AS o
         WHERE o.order_id = v_order_id;

        IF FOUND THEN
            RAISE NOTICE 'order_id=%, status=%', v_order_id, v_status;
        ELSE
            RAISE NOTICE 'order_id=%는 존재하지 않습니다.', v_order_id;
        END IF;
    END LOOP;
END;
$do$;

/*
  배열 전체를 SQL 조건으로 전달할 때는 반복하지 않고 ANY를 사용할 수 있다.

  SELECT *
  FROM proc_lab.orders
  WHERE order_id = ANY(ARRAY[1, 3, 5]::bigint[]);

  FOREACH가 필요한 경우
    - 배열 원소마다 서로 다른 절차를 실행할 때

  ANY가 적합한 경우
    - 배열에 포함된 행을 한 번에 조회하거나 변경할 때
*/


-- ============================================================================
-- 15. [중급] RETURN — 단일 값 반환 함수
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_pl_line_total(
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
        RAISE EXCEPTION '단가는 0 이상이어야 합니다.'
            USING ERRCODE = '22003';
    END IF;

    IF p_quantity <= 0 THEN
        RAISE EXCEPTION '수량은 1 이상이어야 합니다.'
            USING ERRCODE = '22003';
    END IF;

    IF p_discount_percent NOT BETWEEN 0 AND 100 THEN
        RAISE EXCEPTION '할인율은 0~100 범위여야 합니다.'
            USING ERRCODE = '22003';
    END IF;

    v_subtotal := p_unit_price * p_quantity;
    v_result   := v_subtotal * (1 - p_discount_percent / 100.0);

    RETURN round(v_result, 2);
END;
$function$;

SELECT proc_lab.fn_pl_line_total(50000, 2)     AS no_discount;
SELECT proc_lab.fn_pl_line_total(50000, 2, 10) AS discount_10_percent;

/*
  RETURN 값;
    - 단일 값을 반환하고 함수 실행을 즉시 종료한다.

  프로시저의 RETURN;
    - 반환값 없이 프로시저 실행만 즉시 종료한다.
    - OUT/INOUT 매개변수 값은 호출자에게 전달된다.
*/


-- ============================================================================
-- 16. [중급] RETURN QUERY — 조회 결과 여러 행 반환
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_pl_customer_orders(
    p_customer_id bigint,
    p_limit       integer DEFAULT 10
)
RETURNS TABLE (
    order_id     bigint,
    order_status varchar,
    total_amount numeric,
    ordered_at   timestamptz
)
LANGUAGE plpgsql
STABLE
STRICT
AS $function$
BEGIN
    IF p_limit NOT BETWEEN 1 AND 100 THEN
        RAISE EXCEPTION '조회 건수는 1~100 범위여야 합니다: %', p_limit
            USING ERRCODE = '22023';
    END IF;

    RETURN QUERY
    SELECT o.order_id,
           o.order_status,
           o.total_amount,
           o.ordered_at
    FROM proc_lab.orders AS o
    WHERE o.customer_id = p_customer_id
    ORDER BY o.ordered_at DESC
    LIMIT p_limit;
END;
$function$;

SELECT *
FROM proc_lab.fn_pl_customer_orders(1, 5);

/*
  RETURN QUERY
    - SELECT 결과 전체를 함수의 결과 집합에 추가한다.
    - RETURN QUERY 뒤에도 함수의 다음 문장이 실행될 수 있다.
    - 즉시 종료하려면 마지막에 RETURN;을 사용할 수 있다.
*/


-- ============================================================================
-- 17. [중급] RETURN NEXT — 결과를 한 행씩 추가
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_pl_discount_schedule(
    p_price numeric
)
RETURNS TABLE (
    discount_percent integer,
    discounted_price numeric
)
LANGUAGE plpgsql
IMMUTABLE
STRICT
PARALLEL SAFE
AS $function$
BEGIN
    IF p_price < 0 THEN
        RAISE EXCEPTION '가격은 0 이상이어야 합니다.'
            USING ERRCODE = '22003';
    END IF;

    FOR v_percent IN 0..30 BY 10 LOOP
        discount_percent := v_percent;
        discounted_price := round(
            p_price * (1 - v_percent / 100.0),
            2
        );

        RETURN NEXT;
    END LOOP;
END;
$function$;

SELECT *
FROM proc_lab.fn_pl_discount_schedule(100000);

/*
  RETURNS TABLE의 컬럼은 함수 내부에서 OUT 변수처럼 사용할 수 있다.
  각 변수에 값을 대입한 후 RETURN NEXT;로 한 행을 결과에 추가한다.

  주의
    RETURN NEXT/RETURN QUERY의 결과는 함수가 끝날 때 호출자에게 반환된다.
    매우 큰 결과 집합은 단순 SQL 함수나 집합 기반 SELECT가 더 적합할 수 있다.
*/


-- ============================================================================
-- 18. [중급] GET DIAGNOSTICS — ROW_COUNT와 실행 문맥
-- ============================================================================

BEGIN;

DO $do$
DECLARE
    v_affected integer;
    v_context  text;
BEGIN
    UPDATE proc_lab.products
       SET stock_qty = stock_qty + 1
     WHERE active
       AND stock_qty < 70;

    GET DIAGNOSTICS
        v_affected = ROW_COUNT,
        v_context  = PG_CONTEXT;

    RAISE NOTICE '재고 변경 행 수=%', v_affected;
    RAISE NOTICE '현재 실행 문맥=%', v_context;
END;
$do$;

SELECT product_id, product_name, stock_qty
FROM proc_lab.products
ORDER BY product_id;

ROLLBACK;

/*
  ROW_COUNT
    직전에 실행한 SQL 문이 처리한 행 수

  PG_CONTEXT
    현재 PL/pgSQL 호출 스택과 실행 위치

  GET DIAGNOSTICS는 정상 실행 후 상태를 확인한다.
  예외가 발생한 뒤의 오류 상세 정보는 GET STACKED DIAGNOSTICS를 사용한다.
*/


-- ============================================================================
-- 19. [중급] RAISE 출력문과 USING 옵션
-- ============================================================================

DO $do$
DECLARE
    v_discount numeric := 10;
    v_price    numeric := 100000;
BEGIN
    RAISE DEBUG   'DEBUG: 상세 개발 정보';
    RAISE INFO    'INFO: 일반 정보';
    RAISE NOTICE  'NOTICE: 할인율=%퍼센트, 가격=%', v_discount, v_price;
    RAISE WARNING 'WARNING: 경고이지만 실행은 계속됩니다.';

    /*
      실제 % 기호가 필요하면 format()의 %%를 사용할 수 있다.
      RAISE 자체에는 결과 문자열 하나만 넘기므로 자리 수 혼동이 줄어든다.
    */
    RAISE NOTICE '%',
        format('할인율=%s%%, 가격=%s', v_discount, v_price);
END;
$do$;

/*
  RAISE 심각도
    DEBUG < LOG < INFO < NOTICE < WARNING < EXCEPTION

  클라이언트에 어떤 수준까지 보일지는 client_min_messages 설정의 영향을 받는다.

  RAISE EXCEPTION '메시지'
      USING ERRCODE = '22023',
            DETAIL  = '상세 원인',
            HINT    = '해결 방법',
            COLUMN  = 'column_name';

  ERRCODE
    - PostgreSQL 표준 SQLSTATE를 사용할 수 있다.
    - 사용자 정의 오류는 P0001 등을 자주 사용한다.
    - 끝 세 자리가 000인 범주 코드는 예외 처리 구분에 불리하므로 피한다.
*/


-- ============================================================================
-- 20. [고급] EXCEPTION — 특정 오류 처리
-- ============================================================================

DO $do$
DECLARE
    v_result numeric;
BEGIN
    BEGIN
        v_result := 100 / 0;
        RAISE NOTICE '결과=%', v_result;
    EXCEPTION
        WHEN division_by_zero THEN
            RAISE NOTICE '0으로 나눌 수 없습니다. 기본값 0을 사용합니다.';
            v_result := 0;
    END;

    RAISE NOTICE '오류 처리 후 실행 계속: result=%', v_result;
END;
$do$;

/*
  자주 사용하는 오류 조건
    NO_DATA_FOUND    : SELECT INTO STRICT 결과가 0행
    TOO_MANY_ROWS    : SELECT INTO STRICT 결과가 2행 이상
    UNIQUE_VIOLATION : UNIQUE/PK 중복, SQLSTATE 23505
    FOREIGN_KEY_VIOLATION : FK 위반, SQLSTATE 23503
    CHECK_VIOLATION  : CHECK 위반, SQLSTATE 23514
    DIVISION_BY_ZERO : 0으로 나눔, SQLSTATE 22012
    OTHERS           : QUERY_CANCELED과 ASSERT_FAILURE를 제외한 나머지

  가능한 경우 OTHERS만 사용하기보다 예상 가능한 오류를 구체적으로 처리한다.
*/


-- ============================================================================
-- 21. [고급] 중첩 블록과 서브트랜잭션 동작
-- ============================================================================

BEGIN;

DO $do$
DECLARE
    v_before integer;
    v_after  integer;
BEGIN
    SELECT p.stock_qty
      INTO v_before
      FROM proc_lab.products AS p
     WHERE p.product_id = 1;

    BEGIN
        UPDATE proc_lab.products
           SET stock_qty = stock_qty - 1
         WHERE product_id = 1;

        -- 내부 블록에서 의도적으로 오류 발생
        RAISE EXCEPTION '내부 작업 실패 테스트'
            USING ERRCODE = 'P0001';

    EXCEPTION
        WHEN SQLSTATE 'P0001' THEN
            /*
              내부 BEGIN 블록에서 실행한 UPDATE는 자동으로 되돌아간다.
              바깥 블록은 계속 실행된다.
            */
            RAISE NOTICE '내부 오류를 처리하여 바깥 블록을 계속합니다.';
    END;

    SELECT p.stock_qty
      INTO v_after
      FROM proc_lab.products AS p
     WHERE p.product_id = 1;

    RAISE NOTICE '오류 전 재고=%, 오류 처리 후 재고=%',
                 v_before,
                 v_after;
END;
$do$;

ROLLBACK;

/*
  EXCEPTION 절이 있는 PL/pgSQL 블록은 내부적으로 서브트랜잭션을 사용한다.
  해당 블록에서 오류가 발생하면 블록 안의 DB 변경은 취소되지만,
  오류를 처리하면 바깥 블록은 계속 실행할 수 있다.

  EXCEPTION 블록은 일반 블록보다 비용이 크므로
  정상 흐름의 조건 분기를 대신하여 과도하게 사용하지 않는다.
*/


-- ============================================================================
-- 22. [고급] GET STACKED DIAGNOSTICS — 오류 상세와 문맥 기록
-- ============================================================================

BEGIN;

DO $do$
DECLARE
    v_state      text;
    v_message    text;
    v_detail     text;
    v_hint       text;
    v_context    text;
    v_table_name text;
BEGIN
    BEGIN
        RAISE EXCEPTION '주문 처리 중 테스트 오류가 발생했습니다.'
            USING ERRCODE = 'P0001',
                  DETAIL = 'order_id=999999',
                  HINT = '주문번호와 현재 상태를 확인하세요.',
                  TABLE = 'orders';

    EXCEPTION
        WHEN OTHERS THEN
            GET STACKED DIAGNOSTICS
                v_state      = RETURNED_SQLSTATE,
                v_message    = MESSAGE_TEXT,
                v_detail     = PG_EXCEPTION_DETAIL,
                v_hint       = PG_EXCEPTION_HINT,
                v_context    = PG_EXCEPTION_CONTEXT,
                v_table_name = TABLE_NAME;

            RAISE NOTICE 'SQLSTATE=%', v_state;
            RAISE NOTICE 'MESSAGE=%', v_message;
            RAISE NOTICE 'DETAIL=%', v_detail;
            RAISE NOTICE 'HINT=%', v_hint;
            RAISE NOTICE 'TABLE=%', v_table_name;
            RAISE NOTICE 'CONTEXT=%', v_context;

            INSERT INTO proc_lab.routine_error_log
                (
                    routine_name,
                    error_state,
                    error_message,
                    error_detail,
                    error_context
                )
            VALUES
                (
                    'plpgsql_diagnostics_demo',
                    v_state,
                    v_message,
                    v_detail,
                    v_context
                );
    END;
END;
$do$;

SELECT log_id,
       routine_name,
       error_state,
       error_message,
       error_detail,
       error_context
FROM proc_lab.routine_error_log
WHERE routine_name = 'plpgsql_diagnostics_demo'
ORDER BY log_id DESC;

ROLLBACK;

/*
  GET STACKED DIAGNOSTICS는 반드시 EXCEPTION 처리 절 안에서 사용한다.

  오류를 처리한 뒤 RAISE;를 실행하면 같은 오류를 다시 호출자에게 전달한다.
  그러나 같은 트랜잭션 전체가 롤백되면 방금 INSERT한 오류 로그도 사라진다.

  오류 로그를 반드시 보존해야 한다면
    - 현재 예제처럼 오류를 흡수하고 결과 코드로 반환하거나
    - 별도 연결/외부 로깅 시스템을 사용하거나
    - 애플리케이션에서 롤백 후 별도 트랜잭션으로 기록하는 방식을 검토한다.
*/


-- ============================================================================
-- 23. [고급] 예외 재발생과 메시지 추가
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_pl_find_product_strict(
    p_product_id bigint
)
RETURNS proc_lab.products
LANGUAGE plpgsql
STABLE
STRICT
AS $function$
#print_strict_params on
DECLARE
    v_product proc_lab.products%ROWTYPE;
BEGIN
    SELECT p.*
      INTO STRICT v_product
      FROM proc_lab.products AS p
     WHERE p.product_id = p_product_id;

    RETURN v_product;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE EXCEPTION '상품번호 %를 찾을 수 없습니다.', p_product_id
            USING ERRCODE = 'P0002',
                  HINT = 'proc_lab.products의 product_id를 확인하세요.';
END;
$function$;

SELECT (proc_lab.fn_pl_find_product_strict(1)).*;

-- 오류 실습: 주석을 해제하면 사용자 메시지와 HINT를 확인할 수 있다.
-- SELECT (proc_lab.fn_pl_find_product_strict(999999)).*;

/*
  #print_strict_params on
    SELECT INTO STRICT 관련 오류가 발생할 때 함수 매개변수 값을
    DETAIL에 표시하도록 하는 PL/pgSQL 컴파일 옵션이다.

  함수 전체가 아니라 세션 설정으로도 사용할 수 있다.
    SET plpgsql.print_strict_params = on;
*/


-- ============================================================================
-- 24. [고급] 안전한 동적 SQL — EXECUTE + format() + USING
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_pl_dynamic_count(
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
      컬럼 존재 여부를 시스템 카탈로그에서 확인한다.
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
      식별자와 값의 처리 방법은 다르다.

      식별자
        - 테이블명, 컬럼명은 바인딩 매개변수로 전달할 수 없다.
        - regclass, format()의 %I를 사용한다.

      값
        - 문자열에 연결하지 않는다.
        - $1 자리와 EXECUTE ... USING을 사용한다.
    */
    v_sql := format(
        'SELECT count(*) FROM %s WHERE %I::text = $1',
        p_table,
        p_column
    );

    RAISE NOTICE '실행 SQL=% / 바인딩 값=%', v_sql, p_value;

    EXECUTE v_sql
       INTO v_count
       USING p_value;

    RETURN v_count;
END;
$function$;

SELECT proc_lab.fn_pl_dynamic_count(
           'proc_lab.orders'::regclass,
           'order_status',
           'PENDING'
       ) AS pending_count;

-- SQL Injection처럼 보이지만 USING에 의해 하나의 문자열 값으로 처리된다.
SELECT proc_lab.fn_pl_dynamic_count(
           'proc_lab.orders'::regclass,
           'order_status',
           $$PENDING' OR '1'='1$$
       ) AS injection_attempt_count;

/*
  위험한 방식
    v_sql := 'SELECT ... WHERE column = ''' || p_value || '''';

  문제점
    - 작은따옴표 처리 오류
    - SQL Injection 위험
    - 자료형 변환 문제

  안전한 방식
    EXECUTE '... WHERE column = $1' USING p_value;
*/


-- ============================================================================
-- 25. [고급] 동적 DML과 RETURNING
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_pl_update_note_dynamic(
    p_table    regclass,
    p_id_column name,
    p_id_value bigint,
    p_note     text
)
RETURNS integer
LANGUAGE plpgsql
VOLATILE
STRICT
AS $function$
DECLARE
    v_sql      text;
    v_affected integer;
BEGIN
    /*
      교육 목적상 note 컬럼을 가진 허용 테이블을 proc_lab.orders로 제한한다.
      동적 SQL을 범용으로 만들수록 권한과 대상 검증이 중요해진다.
    */
    IF p_table <> 'proc_lab.orders'::regclass THEN
        RAISE EXCEPTION '허용되지 않은 대상 테이블입니다: %', p_table
            USING ERRCODE = '42501';
    END IF;

    IF p_id_column <> 'order_id'::name THEN
        RAISE EXCEPTION '허용되지 않은 ID 컬럼입니다: %', p_id_column
            USING ERRCODE = '42501';
    END IF;

    v_sql := format(
        'UPDATE %s SET note = $1, updated_at = clock_timestamp() ' ||
        'WHERE %I = $2',
        p_table,
        p_id_column
    );

    EXECUTE v_sql USING p_note, p_id_value;
    GET DIAGNOSTICS v_affected = ROW_COUNT;

    RETURN v_affected;
END;
$function$;

BEGIN;

SELECT proc_lab.fn_pl_update_note_dynamic(
           'proc_lab.orders'::regclass,
           'order_id',
           1,
           'PL/pgSQL 동적 DML 테스트'
       ) AS affected_count;

SELECT order_id, note, updated_at
FROM proc_lab.orders
WHERE order_id = 1;

ROLLBACK;


-- ============================================================================
-- 26. [고급] 명시적 커서 OPEN / FETCH / CLOSE
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_pl_products_cursor(
    p_min_price numeric
)
RETURNS TABLE (
    product_id   bigint,
    product_name varchar,
    unit_price   numeric
)
LANGUAGE plpgsql
STABLE
STRICT
AS $function$
DECLARE
    v_product record;

    cur_products CURSOR (v_min_price numeric) FOR
        SELECT p.product_id,
               p.product_name,
               p.unit_price
        FROM proc_lab.products AS p
        WHERE p.active
          AND p.unit_price >= v_min_price
        ORDER BY p.unit_price DESC;
BEGIN
    IF p_min_price < 0 THEN
        RAISE EXCEPTION '최소 가격은 0 이상이어야 합니다.'
            USING ERRCODE = '22003';
    END IF;

    OPEN cur_products(p_min_price);

    LOOP
        FETCH cur_products INTO v_product;
        EXIT WHEN NOT FOUND;

        product_id   := v_product.product_id;
        product_name := v_product.product_name;
        unit_price   := v_product.unit_price;

        RETURN NEXT;
    END LOOP;

    CLOSE cur_products;
END;
$function$;

SELECT *
FROM proc_lab.fn_pl_products_cursor(80000);

/*
  커서가 유용한 경우
    - 한 행씩 순차 처리해야 함
    - 일부 행만 FETCH해야 함
    - 외부 클라이언트에 refcursor를 전달해야 함

  커서가 불필요한 경우
    - 단순 조회: RETURN QUERY SELECT ...
    - 동일 일괄 변경: UPDATE ... WHERE ...

  PL/pgSQL의 FOR record IN SELECT는 내부적으로 커서를 관리하므로
  대부분의 행 반복에는 명시적 OPEN/FETCH/CLOSE보다 간단하다.
*/


-- ============================================================================
-- 27. [고급] 블록 라벨과 변수 범위
-- ============================================================================

DO $do$
<<outer_block>>
DECLARE
    v_value integer := 10;
BEGIN
    RAISE NOTICE '바깥 블록 값=%', v_value;

    <<inner_block>>
    DECLARE
        v_value integer := 20;
    BEGIN
        RAISE NOTICE '안쪽 블록 값=%', v_value;
        RAISE NOTICE '라벨로 접근한 바깥 블록 값=%',
                     outer_block.v_value;
    END inner_block;

    RAISE NOTICE '안쪽 블록 종료 후 바깥 값=%', v_value;
END outer_block;
$do$;

/*
  변수 범위
    - 변수는 선언된 블록과 그 안쪽 블록에서 사용할 수 있다.
    - 안쪽 블록에서 같은 이름을 선언하면 바깥 변수를 가린다.
    - 블록라벨.변수명으로 가려진 바깥 변수에 접근할 수 있다.

  실무에서는 같은 이름으로 변수를 중복 선언하기보다
  v_, p_, c_ 접두어로 역할을 명확히 하는 것이 읽기 쉽다.
*/


-- ============================================================================
-- 28. [고급] 변수명과 컬럼명 충돌 방지
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_pl_customer_name(
    p_customer_id bigint
)
RETURNS text
LANGUAGE plpgsql
STABLE
STRICT
AS $function$
DECLARE
    v_customer_name text;
BEGIN
    /*
      매개변수는 p_, 지역변수는 v_, 테이블은 별칭 c를 사용한다.
      어느 이름이 변수이고 어느 이름이 컬럼인지 명확하다.
    */
    SELECT c.customer_name
      INTO STRICT v_customer_name
      FROM proc_lab.customers AS c
     WHERE c.customer_id = p_customer_id;

    RETURN v_customer_name;
END;
$function$;

SELECT proc_lab.fn_pl_customer_name(1);

/*
  피해야 할 예
    DECLARE customer_id bigint;
    SELECT customer_name
    FROM customers
    WHERE customer_id = customer_id;

  위 조건은 변수와 컬럼 중 어느 것을 의미하는지 모호하다.

  권장 규칙
    p_ : 입력 매개변수
    v_ : 지역 변수
    c_ : 상수
    테이블 컬럼: 항상 테이블 별칭.컬럼명
*/


-- ============================================================================
-- 29. [고급] ASSERT — 개발 단계의 내부 가정 검증
-- ============================================================================

DO $do$
DECLARE
    v_quantity integer := 10;
BEGIN
    ASSERT v_quantity > 0,
           '내부 가정 위반: 수량은 양수여야 합니다.';

    RAISE NOTICE 'ASSERT 통과: quantity=%', v_quantity;
END;
$do$;

/*
  ASSERT 조건, '메시지';

  - 개발 중 "반드시 참이어야 하는 내부 조건"을 검사한다.
  - 조건이 false이면 ASSERT_FAILURE 오류가 발생한다.
  - plpgsql.check_asserts 설정으로 비활성화될 수 있다.
  - 사용자 입력 검증을 ASSERT에만 의존하면 안 된다.

  사용자 입력 검증
    IF 잘못된조건 THEN
        RAISE EXCEPTION ...;
    END IF;

  개발자의 내부 가정 검증
    ASSERT 반드시참인조건, '메시지';
*/


-- ============================================================================
-- 30. [고급] 트랜잭션과 PL/pgSQL의 BEGIN 구분
-- ============================================================================

/*
  다음 두 BEGIN은 이름은 같지만 역할이 다르다.

  SQL 트랜잭션 시작
    BEGIN;
    UPDATE ...;
    COMMIT;

  PL/pgSQL 코드 블록 시작
    DO $do$
    BEGIN
        RAISE NOTICE '코드 블록';
    END;
    $do$;

  함수 안의 BEGIN은 트랜잭션 시작이 아니다.
  함수는 호출자의 트랜잭션 안에서 실행되며 COMMIT/ROLLBACK할 수 없다.
*/

CREATE OR REPLACE PROCEDURE proc_lab.pr_pl_transaction_demo(
    IN p_first_message  text,
    IN p_second_message text
)
LANGUAGE plpgsql
AS $procedure$
BEGIN
    INSERT INTO proc_lab.transaction_demo_log(message)
    VALUES (p_first_message);

    COMMIT;

    INSERT INTO proc_lab.transaction_demo_log(message)
    VALUES (p_second_message);

    ROLLBACK;
END;
$procedure$;

/*
  아래 CALL은 자동 실행하지 않는다.
  DBeaver에서 Auto-commit 상태로 CALL 문 하나만 선택해 실행한다.

  CALL proc_lab.pr_pl_transaction_demo(
      'COMMIT으로 보존할 메시지',
      'ROLLBACK으로 취소할 메시지'
  );

  SELECT *
  FROM proc_lab.transaction_demo_log
  ORDER BY log_id;

  프로시저의 COMMIT/ROLLBACK 주요 제한
    - CALL이 최상위 명령이어야 한다.
    - 명시적 BEGIN ... COMMIT 안에서 호출하면 안 된다.
    - 함수에서 간접 호출한 프로시저이면 안 된다.
    - SECURITY DEFINER 프로시저이면 안 된다.
    - CREATE PROCEDURE에 SET 절이 있으면 안 된다.
    - EXCEPTION 블록 안에서는 트랜잭션을 종료할 수 없다.
*/


-- ============================================================================
-- 31. [고급] PL/pgSQL 트리거 함수의 특별 변수
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_pl_set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $function$
BEGIN
    /*
      BEFORE UPDATE 트리거 함수에서는 NEW 값을 직접 바꿀 수 있다.
      변경된 NEW를 반환하면 실제 UPDATE에 반영된다.
    */
    NEW.updated_at := clock_timestamp();
    RETURN NEW;
END;
$function$;

/*
  트리거 함수의 주요 특별 변수
    NEW           : INSERT/UPDATE의 새 행
    OLD           : UPDATE/DELETE의 기존 행
    TG_OP         : INSERT, UPDATE, DELETE, TRUNCATE
    TG_TABLE_NAME : 트리거가 실행된 테이블 이름
    TG_TABLE_SCHEMA: 테이블 스키마 이름
    TG_WHEN       : BEFORE, AFTER, INSTEAD OF
    TG_LEVEL      : ROW, STATEMENT
    TG_ARGV       : 트리거 정의에서 전달한 문자열 인수 배열

  아래 트리거는 Trigger 완전 실습 파일과 함께 실행할 때 중복 동작을 피하도록
  기본적으로 생성하지 않는다. 단독 실습 시에만 주석을 해제한다.

  DROP TRIGGER IF EXISTS trg_pl_orders_updated_at ON proc_lab.orders;

  CREATE TRIGGER trg_pl_orders_updated_at
  BEFORE UPDATE
  ON proc_lab.orders
  FOR EACH ROW
  EXECUTE FUNCTION proc_lab.fn_pl_set_updated_at();

  AFTER 트리거에서는 반환한 NEW가 실제 행 변경에 반영되지 않는다.
  AFTER에서 다시 UPDATE하면 추가 쓰기와 재귀 호출 위험이 생기므로
  updated_at 자동 변경은 BEFORE 트리거가 적합하다.
*/


-- ============================================================================
-- 32. [고급] SECURITY DEFINER와 search_path 고정
-- ============================================================================

CREATE OR REPLACE FUNCTION proc_lab.fn_pl_customer_exists(
    p_customer_id bigint
)
RETURNS boolean
LANGUAGE plpgsql
STABLE
STRICT
SECURITY DEFINER
SET search_path = pg_catalog, proc_lab, pg_temp
AS $function$
DECLARE
    v_exists boolean;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM proc_lab.customers AS c
        WHERE c.customer_id = p_customer_id
    )
      INTO v_exists;

    RETURN v_exists;
END;
$function$;

REVOKE ALL
ON FUNCTION proc_lab.fn_pl_customer_exists(bigint)
FROM PUBLIC;

SELECT proc_lab.fn_pl_customer_exists(1) AS customer_1_exists;

/*
  SECURITY INVOKER
    - 호출자 권한으로 실행
    - 기본값

  SECURITY DEFINER
    - 함수 소유자 권한으로 실행
    - 권한 상승이 가능하므로 특별한 주의가 필요

  안전한 작성 방법
    1. search_path를 pg_catalog, 신뢰 스키마, pg_temp 순으로 고정
    2. 테이블을 스키마까지 완전 수식
    3. PUBLIC의 기본 EXECUTE 권한 제거
    4. 필요한 애플리케이션 역할에만 EXECUTE 부여
    5. 동적 SQL의 대상 객체와 값을 엄격히 검증

  운영 역할이 존재할 때만 다음 예의 역할명을 바꿔 실행한다.

  GRANT EXECUTE
  ON FUNCTION proc_lab.fn_pl_customer_exists(bigint)
  TO app_user;
*/


-- ============================================================================
-- 33. [성능] 행 단위 반복 처리와 집합 기반 SQL 비교
-- ============================================================================

/*
  예제 A: 행 단위 처리
    상품마다 UPDATE가 한 번씩 실행된다.
*/
BEGIN;

DO $do$
DECLARE
    v_product record;
    v_started_at timestamptz := clock_timestamp();
BEGIN
    FOR v_product IN
        SELECT p.product_id
        FROM proc_lab.products AS p
        WHERE p.active
          AND p.stock_qty < 100
    LOOP
        UPDATE proc_lab.products
           SET stock_qty = stock_qty + 1
         WHERE product_id = v_product.product_id;
    END LOOP;

    RAISE NOTICE '행 단위 반복 처리 시간=%',
                 clock_timestamp() - v_started_at;
END;
$do$;

ROLLBACK;

/*
  예제 B: 집합 기반 처리
    조건에 맞는 상품을 UPDATE 한 문장으로 변경한다.
*/
BEGIN;

DO $do$
DECLARE
    v_affected  integer;
    v_started_at timestamptz := clock_timestamp();
BEGIN
    UPDATE proc_lab.products
       SET stock_qty = stock_qty + 1
     WHERE active
       AND stock_qty < 100;

    GET DIAGNOSTICS v_affected = ROW_COUNT;

    RAISE NOTICE '집합 기반 처리 시간=%, 처리건수=%',
                 clock_timestamp() - v_started_at,
                 v_affected;
END;
$do$;

ROLLBACK;

/*
  샘플 데이터가 5행뿐이므로 시간 차이가 의미 있게 나타나지 않을 수 있다.
  대량 데이터에서는 SQL 문 실행 횟수, 문맥 전환, 잠금 관리 차이가 커진다.

  반복문을 사용해야 하는 경우
    - 행마다 서로 다른 복잡한 판단
    - 행별 독립 예외 처리
    - 외부 시스템 또는 서로 다른 동적 대상 호출

  집합 기반 SQL이 적합한 경우
    - 같은 조건과 계산으로 여러 행 조회/변경
    - JOIN, 집계, 일괄 UPDATE/INSERT/DELETE
*/


-- ============================================================================
-- 34. [성능] 정적 SQL, 실행계획 캐시, 동적 SQL
-- ============================================================================

/*
  PL/pgSQL의 정적 SQL
    SELECT ... INTO
    UPDATE ...
    INSERT ...

  특징
    - PL/pgSQL이 SQL 문을 준비하고 실행계획을 재사용할 수 있다.
    - 변수 값은 SQL 매개변수처럼 전달된다.
    - 대부분의 업무 로직은 정적 SQL을 우선한다.

  동적 SQL EXECUTE
    - 실행할 때마다 문자열을 해석하고 계획한다.
    - 테이블명, 컬럼명, 정렬 방향처럼 SQL 구조가 바뀔 때 사용한다.
    - 값만 달라지는 경우에는 동적 SQL이 필요 없다.

  잘못된 동적 SQL 사용
    EXECUTE format(
        'SELECT * FROM proc_lab.orders WHERE order_id = %s',
        p_order_id
    );

  값만 달라진다면 정적 SQL이 가장 단순하다.
    SELECT *
    FROM proc_lab.orders
    WHERE order_id = p_order_id;

  동적 SQL이 꼭 필요하다면 값을 USING으로 전달한다.
    EXECUTE 'SELECT ... WHERE order_id = $1' USING p_order_id;
*/


-- ============================================================================
-- 35. [품질] 추가 경고와 코드 검사 설정
-- ============================================================================

/*
  PL/pgSQL은 컴파일 시 추가 경고와 오류 검사를 활성화할 수 있다.

  현재 세션에서 추가 경고 활성화
    SET plpgsql.extra_warnings = 'all';

  특정 항목만 경고
    SET plpgsql.extra_warnings =
        'shadowed_variables,strict_multi_assignment';

  경고를 오류로 처리
    SET plpgsql.extra_errors = 'shadowed_variables';

  대표 검사 항목
    shadowed_variables
      안쪽 블록 변수가 바깥 변수와 같은 이름을 사용

    strict_multi_assignment
      SELECT 결과 컬럼 수와 INTO 대상 변수 수가 맞지 않음

    too_many_rows
      STRICT가 없는 SELECT INTO에 여러 행이 반환될 가능성

  교육 실습 전체에 영향을 주지 않도록 이 파일에서는 SET을 자동 실행하지 않는다.
*/


-- ============================================================================
-- 36. 함수·프로시저·트리거 함수에서 PL/pgSQL의 역할
-- ============================================================================

/*
  객체              호출 방식                      PL/pgSQL의 역할
  ---------------------------------------------------------------------------
  FUNCTION          SELECT fn_name(...)            계산·조회·값 반환
  PROCEDURE         CALL pr_name(...)              업무 절차·여러 DML
  TRIGGER FUNCTION  트리거 이벤트가 자동 호출      OLD/NEW 기반 자동 처리
  DO BLOCK          DO $do$ ... $do$               일회성 관리·문법 실습

  LANGUAGE sql이 적합한 경우
    - SQL 한 문장으로 충분함
    - 조건문, 변수, 예외 처리가 필요 없음

  LANGUAGE plpgsql이 적합한 경우
    - 여러 SQL 문을 순서대로 실행
    - 변수, IF, LOOP, EXCEPTION 필요
    - 동적 SQL과 상세 진단 필요

  단순한 SQL 한 문장을 억지로 PL/pgSQL로 감싸면
  코드가 길어지고 오히려 이해와 최적화가 어려워질 수 있다.
*/


-- ============================================================================
-- 37. PL/pgSQL 루틴 메타데이터와 정의 조회
-- ============================================================================

SELECT n.nspname AS routine_schema,
       p.proname AS routine_name,
       CASE p.prokind
           WHEN 'f' THEN 'FUNCTION'
           WHEN 'p' THEN 'PROCEDURE'
           WHEN 'a' THEN 'AGGREGATE'
           WHEN 'w' THEN 'WINDOW'
       END AS routine_kind,
       pg_catalog.pg_get_function_identity_arguments(p.oid)
           AS identity_arguments,
       pg_catalog.pg_get_function_result(p.oid)
           AS result_type,
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
  AND l.lanname = 'plpgsql'
ORDER BY p.proname,
         pg_catalog.pg_get_function_identity_arguments(p.oid);

-- 특정 함수의 전체 정의문 확인
SELECT pg_catalog.pg_get_functiondef(
           'proc_lab.fn_pl_line_total(numeric,integer,numeric)'::regprocedure
       );


-- ============================================================================
-- 38. PL/pgSQL 객체 관리
-- ============================================================================

/*
  정의 변경
    CREATE OR REPLACE FUNCTION ...
    CREATE OR REPLACE PROCEDURE ...

  설명
    COMMENT ON FUNCTION 함수명(입력자료형...) IS '설명';
    COMMENT ON PROCEDURE 프로시저명(입력자료형...) IS '설명';

  권한
    GRANT EXECUTE ON FUNCTION 함수명(입력자료형...) TO 역할명;
    GRANT EXECUTE ON PROCEDURE 프로시저명(입력자료형...) TO 역할명;

  삭제
    DROP FUNCTION 함수명(입력자료형...);
    DROP PROCEDURE 프로시저명(입력자료형...);

  함수와 프로시저는 오버로딩될 수 있으므로
  관리 명령에는 입력 매개변수 자료형을 함께 지정하는 습관이 중요하다.
*/

COMMENT ON FUNCTION proc_lab.fn_pl_line_total(numeric, integer, numeric)
IS 'PL/pgSQL 변수·검증·RETURN을 설명하는 주문 항목 금액 계산 함수';

-- 필요할 때만 주석을 해제한다.
-- DROP FUNCTION proc_lab.fn_pl_line_total(numeric, integer, numeric);
-- DROP PROCEDURE proc_lab.pr_pl_transaction_demo(text, text);


-- ============================================================================
-- 39. 최종 확인
-- ============================================================================

SELECT 'fn_pl_stock_grade' AS example_name,
       proc_lab.fn_pl_stock_grade(40) AS result
UNION ALL
SELECT 'fn_pl_status_korean',
       proc_lab.fn_pl_status_korean('PAID')
UNION ALL
SELECT 'fn_pl_line_total',
       proc_lab.fn_pl_line_total(50000, 2, 10)::text
UNION ALL
SELECT 'fn_pl_customer_name',
       proc_lab.fn_pl_customer_name(1)
UNION ALL
SELECT 'fn_pl_customer_exists',
       proc_lab.fn_pl_customer_exists(1)::text;


-- ============================================================================
-- 40. 복습 문제
-- ============================================================================

/*
  [문제 1]
  DO 블록에서 products의 product_id=3을 %ROWTYPE으로 조회하고
  상품명, 단가, 재고를 RAISE NOTICE로 출력한다.

  [문제 2]
  fn_pl_customer_grade_label(p_customer_id)를 만든다.
  customers를 SELECT INTO STRICT로 조회하여 다음 값을 반환한다.
    VIP    → '최우수 고객'
    GOLD   → '우수 고객'
    SILVER → '일반 고객'
    BASIC  → '신규 고객'

  [문제 3]
  fn_pl_products_below_stock(p_stock_qty)를 RETURNS TABLE로 만들고
  기준보다 재고가 적은 활성 상품을 RETURN QUERY로 반환한다.

  [문제 4]
  pr_pl_bulk_activate_products(p_product_ids bigint[], OUT p_affected)를 만든다.
  반복문 대신 product_id = ANY(p_product_ids)를 사용하고
  GET DIAGNOSTICS로 처리 건수를 구한다.

  [문제 5]
  존재하지 않는 주문번호를 SELECT INTO STRICT로 조회하고
  EXCEPTION에서 GET STACKED DIAGNOSTICS를 사용하여
  SQLSTATE, 메시지, 오류 문맥을 RAISE NOTICE로 출력한다.

  [문제 6]
  동적 SQL 함수에 다음과 같은 값이 들어와도 SQL Injection이 되지 않는 이유를
  format()의 %I와 EXECUTE ... USING 관점에서 설명한다.
    $$PENDING' OR '1'='1$$

  [문제 7]
  상품 100개의 재고를 1씩 증가시키는 작업을 가정하고
  FOR 반복문 UPDATE와 집합 기반 UPDATE의 차이를 설명한다.

  [문제 8]
  함수 안의 BEGIN과 SQL 클라이언트에서 실행하는 BEGIN이
  각각 무엇을 시작하는지 설명한다.
*/
