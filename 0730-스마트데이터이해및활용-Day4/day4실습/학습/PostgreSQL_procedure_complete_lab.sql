/*
===============================================================================
 PostgreSQL PROCEDURE 완전 실습
===============================================================================
 대상 버전 : PostgreSQL 14 이상
 실습 도구 : DBeaver, pgAdmin, psql
 선행 파일 : PostgreSQL_day04_프로시저_함수_샘플_스키마_DDL_DML.sql
 사용 스키마: proc_lab

 학습 목표
   1. CREATE PROCEDURE와 CALL 문법을 이해한다.
   2. IN / OUT / INOUT 매개변수를 사용한다.
   3. 여러 테이블의 DML을 하나의 업무 절차로 묶는다.
   4. SELECT INTO, PERFORM, GET DIAGNOSTICS, RAISE를 사용한다.
   5. 멱등성, UPSERT, 집합 기반 배치, 행 잠금을 구현한다.
   6. 예외 정보와 오류 문맥을 기록한다.
   7. 프로시저의 트랜잭션 제어 가능 조건과 보안 설정을 이해한다.

 함수와 프로시저의 핵심 차이
   - 함수      : SELECT 식 안에서 호출하고 결과를 반환한다.
   - 프로시저  : CALL로 호출하고 업무 절차를 수행한다.
   - 프로시저는 OUT/INOUT 매개변수로 결과를 돌려줄 수 있다.
   - 최상위 CALL 등 조건을 만족하면 프로시저에서 COMMIT/ROLLBACK이 가능하다.

 실행 안내
   - 위에서 아래로 실행할 수 있다.
   - 일반 DML 프로시저 테스트는 BEGIN ... ROLLBACK으로 원상 복구한다.
   - 트랜잭션 제어 데모 CALL은 기본적으로 주석 처리했다.
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

DO $block$
BEGIN
    IF to_regclass('proc_lab.customers') IS NULL
       OR to_regclass('proc_lab.products') IS NULL
       OR to_regclass('proc_lab.orders') IS NULL
       OR to_regclass('proc_lab.order_items') IS NULL
       OR to_regclass('proc_lab.order_status_history') IS NULL
       OR to_regclass('proc_lab.routine_error_log') IS NULL THEN
        RAISE EXCEPTION
            'proc_lab 샘플 테이블이 없습니다. 선행 DDL/DML 파일을 먼저 실행하세요.';
    END IF;
END;
$block$;


-- ============================================================================
-- 01. 프로시저 기본 구조와 호출 방법
-- ============================================================================

/*
  기본 구조

  CREATE OR REPLACE PROCEDURE 스키마.프로시저명(
      IN    입력값 자료형,
      OUT   출력값 자료형,
      INOUT 입출력값 자료형
  )
  LANGUAGE plpgsql
  AS $procedure$
  DECLARE
      지역변수 자료형;
  BEGIN
      업무 처리;
  END;
  $procedure$;

  호출
    CALL proc_lab.프로시저명(입력값, NULL);

  OUT 매개변수 자리에도 CALL 문에서는 보통 NULL을 전달한다.
  실행 결과는 DBeaver의 Results 영역에 한 행으로 표시된다.
*/


-- ============================================================================
-- 02. [기초] IN + OUT + 변수 + RAISE NOTICE
-- ============================================================================

CREATE OR REPLACE PROCEDURE proc_lab.pr_calculate_line_total(
    IN  p_unit_price       numeric,
    IN  p_quantity         integer,
    IN  p_discount_percent numeric,
    OUT p_line_total       numeric
)
LANGUAGE plpgsql
AS $procedure$
DECLARE
    v_subtotal numeric;
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

    v_subtotal  := p_unit_price * p_quantity;
    p_line_total := round(
        v_subtotal * (1 - p_discount_percent / 100.0),
        2
    );

    RAISE NOTICE
        '단가=%, 수량=%, 할인율=%퍼센트, 계산결과=%',
        p_unit_price,
        p_quantity,
        p_discount_percent,
        p_line_total;
END;
$procedure$;

CALL proc_lab.pr_calculate_line_total(50000, 2, 10, NULL);

/*
  출력문 종류
    RAISE DEBUG     '...';
    RAISE LOG       '...';
    RAISE INFO      '...';
    RAISE NOTICE    '...';  -- 실습 디버깅에 가장 많이 사용
    RAISE WARNING   '...';
    RAISE EXCEPTION '...';  -- 오류 발생, 현재 처리를 중단

  RAISE 문자열의 %는 값 자리 표시자이다.
  값 개수와 % 자리 수가 일치해야 한다.
*/


-- ============================================================================
-- 03. [기초] 주문 생성 — INOUT + 여러 테이블 DML + 행 잠금
-- ============================================================================

CREATE OR REPLACE PROCEDURE proc_lab.pr_create_order(
    IN    p_customer_id bigint,
    IN    p_product_id  bigint,
    IN    p_quantity    integer,
    INOUT p_order_id    bigint DEFAULT NULL
)
LANGUAGE plpgsql
AS $procedure$
DECLARE
    v_product proc_lab.products%ROWTYPE;
BEGIN
    RAISE NOTICE
        '[입력] customer_id=%, product_id=%, quantity=%',
        p_customer_id, p_product_id, p_quantity;

    IF p_quantity <= 0 THEN
        RAISE EXCEPTION '주문 수량은 1 이상이어야 합니다.'
            USING ERRCODE = '22003';
    END IF;

    -- 고객 존재 여부 확인. PERFORM은 SELECT 결과 자체가 필요 없을 때 사용한다.
    PERFORM 1
    FROM proc_lab.customers AS c
    WHERE c.customer_id = p_customer_id
      AND c.active;

    IF NOT FOUND THEN
        RAISE EXCEPTION '활성 고객 %를 찾을 수 없습니다.', p_customer_id
            USING ERRCODE = 'P0002';
    END IF;

    /*
      상품 행을 잠가 같은 상품의 동시 주문이 재고를 함께 변경하지 못하게 한다.
      SELECT INTO STRICT는 상품이 없을 때 NO_DATA_FOUND를 발생시킨다.
    */
    SELECT p.*
      INTO STRICT v_product
      FROM proc_lab.products AS p
     WHERE p.product_id = p_product_id
       AND p.active
     FOR UPDATE;

    RAISE NOTICE
        '[조회] product=%, unit_price=%, current_stock=%',
        v_product.product_name,
        v_product.unit_price,
        v_product.stock_qty;

    IF v_product.stock_qty < p_quantity THEN
        RAISE EXCEPTION '재고가 부족합니다. 현재=%, 요청=%',
                        v_product.stock_qty, p_quantity
            USING ERRCODE = 'P0001';
    END IF;

    UPDATE proc_lab.products
       SET stock_qty = stock_qty - p_quantity
     WHERE product_id = p_product_id;

    INSERT INTO proc_lab.orders
        (customer_id, order_status, total_amount)
    VALUES
        (
            p_customer_id,
            'PENDING',
            round(v_product.unit_price * p_quantity, 2)
        )
    RETURNING order_id INTO p_order_id;

    INSERT INTO proc_lab.order_items
        (order_id, product_id, quantity, unit_price)
    VALUES
        (p_order_id, p_product_id, p_quantity, v_product.unit_price);

    RAISE NOTICE '[완료] 새 주문번호=%, 남은 재고=%',
                 p_order_id,
                 v_product.stock_qty - p_quantity;

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE EXCEPTION '활성 상품 %를 찾을 수 없습니다.', p_product_id
            USING ERRCODE = 'P0002';
END;
$procedure$;

/*
  프로시저 내부에서 오류가 처리되지 않고 밖으로 전파되면
  해당 CALL의 변경 작업은 모두 롤백된다.
  따라서 재고만 차감되고 주문은 생성되지 않는 중간 상태를 막을 수 있다.
*/
BEGIN;

CALL proc_lab.pr_create_order(1, 2, 2, NULL);

SELECT order_id, customer_id, order_status, total_amount
FROM proc_lab.orders
ORDER BY order_id DESC
LIMIT 1;

SELECT product_id, product_name, stock_qty
FROM proc_lab.products
WHERE product_id = 2;

ROLLBACK;


-- ============================================================================
-- 04. [중급] 상태 변경 — OUT + GET DIAGNOSTICS + 멱등성
-- ============================================================================

CREATE OR REPLACE PROCEDURE proc_lab.pr_change_order_status(
    IN  p_order_id   bigint,
    IN  p_new_status text,
    OUT p_message    text
)
LANGUAGE plpgsql
AS $procedure$
DECLARE
    v_old_status proc_lab.orders.order_status%TYPE;
    v_row_count  integer;
BEGIN
    IF p_new_status NOT IN
       ('PENDING', 'PAID', 'SHIPPING', 'COMPLETED', 'CANCELLED') THEN
        RAISE EXCEPTION '허용되지 않은 주문 상태입니다: %', p_new_status
            USING ERRCODE = '22023';
    END IF;

    SELECT o.order_status
      INTO STRICT v_old_status
      FROM proc_lab.orders AS o
     WHERE o.order_id = p_order_id
     FOR UPDATE;

    UPDATE proc_lab.orders
       SET order_status = p_new_status,
           updated_at   = clock_timestamp()
     WHERE order_id = p_order_id
       AND order_status IS DISTINCT FROM p_new_status;

    GET DIAGNOSTICS v_row_count = ROW_COUNT;

    RAISE NOTICE '기존상태=%, 요청상태=%, 변경행수=%',
                 v_old_status, p_new_status, v_row_count;

    IF v_row_count = 0 THEN
        p_message := format(
            '주문 %s는 이미 %s 상태입니다.',
            p_order_id,
            p_new_status
        );
        RETURN;
    END IF;

    INSERT INTO proc_lab.order_status_history
        (order_id, old_status, new_status)
    VALUES
        (p_order_id, v_old_status, p_new_status);

    p_message := format(
        '주문 %s: %s → %s 변경 완료',
        p_order_id,
        v_old_status,
        p_new_status
    );
END;
$procedure$;

/*
  멱등성
    같은 요청을 여러 번 실행해도 최종 결과가 한 번 실행한 것과 같다.

  이 예제에서는
    order_status IS DISTINCT FROM p_new_status
  조건으로 이미 같은 상태인 행을 다시 UPDATE하지 않고,
  이력도 중복 INSERT하지 않는다.
*/
BEGIN;

CALL proc_lab.pr_change_order_status(3, 'PAID', NULL);
CALL proc_lab.pr_change_order_status(3, 'PAID', NULL);

SELECT *
FROM proc_lab.order_status_history
WHERE order_id = 3
ORDER BY history_id;

ROLLBACK;


-- ============================================================================
-- 05. [중급] UPSERT — 이메일 기준 고객 등록 또는 수정
-- ============================================================================

CREATE OR REPLACE PROCEDURE proc_lab.pr_upsert_customer(
    IN    p_customer_name varchar,
    IN    p_email         varchar,
    INOUT p_customer_id   bigint DEFAULT NULL
)
LANGUAGE plpgsql
AS $procedure$
BEGIN
    IF nullif(btrim(p_customer_name), '') IS NULL THEN
        RAISE EXCEPTION '고객명은 필수입니다.'
            USING ERRCODE = '23502';
    END IF;

    IF p_email IS NULL
       OR p_email !~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$' THEN
        RAISE EXCEPTION '올바른 이메일 형식이 아닙니다: %', p_email
            USING ERRCODE = '22023';
    END IF;

    /*
      email_normalized는 lower(email)로 생성되는 UNIQUE 컬럼이다.
      대소문자가 달라도 같은 이메일이면 충돌하므로 UPDATE가 실행된다.
    */
    INSERT INTO proc_lab.customers
        (customer_name, email)
    VALUES
        (btrim(p_customer_name), btrim(p_email))
    ON CONFLICT (email_normalized)
    DO UPDATE
       SET customer_name = EXCLUDED.customer_name,
           email         = EXCLUDED.email,
           active        = true
    RETURNING customer_id INTO p_customer_id;

    RAISE NOTICE '저장된 customer_id=%', p_customer_id;
END;
$procedure$;

BEGIN;

CALL proc_lab.pr_upsert_customer(
    '한지민',
    'jimin@example.com',
    NULL
);

CALL proc_lab.pr_upsert_customer(
    '한지민(수정)',
    'JIMIN@example.com',
    NULL
);

SELECT customer_id, customer_name, email, email_normalized
FROM proc_lab.customers
WHERE email_normalized = 'jimin@example.com';

ROLLBACK;


-- ============================================================================
-- 06. [중급] 반복문 — 행마다 다른 처리가 필요한 경우
-- ============================================================================

CREATE OR REPLACE PROCEDURE proc_lab.pr_restock_low_inventory(
    IN  p_threshold integer,
    IN  p_add_qty   integer,
    OUT p_affected  integer
)
LANGUAGE plpgsql
AS $procedure$
DECLARE
    v_product proc_lab.products%ROWTYPE;
BEGIN
    IF p_threshold < 0 OR p_add_qty <= 0 THEN
        RAISE EXCEPTION
            '기준 재고는 0 이상, 추가 수량은 1 이상이어야 합니다.'
            USING ERRCODE = '22003';
    END IF;

    p_affected := 0;

    FOR v_product IN
        SELECT p.*
        FROM proc_lab.products AS p
        WHERE p.active
          AND p.stock_qty < p_threshold
        ORDER BY p.product_id
        FOR UPDATE
    LOOP
        UPDATE proc_lab.products
           SET stock_qty = stock_qty + p_add_qty
         WHERE product_id = v_product.product_id;

        p_affected := p_affected + 1;

        RAISE NOTICE '% 재고 보충: % → %',
                     v_product.product_name,
                     v_product.stock_qty,
                     v_product.stock_qty + p_add_qty;
    END LOOP;
END;
$procedure$;

BEGIN;

CALL proc_lab.pr_restock_low_inventory(70, 20, NULL);

SELECT product_id, product_name, stock_qty
FROM proc_lab.products
ORDER BY product_id;

ROLLBACK;

/*
  모든 행에 똑같은 계산을 적용하기만 한다면 반복문보다 다음 한 문장이 좋다.

  UPDATE proc_lab.products
     SET stock_qty = stock_qty + 20
   WHERE active
     AND stock_qty < 70;

  반복문이 적합한 경우
    - 행마다 서로 다른 판단이나 외부 호출이 필요
    - 행별 NOTICE 또는 개별 오류 처리가 필요

  집합 기반 처리가 적합한 경우
    - 동일 조건과 동일 계산을 여러 행에 적용
    - 대량 데이터 성능이 중요
*/


-- ============================================================================
-- 07. [중급] 배열 + ANY — 반복문 없는 일괄 상태 변경
-- ============================================================================

CREATE OR REPLACE PROCEDURE proc_lab.pr_bulk_change_status(
    IN  p_order_ids  bigint[],
    IN  p_new_status text,
    OUT p_affected   integer
)
LANGUAGE plpgsql
AS $procedure$
BEGIN
    IF p_order_ids IS NULL OR cardinality(p_order_ids) = 0 THEN
        RAISE EXCEPTION '주문번호 배열은 비어 있을 수 없습니다.'
            USING ERRCODE = '22023';
    END IF;

    IF p_new_status NOT IN
       ('PENDING', 'PAID', 'SHIPPING', 'COMPLETED', 'CANCELLED') THEN
        RAISE EXCEPTION '허용되지 않은 주문 상태입니다: %', p_new_status
            USING ERRCODE = '22023';
    END IF;

    /*
      target에서 변경 전 상태를 보관하고 행을 잠근다.
      updated에서 한 번의 UPDATE로 상태를 변경한다.
      history_insert에서 변경된 행만 이력으로 남긴다.
    */
    WITH target AS (
        SELECT o.order_id,
               o.order_status AS old_status
        FROM proc_lab.orders AS o
        WHERE o.order_id = ANY(p_order_ids)
          AND o.order_status IS DISTINCT FROM p_new_status
        FOR UPDATE
    ),
    updated AS (
        UPDATE proc_lab.orders AS o
           SET order_status = p_new_status,
               updated_at   = clock_timestamp()
          FROM target AS t
         WHERE o.order_id = t.order_id
        RETURNING o.order_id,
                  t.old_status,
                  o.order_status AS new_status
    ),
    history_insert AS (
        INSERT INTO proc_lab.order_status_history
            (order_id, old_status, new_status)
        SELECT u.order_id, u.old_status, u.new_status
        FROM updated AS u
        RETURNING 1
    )
    SELECT count(*)::integer
      INTO p_affected
      FROM updated;

    RAISE NOTICE '상태 일괄 변경 완료: 처리건수=%', p_affected;
END;
$procedure$;

BEGIN;

CALL proc_lab.pr_bulk_change_status(
    ARRAY[1, 3, 5]::bigint[],
    'PAID',
    NULL
);

SELECT order_id, order_status, updated_at
FROM proc_lab.orders
WHERE order_id = ANY(ARRAY[1, 3, 5]::bigint[])
ORDER BY order_id;

ROLLBACK;


-- ============================================================================
-- 08. [고급] 배치 키를 이용한 멱등성 할인
-- ============================================================================

CREATE TABLE IF NOT EXISTS proc_lab.bulk_discount_runs (
    batch_key        text PRIMARY KEY,
    discount_percent numeric NOT NULL,
    min_price        numeric NOT NULL,
    affected_count   integer NOT NULL DEFAULT 0,
    run_status       text NOT NULL DEFAULT 'PROCESSING',
    started_at       timestamptz NOT NULL DEFAULT clock_timestamp(),
    completed_at     timestamptz,
    CONSTRAINT ck_bulk_discount_percent
        CHECK (discount_percent > 0 AND discount_percent <= 50),
    CONSTRAINT ck_bulk_discount_min_price
        CHECK (min_price >= 0),
    CONSTRAINT ck_bulk_discount_status
        CHECK (run_status IN ('PROCESSING', 'COMPLETED'))
);

CREATE OR REPLACE PROCEDURE proc_lab.pr_bulk_discount_once(
    IN  p_batch_key        text,
    IN  p_discount_percent numeric,
    IN  p_min_price        numeric,
    OUT p_affected         integer,
    OUT p_message          text
)
LANGUAGE plpgsql
AS $procedure$
DECLARE
    v_inserted integer;
    v_run      proc_lab.bulk_discount_runs%ROWTYPE;
BEGIN
    IF nullif(btrim(p_batch_key), '') IS NULL THEN
        RAISE EXCEPTION '배치 키는 필수입니다.'
            USING ERRCODE = '23502';
    END IF;

    IF p_discount_percent <= 0 OR p_discount_percent > 50 THEN
        RAISE EXCEPTION '배치 할인율은 0 초과 50 이하이어야 합니다.'
            USING ERRCODE = '22023';
    END IF;

    IF p_min_price < 0 THEN
        RAISE EXCEPTION '최소 가격은 0 이상이어야 합니다.'
            USING ERRCODE = '22003';
    END IF;

    /*
      배치 키를 먼저 INSERT한다.
      같은 키가 이미 있으면 INSERT하지 않는다.
    */
    INSERT INTO proc_lab.bulk_discount_runs
        (batch_key, discount_percent, min_price)
    VALUES
        (btrim(p_batch_key), p_discount_percent, p_min_price)
    ON CONFLICT (batch_key) DO NOTHING;

    GET DIAGNOSTICS v_inserted = ROW_COUNT;

    IF v_inserted = 0 THEN
        /*
          기존 실행을 잠그고 결과를 확인한다.
          동시 요청이 같은 키를 사용하면 선행 트랜잭션이 끝날 때까지 대기한 후
          완료 결과를 재사용한다.
        */
        SELECT r.*
          INTO STRICT v_run
          FROM proc_lab.bulk_discount_runs AS r
         WHERE r.batch_key = btrim(p_batch_key)
         FOR UPDATE;

        IF v_run.discount_percent IS DISTINCT FROM p_discount_percent
           OR v_run.min_price IS DISTINCT FROM p_min_price THEN
            RAISE EXCEPTION
                '같은 배치 키에 다른 조건을 사용할 수 없습니다: %',
                p_batch_key
                USING ERRCODE = '22023',
                      DETAIL = format(
                          '기존 할인율=%s, 기존 최소가격=%s',
                          v_run.discount_percent,
                          v_run.min_price
                      );
        END IF;

        IF v_run.run_status = 'COMPLETED' THEN
            p_affected := v_run.affected_count;
            p_message := format(
                '이미 완료된 배치 %s의 결과를 반환합니다.',
                p_batch_key
            );

            RAISE NOTICE '% 처리건수=%', p_message, p_affected;
            RETURN;
        END IF;

        RAISE EXCEPTION '배치 %가 아직 처리 중입니다.', p_batch_key
            USING ERRCODE = '55006';
    END IF;

    UPDATE proc_lab.products
       SET unit_price = round(
           unit_price * (1 - p_discount_percent / 100.0),
           2
       )
     WHERE active
       AND unit_price >= p_min_price;

    GET DIAGNOSTICS p_affected = ROW_COUNT;

    UPDATE proc_lab.bulk_discount_runs
       SET affected_count = p_affected,
           run_status     = 'COMPLETED',
           completed_at   = clock_timestamp()
     WHERE batch_key = btrim(p_batch_key);

    p_message := format('배치 %s 할인 완료', p_batch_key);

    /*
      %는 값 자리 표시자이므로 숫자와 실제 퍼센트 기호를 함께 출력하려면
      혼동을 피하도록 "퍼센트"라는 단어를 붙여 표현할 수 있다.
    */
    RAISE NOTICE
        '배치 % 할인 완료: 할인율=%퍼센트, 최소가격=%, 처리건수=%',
        p_batch_key,
        p_discount_percent,
        p_min_price,
        p_affected;
END;
$procedure$;

BEGIN;

-- 첫 호출: 실제 할인 수행
CALL proc_lab.pr_bulk_discount_once(
    'EVENT-2026-07-A',
    5,
    100000,
    NULL,
    NULL
);

-- 같은 키와 같은 조건: 다시 할인하지 않고 기존 결과 반환
CALL proc_lab.pr_bulk_discount_once(
    'EVENT-2026-07-A',
    5,
    100000,
    NULL,
    NULL
);

SELECT *
FROM proc_lab.bulk_discount_runs
WHERE batch_key = 'EVENT-2026-07-A';

ROLLBACK;


-- ============================================================================
-- 09. [고급] 주문 취소 — 중첩 블록 + 오류 로그 + 오류 문맥
-- ============================================================================

CREATE OR REPLACE PROCEDURE proc_lab.pr_cancel_order(
    IN  p_order_id bigint,
    OUT p_message  text
)
LANGUAGE plpgsql
AS $procedure$
DECLARE
    v_status  proc_lab.orders.order_status%TYPE;
    v_state   text;
    v_message text;
    v_detail  text;
    v_context text;
BEGIN
    /*
      내부 BEGIN ... EXCEPTION 블록은 서브트랜잭션처럼 동작한다.
      내부에서 오류가 나면 내부 블록의 재고/주문/이력 변경은 취소되고,
      EXCEPTION 절에서 기록한 오류 로그는 바깥 블록에 남는다.
    */
    BEGIN
        SELECT o.order_status
          INTO STRICT v_status
          FROM proc_lab.orders AS o
         WHERE o.order_id = p_order_id
         FOR UPDATE;

        IF v_status IN ('SHIPPING', 'COMPLETED') THEN
            RAISE EXCEPTION
                '배송 중 또는 완료 주문은 취소할 수 없습니다.'
                USING ERRCODE = 'P0001',
                      DETAIL = format(
                          'order_id=%s, status=%s',
                          p_order_id,
                          v_status
                      );
        END IF;

        -- 같은 취소 요청을 다시 실행해도 재고를 두 번 복구하지 않는다.
        IF v_status = 'CANCELLED' THEN
            p_message := format(
                '주문 %s는 이미 취소되었습니다.',
                p_order_id
            );
            RETURN;
        END IF;

        UPDATE proc_lab.products AS p
           SET stock_qty = p.stock_qty + oi.quantity
          FROM proc_lab.order_items AS oi
         WHERE oi.order_id = p_order_id
           AND oi.product_id = p.product_id;

        UPDATE proc_lab.orders
           SET order_status = 'CANCELLED',
               updated_at   = clock_timestamp()
         WHERE order_id = p_order_id;

        INSERT INTO proc_lab.order_status_history
            (order_id, old_status, new_status)
        VALUES
            (p_order_id, v_status, 'CANCELLED');

        p_message := format(
            '주문 %s 취소 및 재고 복구 완료',
            p_order_id
        );

    EXCEPTION
        WHEN OTHERS THEN
            GET STACKED DIAGNOSTICS
                v_state   = RETURNED_SQLSTATE,
                v_message = MESSAGE_TEXT,
                v_detail  = PG_EXCEPTION_DETAIL,
                v_context = PG_EXCEPTION_CONTEXT;

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
                    'pr_cancel_order',
                    v_state,
                    v_message,
                    v_detail,
                    v_context
                );

            /*
              여기서는 오류를 재발생시키지 않는다.
              따라서 같은 트랜잭션 안에서 오류 로그가 보존되고
              호출자에게 OUT 메시지로 실패 내용을 전달한다.
            */
            p_message := format(
                '취소 실패 [%s] %s',
                v_state,
                v_message
            );
    END;
END;
$procedure$;

BEGIN;

-- order_id=5는 샘플 기준 PENDING이므로 정상 취소
CALL proc_lab.pr_cancel_order(5, NULL);

-- order_id=2는 샘플 기준 SHIPPING이므로 실패하고 오류 로그 기록
CALL proc_lab.pr_cancel_order(2, NULL);

-- 존재하지 않는 주문: SQLSTATE와 오류 문맥 기록
CALL proc_lab.pr_cancel_order(999999, NULL);

SELECT log_id,
       routine_name,
       error_state,
       error_message,
       error_detail,
       error_context
FROM proc_lab.routine_error_log
ORDER BY log_id DESC;

ROLLBACK;


-- ============================================================================
-- 10. [고급] SECURITY DEFINER 프로시저
-- ============================================================================

CREATE OR REPLACE PROCEDURE proc_lab.pr_set_customer_active(
    IN  p_customer_id bigint,
    IN  p_active      boolean,
    OUT p_message     text
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, proc_lab, pg_temp
AS $procedure$
DECLARE
    v_affected integer;
BEGIN
    UPDATE proc_lab.customers
       SET active = p_active
     WHERE customer_id = p_customer_id;

    GET DIAGNOSTICS v_affected = ROW_COUNT;

    IF v_affected = 0 THEN
        RAISE EXCEPTION '고객번호 %를 찾을 수 없습니다.', p_customer_id
            USING ERRCODE = 'P0002';
    END IF;

    p_message := format(
        '고객 %s의 active를 %s로 변경했습니다.',
        p_customer_id,
        p_active
    );
END;
$procedure$;

/*
  SECURITY DEFINER는 프로시저 소유자의 권한으로 실행된다.
  반드시 search_path를 신뢰 가능한 스키마로 고정하고,
  PUBLIC의 기본 EXECUTE 권한을 제거한 뒤 필요한 역할에만 부여한다.

  주의
    SECURITY DEFINER 프로시저는 COMMIT/ROLLBACK을 실행할 수 없다.
*/
REVOKE ALL
ON PROCEDURE proc_lab.pr_set_customer_active(bigint, boolean)
FROM PUBLIC;

-- 운영 역할이 존재할 때만 역할명을 바꿔 실행한다.
-- GRANT EXECUTE
-- ON PROCEDURE proc_lab.pr_set_customer_active(bigint, boolean)
-- TO app_user;

BEGIN;
CALL proc_lab.pr_set_customer_active(1, false, NULL);
SELECT customer_id, customer_name, active
FROM proc_lab.customers
WHERE customer_id = 1;
ROLLBACK;


-- ============================================================================
-- 11. [고급] 프로시저 내부 COMMIT / ROLLBACK
-- ============================================================================

CREATE OR REPLACE PROCEDURE proc_lab.pr_transaction_demo(
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

    INSERT INTO proc_lab.transaction_demo_log(message)
    VALUES ('ROLLBACK 이후 새 트랜잭션에서 입력된 행');
END;
$procedure$;

/*
  아래 CALL은 자동 실행하지 않는다.
  DBeaver에서 Auto-commit을 켜고 CALL 한 문장만 선택하여 실행한다.

  CALL proc_lab.pr_transaction_demo(
      '첫 번째 INSERT: COMMIT으로 보존',
      '두 번째 INSERT: ROLLBACK으로 취소'
  );

  SELECT *
  FROM proc_lab.transaction_demo_log
  ORDER BY log_id;

  예상 결과
    - 첫 번째 메시지              : 남음
    - 두 번째 메시지              : 취소되어 없음
    - ROLLBACK 이후 입력된 메시지 : 마지막 새 트랜잭션에 입력됨

  실행 가능한 주요 조건
    1. CALL이 최상위 명령이어야 한다.
    2. 명시적 BEGIN ... COMMIT 블록 안에서 호출하면 안 된다.
    3. 함수 내부에서 간접 호출된 프로시저이면 안 된다.
    4. SECURITY DEFINER 프로시저이면 안 된다.
    5. CREATE PROCEDURE의 SET 절이 붙은 프로시저이면 안 된다.
    6. EXCEPTION 블록 내부에서는 트랜잭션을 종료할 수 없다.

  명시적 트랜잭션 안에서의 오류 확인 예
    BEGIN;
    CALL proc_lab.pr_transaction_demo('A', 'B');
    ROLLBACK;

  위 호출에서는 일반적으로 "invalid transaction termination" 오류가 발생한다.
*/


-- ============================================================================
-- 12. 프로시저 메타데이터와 정의 조회
-- ============================================================================

SELECT n.nspname AS procedure_schema,
       p.proname AS procedure_name,
       pg_catalog.pg_get_function_identity_arguments(p.oid)
           AS identity_arguments,
       l.lanname AS language_name,
       p.prosecdef AS security_definer,
       p.proconfig AS configuration,
       pg_catalog.pg_get_userbyid(p.proowner) AS owner_name
FROM pg_catalog.pg_proc AS p
JOIN pg_catalog.pg_namespace AS n
  ON n.oid = p.pronamespace
JOIN pg_catalog.pg_language AS l
  ON l.oid = p.prolang
WHERE n.nspname = 'proc_lab'
  AND p.prokind = 'p'
ORDER BY p.proname,
         pg_catalog.pg_get_function_identity_arguments(p.oid);

-- 프로시저 정의문 확인
SELECT pg_catalog.pg_get_functiondef(
           'proc_lab.pr_change_order_status(bigint,text)'::regprocedure
       );


-- ============================================================================
-- 13. 프로시저 관리: 권한, 설명, 삭제
-- ============================================================================

COMMENT ON PROCEDURE
    proc_lab.pr_change_order_status(bigint, text)
IS '주문 상태를 멱등하게 변경하고 변경 이력을 기록하는 프로시저';

/*
  권한
    GRANT EXECUTE ON PROCEDURE 프로시저명(입력자료형...) TO 역할명;
    REVOKE EXECUTE ON PROCEDURE 프로시저명(입력자료형...) FROM 역할명;

  삭제
    DROP PROCEDURE 프로시저명(입력자료형...);

  OUT 매개변수 자료형은 객체 식별용 입력 자료형 목록에 넣지 않는다.
  IN과 INOUT 매개변수 자료형으로 프로시저를 식별한다.
*/

-- 필요할 때만 주석을 해제한다.
-- DROP PROCEDURE proc_lab.pr_change_order_status(bigint, text);


-- ============================================================================
-- 14. 실무 설계 점검
-- ============================================================================

/*
  1. 트랜잭션 경계
     - 일반적으로 Spring의 @Transactional 등 애플리케이션이 관리한다.
     - DB 프로시저 내부 COMMIT은 특별한 배치·운영 작업에 제한한다.

  2. 행 잠금
     - 같은 주문이나 상품을 동시에 변경하면 SELECT ... FOR UPDATE를 검토한다.
     - 잠금 순서를 통일해 교착상태 가능성을 줄인다.

  3. 멱등성
     - 상태가 이미 같으면 UPDATE/이력을 반복하지 않는다.
     - 외부 배치 요청은 UNIQUE 배치 키로 한 번만 적용되게 한다.

  4. 반복문과 집합 기반 처리
     - 동일 변경은 UPDATE 한 문장으로 처리한다.
     - 행별 별도 판단이 꼭 필요할 때 반복문을 사용한다.

  5. 예외 처리
     - 오류를 숨길지 재발생시킬지 정책을 명확히 한다.
     - 같은 트랜잭션에서 오류를 재발생시키면 오류 로그도 함께 롤백된다.

  6. 애플리케이션/JPA 연동
     - 일반 CRUD는 Repository/JPQL을 우선한다.
     - 여러 테이블의 원자적 업무, 배치, DB 공유 로직에 프로시저를 선별 적용한다.
     - 프로시저나 벌크 DML 뒤에는 JPA 영속성 컨텍스트가 오래된 값을 가질 수
       있으므로 clear/refresh 정책을 검토한다.
*/


-- ============================================================================
-- 15. 최종 확인 및 복습
-- ============================================================================

SELECT p.proname AS procedure_name,
       pg_catalog.pg_get_function_identity_arguments(p.oid)
           AS identity_arguments,
       p.prosecdef AS security_definer
FROM pg_catalog.pg_proc AS p
JOIN pg_catalog.pg_namespace AS n
  ON n.oid = p.pronamespace
WHERE n.nspname = 'proc_lab'
  AND p.prokind = 'p'
ORDER BY p.proname;

/*
  복습 질문
    1. OUT 매개변수 자리에는 CALL에서 왜 NULL을 전달하는가?
    2. GET DIAGNOSTICS ... ROW_COUNT는 언제 사용하는가?
    3. 같은 상태 변경 요청에서 이력을 중복 입력하지 않는 방법은 무엇인가?
    4. UPSERT가 애플리케이션의 SELECT 후 INSERT 방식보다 안전한 이유는?
    5. 배열과 ANY를 이용하면 반복문을 어떻게 줄일 수 있는가?
    6. 배치 키가 중복 할인을 어떻게 방지하는가?
    7. 오류를 재발생시키면 같은 트랜잭션의 오류 로그는 어떻게 되는가?
    8. 프로시저 내부 COMMIT/ROLLBACK이 허용되지 않는 경우는 무엇인가?
    9. SECURITY DEFINER에서 search_path를 왜 고정해야 하는가?
*/

