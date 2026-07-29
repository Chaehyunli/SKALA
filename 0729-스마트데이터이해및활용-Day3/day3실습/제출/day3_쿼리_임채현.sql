/*
===============================================================================
첨부파일 먼저 실행 : PostgreSQL_day03_tuning_샘플_스키마_DDL_DML.sql
===============================================================================
===============================================================================
문제 풀이 공통 체크리스트
===============================================================================
채점은 실행시간의 절대값보다 다음 항목을 중심합니다.

	- 원래 쿼리와 결과가 동일한가?
	- 병목을 정확히 진단했는가?
	- 인덱스 컬럼 순서와 조건이 합리적인가?
	- 실제로 실행계획이 어떻게 변했는가?
	- 인덱스가 사용되지 않더라도 그 이유를 설명했는가?
===============================================================================
*/


-- ############################################################################
-- 문제 1. 기본 키 검색의 실행 계획 읽기 <<< 문제 해결 방법 제시문항(10점 보너스)
-- ############################################################################
/*
[문제]
 사원번호가 100인 사원을 검색하고 실행 계획을 해석한다.
 이미 빠른 쿼리도 튜닝 대상인지 판단한다.

[수행 과제]
 - 실제 사용된 Scan 노드를 확인한다.
 - estimated rows와 actual rows를 비교한다.
 - Buffers와 Execution Time을 기록한다.
 - 추가 인덱스가 필요한지 근거와 함께 답한다.
*/
-- 개선 전
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, TIMING OFF)
SELECT *
FROM employees
WHERE employee_id = 100;

-- 	QUERY PLAN
--Index Scan using employees_pkey on day03_tuning.employees  (cost=0.29..8.31 rows=1 width=111) (actual rows=1 loops=1)
--  Output: employee_id, employee_no, first_name, last_name, full_name, email, department_id, job_code, branch_code, hire_date, salary, employment_status, phone, created_at
--  Index Cond: (employees.employee_id = 100)
--  Buffers: shared hit=6
--Planning:
--  Buffers: shared hit=53
--Planning Time: 0.868 ms
--Execution Time: 0.056 ms


-- 개선안 <<< 이부분은 직접 작성합니다.(쿼리 재작성 또는 인덱스 생성 SQL)
--이 문제의 정답은 "계획을 확인하되 별도 튜닝하지 않는다"이다.


-- 개선 후
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, TIMING OFF)
SELECT *
FROM employees
WHERE employee_id = 100;

-- 	QUERY PLAN
--Index Scan using employees_pkey on day03_tuning.employees  (cost=0.29..8.31 rows=1 width=111) (actual rows=1 loops=1)
--  Output: employee_id, employee_no, first_name, last_name, full_name, email, department_id, job_code, branch_code, hire_date, salary, employment_status, phone, created_at
--  Index Cond: (employees.employee_id = 100)
--  Buffers: shared hit=6
--Planning:
--  Buffers: shared hit=53
--Planning Time: 0.868 ms
--Execution Time: 0.056 ms

/*
-- [개선 결과 해석]
-- 변경된 Plan Node:없음
-- Buffers 변화:없음
-- Execution Time 변화: 미미함
-- 개선된 이유:
 - employee_id는 PRIMARY KEY이므로 자동으로 B-tree 인덱스가 생성된다.
 - 일반적으로 Index Scan 또는 동등한 인덱스 기반 계획이 선택된다.
 - 단건 조회를 위해 employee_id에 중복 인덱스를 추가하는 것은 쓰기 비용과 저장 공간만 늘린다.
 - 따라서 이 문제의 정답은 "계획을 확인하되 별도 튜닝하지 않는다"이다.
 - Seq Scan이 보인다면 테이블 크기, 통계, 설정을 먼저 확인하고 무조건 인덱스를 추가하지 않는다.
*/

--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- CREATE UNIQUE INDEX employees_pkey ON day03_tuning.employees USING btree (employee_id)

-- ############################################################################
-- 문제 2. 함수가 적용된 이메일 검색: 표현식 인덱스 <<< 문제 해결 방법 제시문항(10점 보너스)
-- ############################################################################
/*
[문제]
 대소문자 구분 없이 user1234@corp.com을 검색한다.
 WHERE절의 lower(email) 때문에 일반 email 인덱스만으로는 같은 표현식을 바로 찾기 어렵다.

[개선 목표]
 쿼리의 검색 표현식과 동일한 lower(email) 표현식 인덱스를 설계한다.
*/


-- 개선 전
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, email
FROM employees
WHERE lower(email) = 'user1234@corp.com';

-- 	QUERY PLAN
--Seq Scan on employees  (cost=0.00..1658.00 rows=250 width=46) (actual rows=1 loops=1)
--  Filter: (lower((email)::text) = 'user1234@corp.com'::text)
--  Rows Removed by Filter: 49999
--  Buffers: shared hit=908
--Planning:
--  Buffers: shared hit=6 dirtied=1
--Planning Time: 0.363 ms
--Execution Time: 20.343 ms

-- 개선안 <<< 이부분은 직접 작성합니다.
DROP INDEX IF EXISTS idx_employees_lower_email;

CREATE INDEX idx_employees_lower_email
    ON employees (lower(email));

ANALYZE employees;

-- 개선 후
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, email
FROM employees
WHERE lower(email) = 'user1234@corp.com';

-- 	QUERY PLAN
--Index Scan using idx_employees_lower_email on employees  (cost=0.41..8.43 rows=1 width=46) (actual rows=1 loops=1)
--  Index Cond: (lower((email)::text) = 'user1234@corp.com'::text)
--  Buffers: shared hit=1 read=3
--Planning:
--  Buffers: shared hit=31 read=1
--Planning Time: 0.569 ms
--Execution Time: 0.056 ms


/*
-- [개선 결과 해석]
-- 변경된 Plan Node:Seq Scan -> Index Scan
-- Buffers 변화:shared hit=908 -> shared hit=1 read=3
-- Execution Time 변화: 20.343 ms -> 0.056 ms
-- 개선된 이유:
 - 일반 인덱스 ON employees(email)는 lower(email) 검색식과 구조가 다르다.
 - ON employees(lower(email))은 WHERE절 표현식과 일치하므로 인덱스 조건으로 사용할 수 있다.
 - 개선 전 Seq Scan과 개선 후 Index Scan의 Buffers, Execution Time을 비교한다.
 - 저장 시 이메일을 항상 소문자로 정규화한다면 email = '...'과 일반 인덱스도 대안이다.
*/
--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- CREATE INDEX idx_employees_lower_email ON day03_tuning.employees USING btree (lower((email)::text))

-- ############################################################################
-- 문제 3. 접미사 LIKE 검색: pg_trgm GIN 인덱스
-- ############################################################################
/*
[문제]
 gmail.com 도메인을 사용하는 사원을 찾는다.
 '%@gmail.com'은 앞부분이 와일드카드이므로 일반 B-tree의 정렬 순서를 활용하기 어렵다.

[개선 목표]
 pg_trgm과 GIN 인덱스를 사용해 포함/접미사 검색을 개선한다.
*/


-- 개선 전
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, email
FROM employees
WHERE email LIKE '%@gmail.com';

-- 	QUERY PLAN
-- Seq Scan on employees  (cost=0.00..1533.00 rows=505 width=46) (actual rows=500 loops=1)
--  Filter: ((email)::text ~~ '%@gmail.com'::text)
--  Rows Removed by Filter: 49500
--  Buffers: shared hit=908
--Planning Time: 0.093 ms
--Execution Time: 8.052 ms

-- 개선안 - 이부분은 직접작성합니다.

DROP INDEX IF EXISTS idx_employees_email_trgm;
CREATE INDEX idx_employees_email_trgm
    ON employees USING gin (email gin_trgm_ops);

ANALYZE employees;

-- 개선 후
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, email
FROM employees
WHERE email LIKE '%@gmail.com';

--  QUERY PLAN
-- Bitmap Heap Scan on employees  (cost=96.25..1059.01 rows=1010 width=46) (actual rows=500 loops=1)
--  Recheck Cond: ((email)::text ~~ '%@gmail.com'::text)
--  Heap Blocks: exact=500
--  Buffers: shared hit=577
--  ->  Bitmap Index Scan on idx_employees_email_trgm  (cost=0.00..96.00 rows=1010 width=0) (actual rows=500 loops=1)
--        Index Cond: ((email)::text ~~ '%@gmail.com'::text)
--        Buffers: shared hit=77
--Planning:
--  Buffers: shared hit=1
--Planning Time: 0.100 ms
--Execution Time: 1.635 ms

/*
-- [개선 결과 해석]
-- 변경된 Plan Node: Seq Scan -> Bitmap Heap Scan
-- Buffers 변화: shared hit=908 -> shared hit=577
-- Execution Time 변화: 8.052 ms -> 1.635 ms
-- 개선된 이유:
 - '%@gmail.com'은 선두 와일드카드라 B-tree의 정렬 순서를 활용할 수 없어 Seq Scan이 선택된다.
 - pg_trgm은 문자열을 3글자 단위(trigram)로 쪼개 인덱싱하므로, 접미사/포함 검색에서도 후보 행을 좁힐 수 있다.
 - gin_trgm_ops GIN 인덱스는 LIKE 패턴의 trigram과 일치하는 행만 Bitmap으로 추려 Heap 접근을 줄인다.
 - 다만 gmail 사원 비율이 낮아(1% 수준) 선택도가 좋으므로 인덱스 효과가 크다. 매칭 행이 많으면 옵티마이저가 Seq Scan을 다시 택할 수도 있다.
 - Buffers 감소폭(908->577)이 상대적으로 작은 이유는 gmail 사원이 테이블 전체에 흩어져 있어(Heap Blocks exact=500) Heap 접근 블록이 크게 줄지 않기 때문이다.
 - 반면 Execution Time은 필터 대상이 5만 건에서 500건 후보로 좁혀져 약 5배 개선되었다.
*/

--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- CREATE INDEX idx_employees_email_trgm ON day03_tuning.employees USING gin (email gin_trgm_ops)

-- ############################################################################
-- 문제 4. 필터 + ORDER BY + LIMIT: 부분 정렬 인덱스
-- ############################################################################
/*
[문제]
 재직 중이며 최근 365일 안에 입사한 사원을 연봉순으로 상위 100명 조회한다.

[개선 목표]
 - 재직자만 포함하는 부분 인덱스를 검토한다.
 - ORDER BY salary DESC와 LIMIT 100의 조기 종료를 유도한다.
*/


-- 개선 전
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT
    employee_id,
    employee_no,
    full_name,
    hire_date,
    salary
FROM employees
WHERE employment_status = 'ACTIVE'
  AND hire_date >= current_date - 365
ORDER BY salary DESC
LIMIT 100;

--  QUERY PLAN
-- Limit  (cost=2081.06..2081.31 rows=100 width=39) (actual rows=100 loops=1)
--  Buffers: shared hit=908
--  ->  Sort  (cost=2081.06..2092.38 rows=4528 width=39) (actual rows=100 loops=1)
--        Sort Key: salary DESC
--        Sort Method: top-N heapsort  Memory: 39kB
--        Buffers: shared hit=908
--        ->  Seq Scan on employees  (cost=0.00..1908.00 rows=4528 width=39) (actual rows=4411 loops=1)
--              Filter: (((employment_status)::text = 'ACTIVE'::text) AND (hire_date >= (CURRENT_DATE - 365)))
--              Rows Removed by Filter: 45589
--              Buffers: shared hit=908
--Planning Time: 0.092 ms
--Execution Time: 7.703 ms

-- 개선안 - 이부분은 직접작성합니다.
DROP INDEX IF EXISTS idx_employees_active_salary_hire;
CREATE INDEX idx_employees_active_salary_hire
    ON employees (salary DESC, hire_date)
    INCLUDE (employee_id, employee_no, full_name)
    WHERE employment_status = 'ACTIVE';

-- 개선 후
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT
    employee_id,
    employee_no,
    full_name,
    hire_date,
    salary
FROM employees
WHERE employment_status = 'ACTIVE'
  AND hire_date >= current_date - 365
ORDER BY salary DESC
LIMIT 100;

--  QUERY PLAN
-- Limit  (cost=0.42..40.94 rows=100 width=39) (actual rows=100 loops=1)
--  Buffers: shared hit=11
--  ->  Index Only Scan using idx_employees_active_salary_hire on employees  (cost=0.42..1835.21 rows=4528 width=39) (actual rows=100 loops=1)
--        Index Cond: (hire_date >= (CURRENT_DATE - 365))
--        Heap Fetches: 0
--        Buffers: shared hit=11
--Planning Time: 0.147 ms
--Execution Time: 0.104 ms

/*
-- [개선 결과 해석]
 - 변경된 Plan Node: Seq Scan + Sort(top-N heapsort) -> Index Only Scan (Sort 제거)
 - Buffers 변화: shared hit=908 -> shared hit=11
 - Execution Time 변화: 7.703 ms -> 0.104 ms
 - 개선된 이유:
 - WHERE employment_status='ACTIVE' 부분 인덱스라 재직자(4,528건)만 인덱스에 담겨 스캔 대상 자체가 작다.
 - 선두 컬럼을 salary DESC로 두어 인덱스가 이미 연봉 내림차순으로 정렬돼 있으므로 별도 Sort 노드가 사라지고, LIMIT 100에서 앞 100건만 읽고 조기 종료한다(Sort Method: top-N heapsort 제거).
 - INCLUDE로 출력 컬럼(employee_id, employee_no, full_name)과 hire_date를 인덱스에 포함해 Heap Fetches: 0인 Index Only Scan이 되어 테이블 접근이 사라졌다(Buffers 908 -> 11).
 - 그 결과 5만 건 전체 스캔 후 정렬하던 방식이, 정렬된 인덱스 선두 100건만 읽는 방식으로 바뀌어 실행 시간이 약 74배 단축되었다.
 - 
 - 
*/
--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- CREATE INDEX idx_employees_active_salary_hire ON day03_tuning.employees USING btree (salary DESC, hire_date) INCLUDE (employee_id, employee_no, full_name) WHERE ((employment_status)::text = 'ACTIVE'::text)

-- ############################################################################
-- 문제 5. OR 조건과 IN 조건 비교
-- ############################################################################
/*
[문제]
 지점 코드가 B003, B004, B005 중 하나인 사원을 검색한다.
 OR 조건을 IN으로 바꾸었을 때 가독성과 실행 계획이 어떻게 달라지는지 확인한다.

[개선 목표]
 branch_code 인덱스를 추가하고 OR/IN 두 쿼리의 계획을 비교한다.
*/


-- 개선 전: 인덱스 없음
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, branch_code
FROM employees
WHERE branch_code = 'B003'
   OR branch_code = 'B004'
   OR branch_code = 'B005';

--  QUERY PLAN
-- Seq Scan on employees  (cost=0.00..1783.00 rows=1549 width=33) (actual rows=1500 loops=1)
--  Filter: (((branch_code)::text = 'B003'::text) OR ((branch_code)::text = 'B004'::text) OR ((branch_code)::text = 'B005'::text))
--  Rows Removed by Filter: 48500
--  Buffers: shared hit=908
--Planning:
--  Buffers: shared hit=5 dirtied=2
--Planning Time: 0.284 ms
--Execution Time: 12.682 ms

-- 개선안 <<< 이부분은 직접 작성합니다.
DROP INDEX IF EXISTS idx_employees_branch_code;
CREATE INDEX idx_employees_branch_code
    ON employees (branch_code);

ANALYZE employees;


-- 개선 후 A: OR
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, branch_code
FROM employees
WHERE branch_code = 'B003'
   OR branch_code = 'B004'
   OR branch_code = 'B005';

--  QUERY PLAN
-- Bitmap Heap Scan on employees  (cost=24.88..995.50 rows=1443 width=33) (actual rows=1500 loops=1)
--  Recheck Cond: (((branch_code)::text = 'B003'::text) OR ((branch_code)::text = 'B004'::text) OR ((branch_code)::text = 'B005'::text))
--  Heap Blocks: exact=506
--  Buffers: shared hit=510 read=4
--  ->  BitmapOr  (cost=24.88..24.88 rows=1457 width=0) (actual rows=0 loops=1)
--        Buffers: shared hit=4 read=4
--        ->  Bitmap Index Scan on idx_employees_branch_code  (cost=0.00..7.87 rows=477 width=0) (actual rows=500 loops=1)
--              Index Cond: ((branch_code)::text = 'B003'::text)
--              Buffers: shared read=3
--        ->  Bitmap Index Scan on idx_employees_branch_code  (cost=0.00..8.02 rows=497 width=0) (actual rows=500 loops=1)
--              Index Cond: ((branch_code)::text = 'B004'::text)
--              Buffers: shared hit=2
--        ->  Bitmap Index Scan on idx_employees_branch_code  (cost=0.00..7.91 rows=483 width=0) (actual rows=500 loops=1)
--              Index Cond: ((branch_code)::text = 'B005'::text)
--              Buffers: shared hit=2 read=1
-- Planning:
--   Buffers: shared hit=40 read=1
-- Planning Time: 0.430 ms
-- Execution Time: 0.860 ms

-- 개선 후 B: IN
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, branch_code
FROM employees
WHERE branch_code IN ('B003', 'B004', 'B005');

--  QUERY PLAN
-- Bitmap Heap Scan on employees  (cost=24.17..989.33 rows=1457 width=33) (actual rows=1500 loops=1)
--  Recheck Cond: ((branch_code)::text = ANY ('{B003,B004,B005}'::text[]))
--  Heap Blocks: exact=506
--  Buffers: shared hit=510
--  ->  Bitmap Index Scan on idx_employees_branch_code  (cost=0.00..23.80 rows=1457 width=0) (actual rows=1500 loops=1)
--        Index Cond: ((branch_code)::text = ANY ('{B003,B004,B005}'::text[]))
--        Buffers: shared hit=4
-- Planning:
--  Buffers: shared hit=3
-- Planning Time: 0.114 ms
-- Execution Time: 0.832 ms

/*
-- [개선 결과 해석]
-- 변경된 Plan Node: Seq Scan -> Bitmap Heap Scan (OR은 BitmapOr + Bitmap Index Scan 3개, IN은 = ANY로 Bitmap Index Scan 1개)
-- Buffers 변화: shared hit=908 -> OR shared hit=510 read=4 / IN shared hit=510 (OR/IN 모두 Heap Blocks exact=506)
-- Execution Time 변화: 12.682 ms -> OR 0.860 ms / IN 0.832 ms (약 15배 개선)
-- 개선된 이유:
 - 인덱스가 없을 땐 3개의 OR 조건을 5만 건 전체에 Filter로 적용하는 Seq Scan이 선택된다.
 - branch_code 인덱스 추가 후 각 값을 인덱스로 찾아 Bitmap으로 합친 뒤 Heap을 접근하므로, 스캔 대상이 약 1,500건(3%)으로 좁혀진다.
 - 개선안 A: OR은 BitmapOr 노드 아래에 값마다 Bitmap Index Scan을 하나씩(3회) 수행해 그 결과를 OR로 합친다.
 - 개선안 B: IN은 branch_code = ANY('{B003,B004,B005}') 로 정규화되어 Bitmap Index Scan을 1회만 수행한다. 최종 Heap Blocks(506)와 실행 시간은 사실상 동일하지만, IN이 인덱스 스캔 노드 수가 적어 계획이 더 단순하고 가독성도 좋다.
 - 값 개수가 늘어날수록 OR은 나열이 길어지고 BitmapOr 가지도 늘어나므로, IN 표기가 유지보수에 유리하다.
*/

--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- CREATE INDEX idx_employees_branch_code ON day03_tuning.employees USING btree (branch_code)

-- ############################################################################
-- 문제 6. 비-SARGable 날짜 조건: 함수 대신 범위 검색
-- ############################################################################
/*
[문제]
 2025년에 입사한 사원을 찾는다.
 EXTRACT 함수가 컬럼에 적용된 조건과 원본 컬럼의 범위 조건을 비교한다.

[개선 목표]
 인덱스가 사용할 수 있는 반개구간 [2025-01-01, 2026-01-01)으로 재작성한다.
*/


-- 개선 전: 인덱스 없음

-- 비교 A: 컬럼에 함수 적용
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, hire_date
FROM employees
WHERE extract(year FROM hire_date) = 2025;

--  QUERY PLAN
-- Seq Scan on employees  (cost=0.00..1658.00 rows=250 width=32) (actual rows=5003 loops=1)
--  Filter: (EXTRACT(year FROM hire_date) = '2025'::numeric)
--  Rows Removed by Filter: 44997
--  Buffers: shared hit=908
--Planning:
--  Buffers: shared hit=5 dirtied=2
--Planning Time: 0.172 ms
--Execution Time: 11.804 ms

-- 비교 B: SARGable 범위 조건 <<< 이부분은 직접 작성합니다.
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, hire_date
FROM employees
WHERE hire_date >= date '2025-01-01'
  AND hire_date <  date '2026-01-01';

--  QUERY PLAN
-- Seq Scan on employees  (cost=0.00..1658.00 rows=4926 width=32) (actual rows=5003 loops=1)
--  Filter: ((hire_date >= '2025-01-01'::date) AND (hire_date < '2026-01-01'::date))
--  Rows Removed by Filter: 44997
--  Buffers: shared hit=908
--Planning Time: 0.080 ms
--Execution Time: 4.931 ms

-- 개선안 <<< 이부분은 직접 작성합니다.
DROP INDEX IF EXISTS idx_employees_hire_date;
CREATE INDEX idx_employees_hire_date
    ON employees (hire_date);

ANALYZE employees;

-- 개선 후
-- 비교 A: 컬럼에 함수 적용
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, hire_date
FROM employees
WHERE extract(year FROM hire_date) = 2025;

--  QUERY PLAN
-- Seq Scan on employees  (cost=0.00..1658.00 rows=250 width=32) (actual rows=5003 loops=1)
--  Filter: (EXTRACT(year FROM hire_date) = '2025'::numeric)
--  Rows Removed by Filter: 44997
--  Buffers: shared hit=908
-- Planning Time: 0.080 ms
-- Execution Time: 7.922 ms

-- 비교 B: SARGable 범위 조건 <<< 이부분은 직접 작성합니다.
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, hire_date
FROM employees
WHERE hire_date >= date '2025-01-01'
  AND hire_date <  date '2026-01-01';

--  QUERY PLAN
-- Bitmap Heap Scan on employees  (cost=75.88..1059.37 rows=5033 width=32) (actual rows=5003 loops=1)
--  Recheck Cond: ((hire_date >= '2025-01-01'::date) AND (hire_date < '2026-01-01'::date))
--  Heap Blocks: exact=586
--  Buffers: shared hit=594
--  ->  Bitmap Index Scan on idx_employees_hire_date  (cost=0.00..74.62 rows=5033 width=0) (actual rows=5003 loops=1)
--        Index Cond: ((hire_date >= '2025-01-01'::date) AND (hire_date < '2026-01-01'::date))
--        Buffers: shared hit=8
-- Planning Time: 0.095 ms
-- Execution Time: 1.439 ms

/*
-- [개선 결과 해석]
-- 변경된 Plan Node:
--   A(함수 조건): 인덱스 생성 후에도 Seq Scan 유지 (변화 없음)
--   B(범위 조건): Seq Scan -> Bitmap Heap Scan (Bitmap Index Scan on idx_employees_hire_date)
-- Buffers 변화: A는 shared hit=908 그대로 / B는 908 -> 594 (Heap Blocks exact=586)
-- Execution Time 변화: A는 11.804 -> 7.922 ms(캐시 효과일 뿐 여전히 전체 스캔) / B는 4.931 -> 1.439 ms
-- 개선된 이유:
 - extract(year FROM hire_date) = 2025 처럼 컬럼에 함수를 씌우면 인덱스에 저장된 원본 hire_date 값과 매칭할 수 없어(비-SARGable) 인덱스를 쓰지 못하고 Seq Scan이 강제된다.
 - 같은 조건을 hire_date >= '2025-01-01' AND hire_date < '2026-01-01' 반개구간으로 재작성하면 컬럼이 가공되지 않아(SARGable) B-tree 인덱스로 범위를 바로 탐색할 수 있다.
 - 두 쿼리의 결과 행(5003건)은 동일하지만, A는 5만 건 전체를 훑고 B는 인덱스로 해당 범위만 좁혀 읽는다.
 - 부가 이점으로, A는 함수 때문에 추정 행 수가 250건으로 실제(5003건)와 크게 빗나가 계획 정확도가 떨어지는 반면, B는 추정 5033건으로 실제와 거의 일치해 옵티마이저가 컬럼 통계를 정상 활용한다.
 - 상한을 <= '2025-12-31'이 아니라 < '2026-01-01'로 둔 것은 경계값 누락을 피하는 반개구간 관례 때문이다.
*/

--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- CREATE INDEX idx_employees_hire_date ON day03_tuning.employees USING btree (hire_date)

-- ############################################################################
-- 문제 7. 복합 인덱스와 왼쪽 우선 규칙
-- ############################################################################
/*
[문제]
 부서 5의 DEV 직무 사원을 연봉순으로 상위 20명 조회한다.
 복합 인덱스의 컬럼 순서를 설계하고 왼쪽 선두 컬럼이 빠진 검색도 비교한다.

[개선 목표]
 등호 조건을 앞에, 정렬 컬럼을 뒤에 배치한다.
*/



-- 개선 전
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, salary
FROM employees
WHERE department_id = 5
  AND job_code = 'DEV'
ORDER BY salary DESC
LIMIT 20;

--  QUERY PLAN
-- Limit  (cost=1664.76..1664.81 rows=20 width=35) (actual rows=20 loops=1)
--  Buffers: shared hit=908
--  ->  Sort  (cost=1664.76..1665.39 rows=254 width=35) (actual rows=20 loops=1)
--        Sort Key: salary DESC
--        Sort Method: top-N heapsort  Memory: 27kB
--        Buffers: shared hit=908
--        ->  Seq Scan on employees  (cost=0.00..1658.00 rows=254 width=35) (actual rows=250 loops=1)
--              Filter: ((department_id = 5) AND ((job_code)::text = 'DEV'::text))
--              Rows Removed by Filter: 49750
--              Buffers: shared hit=908
-- Planning:
--  Buffers: shared hit=11 dirtied=3
-- Planning Time: 0.293 ms
-- Execution Time: 7.470 ms

-- 개선안 <<< 이부분은 직접 작성합니다.
DROP INDEX IF EXISTS idx_employees_dept_job_salary;
CREATE INDEX idx_employees_dept_job_salary
    ON employees (department_id, job_code, salary DESC);

ANALYZE employees;

-- 개선 후
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, salary
FROM employees
WHERE department_id = 5
  AND job_code = 'DEV'
ORDER BY salary DESC -- 개선 후 별도 Sort가 제거되는지 확인한다
LIMIT 20;

--  QUERY PLAN(ORDER BY 주석 처리 o)
-- Limit  (cost=11.04..55.40 rows=20 width=35) (actual rows=20 loops=1)
--  Buffers: shared hit=24
--  ->  Bitmap Heap Scan on employees  (cost=11.04..578.87 rows=256 width=35) (actual rows=20 loops=1)
--        Recheck Cond: ((department_id = 5) AND ((job_code)::text = 'DEV'::text))
--        Heap Blocks: exact=20
--        Buffers: shared hit=24
--        ->  Bitmap Index Scan on idx_employees_dept_job_salary  (cost=0.00..10.97 rows=256 width=0) (actual rows=250 loops=1)
--              Index Cond: ((department_id = 5) AND ((job_code)::text = 'DEV'::text))
--              Buffers: shared hit=4
-- Planning Time: 0.138 ms
-- Execution Time: 0.118 ms

--  QUERY PLAN(ORDER BY 주석 처리 x)
-- Limit  (cost=0.41..71.68 rows=20 width=35) (actual rows=20 loops=1)
--  Buffers: shared hit=23
--  ->  Index Scan using idx_employees_dept_job_salary on employees  (cost=0.41..912.58 rows=256 width=35) (actual rows=20 loops=1)
--        Index Cond: ((department_id = 5) AND ((job_code)::text = 'DEV'::text))
--        Buffers: shared hit=23
-- Planning Time: 0.107 ms
-- Execution Time: 0.071 ms

-- 조건 컬럼 department_id를 생략한 비교 쿼리
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, salary
FROM employees
WHERE job_code = 'DEV'
ORDER BY salary DESC
LIMIT 20;

--  QUERY PLAN
-- Limit  (cost=1667.78..1667.83 rows=20 width=35) (actual rows=20 loops=1)
--  Buffers: shared hit=908
--  ->  Sort  (cost=1667.78..1680.44 rows=5065 width=35) (actual rows=20 loops=1)
--        Sort Key: salary DESC
--        Sort Method: top-N heapsort  Memory: 27kB
--        Buffers: shared hit=908
--        ->  Seq Scan on employees  (cost=0.00..1533.00 rows=5065 width=35) (actual rows=5000 loops=1)
--              Filter: ((job_code)::text = 'DEV'::text)
--              Rows Removed by Filter: 45000
--              Buffers: shared hit=908
-- Planning Time: 0.151 ms
-- Execution Time: 5.761 ms

/*
-- [개선 결과 해석]
-- 변경된 Plan Node:
--   본 쿼리(dept+job, ORDER BY 있음): Seq Scan + Sort(top-N) -> Index Scan (Sort 노드 제거)
--   (참고) ORDER BY 생략 시: Bitmap Heap Scan (정렬 불필요로 판단, 순서 미보장)
--   선두 생략(job만): Seq Scan + Sort 유지 (인덱스 미사용)
-- Buffers 변화: 본 쿼리 shared hit=908 -> 23 / 선두 생략은 908 그대로
-- Execution Time 변화: 본 쿼리 7.470 -> 0.071 ms / 선두 생략 5.761 ms (개선 없음)
-- 개선된 이유:
 - (department_id, job_code, salary DESC) 복합 인덱스는 등호 조건 두 개를 선두에 두어 부서 5·DEV 사원 250건으로 인덱스에서 바로 좁힌다.
 - salary DESC를 마지막에 배치해 좁혀진 범위가 이미 연봉 내림차순으로 정렬돼 있으므로, ORDER BY salary DESC가 있어도 별도 Sort 없이 Index Scan으로 앞 20건만 읽고 LIMIT로 조기 종료한다.
 - ORDER BY를 생략하면 정렬이 필요 없어 옵티마이저가 Bitmap Heap Scan을 택하는데, 이는 더 빠를 수 있으나 인덱스의 정렬 순서를 유지하지 않는다. 따라서 "연봉순 상위 20명"이라는 요구를 정확히 만족하려면 ORDER BY를 유지해야 하며, 이때 Index Scan이 선택되어 정렬 순서가 보장된다.
 - 반면 job_code만으로 검색하면 복합 인덱스의 선두 컬럼 department_id가 빠져 왼쪽 우선 규칙(leftmost prefix rule)을 만족하지 못하므로 인덱스를 효율적으로 쓸 수 없고 Seq Scan + Sort로 되돌아간다.
 - 이는 복합 인덱스가 왼쪽 선두 컬럼부터 연속으로 조건이 주어질 때만 효과적이며, 정렬 컬럼을 뒤에 두면 Sort까지 제거할 수 있음을 보여준다.
*/

--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- CREATE INDEX idx_employees_dept_job_salary ON day03_tuning.employees USING btree (department_id, job_code, salary DESC)

-- ############################################################################
-- 문제 8. 커버링 인덱스와 Index Only Scan
-- ############################################################################
/*
[문제]
 특정 사번 구간의 이름과 이메일을 조회한다.
 검색 컬럼과 출력 컬럼을 구분하여 INCLUDE 커버링 인덱스를 설계한다.

[개선 목표]
 Heap Fetches가 적은 Index Only Scan 가능성을 높인다.
*/

-- 개선 전
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_no, full_name, email
FROM employees
WHERE employee_no >= 'EMP040000'
  AND employee_no <  'EMP040051'
ORDER BY employee_no;

--  QUERY PLAN
-- Sort  (cost=1658.01..1658.02 rows=1 width=38) (actual rows=51 loops=1)
--  Sort Key: employee_no
--  Sort Method: quicksort  Memory: 27kB
--  Buffers: shared hit=908
--  ->  Seq Scan on employees  (cost=0.00..1658.00 rows=1 width=38) (actual rows=51 loops=1)
--        Filter: (((employee_no)::text >= 'EMP040000'::text) AND ((employee_no)::text < 'EMP040051'::text))
--        Rows Removed by Filter: 49949
--        Buffers: shared hit=908
-- Planning:
--  Buffers: shared hit=6
-- Planning Time: 0.244 ms
-- Execution Time: 21.839 ms

-- 개선안 <<< 이부분은 직접 작성합니다.
DROP INDEX IF EXISTS idx_employees_no_covering;
CREATE INDEX idx_employees_no_covering
    ON employees (employee_no)
    INCLUDE (full_name, email);

VACUUM ANALYZE employees; -- 따로 실행 

-- 개선 후
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_no, full_name, email
FROM employees
WHERE employee_no >= 'EMP040000'
  AND employee_no <  'EMP040051'
ORDER BY employee_no;

--  QUERY PLAN
-- Index Only Scan using idx_employees_no_covering on employees  (cost=0.41..4.43 rows=1 width=38) (actual rows=51 loops=1)
--  Index Cond: ((employee_no >= 'EMP040000'::text) AND (employee_no < 'EMP040051'::text))
--  Heap Fetches: 0
--  Buffers: shared hit=1 read=4
-- Planning:
--  Buffers: shared hit=19
-- Planning Time: 3.155 ms
-- Execution Time: 0.126 ms

/*
-- [개선 결과 해석]
-- 변경된 Plan Node: Seq Scan + Sort(quicksort) -> Index Only Scan (Sort 노드 제거)
-- Buffers 변화: shared hit=908 -> shared hit=1 read=4 (Heap 접근 제거)
-- Execution Time 변화: 21.839 ms -> 0.126 ms
-- 개선된 이유:
 - employee_no 인덱스가 범위 조건(EMP040000 이상 EMP040051 미만)을 정렬 순서로 바로 탐색하고, 인덱스가 이미 employee_no 오름차순이라 ORDER BY용 별도 Sort 노드가 사라진다.
 - INCLUDE로 출력 컬럼(full_name, email)을 인덱스 리프에 담아, 조회에 필요한 모든 컬럼이 인덱스만으로 충족되므로 테이블(Heap) 접근 없이 Index Only Scan이 된다.
 - VACUUM ANALYZE로 visibility map을 갱신했기에 Heap Fetches: 0이 되었다. 이 과정을 생략하면 Index Only Scan이라도 각 행의 가시성 확인을 위해 Heap을 다시 읽어 Heap Fetches가 크게 남는다.
 - 따라서 계획 이름이 Index Only Scan이라도 Heap Fetches 값을 함께 확인해야 실제 커버링 효과를 판단할 수 있으며, 이 경우 Heap Fetches=0으로 완전한 커버링이 확인된다.
*/

--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- CREATE INDEX idx_employees_no_covering ON day03_tuning.employees USING btree (employee_no) INCLUDE (full_name, email)

-- ############################################################################
-- 문제 9. 조인, loops, 근무 기록 복합 인덱스
-- ############################################################################
/*
[문제]
 부서 5의 재직자 중 최근 30일 초과근무 합계가 큰 사원 20명을 조회한다.
 5만 사원과 50만 근무 기록의 조인 계획을 읽는다.

[개선 목표]
 외부 사원 행마다 반복되는 work_logs 탐색 비용과 loops를 줄인다.
*/

-- 개선 전
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT
    e.employee_id,
    e.employee_no,
    e.full_name,
    sum(w.overtime_minutes) AS total_overtime_minutes
FROM employees AS e
JOIN employee_work_logs AS w
  ON w.employee_id = e.employee_id
WHERE e.department_id = 5
  AND e.employment_status = 'ACTIVE'
  AND w.work_date >= current_date - 30
GROUP BY
    e.employee_id,
    e.employee_no,
    e.full_name
ORDER BY total_overtime_minutes DESC
LIMIT 20;

--  QUERY PLAN
-- Limit  (cost=10027.53..10027.58 rows=20 width=36) (actual rows=20 loops=1)
--  Buffers: shared hit=6965 read=10
--  ->  Sort  (cost=10027.53..10029.86 rows=931 width=36) (actual rows=20 loops=1)
--        Sort Key: (sum(w.overtime_minutes)) DESC
--        Sort Method: top-N heapsort  Memory: 27kB
--        Buffers: shared hit=6965 read=10
--        ->  Finalize GroupAggregate  (cost=9893.21..10002.76 rows=931 width=36) (actual rows=616 loops=1)
--              Group Key: e.employee_id
--              Buffers: shared hit=6965 read=10
--              ->  Gather Merge  (cost=9893.21..9989.57 rows=776 width=36) (actual rows=867 loops=1)
--                    Workers Planned: 2
--                    Workers Launched: 2
--                    Buffers: shared hit=6965 read=10
--                    ->  Partial GroupAggregate  (cost=8893.18..8899.97 rows=388 width=36) (actual rows=289 loops=3)
--                          Group Key: e.employee_id
--                          Buffers: shared hit=6965 read=10
--                          ->  Sort  (cost=8893.18..8894.15 rows=388 width=32) (actual rows=342 loops=3)
--                                Sort Key: e.employee_id
--                                Sort Method: quicksort  Memory: 42kB
--                                Buffers: shared hit=6965 read=10
--                                Worker 0:  Sort Method: quicksort  Memory: 42kB
--                                Worker 1:  Sort Method: quicksort  Memory: 38kB
--                                ->  Hash Join  (cost=1040.39..8876.50 rows=388 width=32) (actual rows=342 loops=3)
--                                      Hash Cond: (w.employee_id = e.employee_id)
--                                      Buffers: shared hit=6949 read=10
--                                      ->  Parallel Seq Scan on employee_work_logs w  (cost=0.00..7812.83 rows=8865 width=12) (actual rows=7077 loops=3)
--                                            Filter: (work_date >= (CURRENT_DATE - 30))
--                                            Rows Removed by Filter: 159589
--                                            Buffers: shared hit=4167
--                                      ->  Hash  (cost=1013.03..1013.03 rows=2189 width=28) (actual rows=2500 loops=3)
--                                            Buckets: 4096  Batches: 1  Memory Usage: 179kB
--                                            Buffers: shared hit=2758 read=10
--                                            ->  Bitmap Heap Scan on employees e  (cost=67.65..1013.03 rows=2189 width=28) (actual rows=2500 loops=3)
--                                                  Recheck Cond: (department_id = 5)
--                                                  Filter: ((employment_status)::text = 'ACTIVE'::text)
--                                                  Heap Blocks: exact=908
--                                                  Buffers: shared hit=2758 read=10
--                                                  ->  Bitmap Index Scan on idx_employees_dept_job_salary  (cost=0.00..67.11 rows=2492 width=0) (actual rows=2500 loops=3)
--                                                        Index Cond: (department_id = 5)
--                                                        Buffers: shared hit=34 read=10
-- Planning:
--  Buffers: shared hit=76 dirtied=6
-- Planning Time: 6.750 ms
-- Execution Time: 25.814 ms

-- 개선안 <<< 이부분은 직접 작성합니다.
-- (1) 조인 대상 work_logs 인덱스
DROP INDEX IF EXISTS idx_work_logs_emp_date;
CREATE INDEX idx_work_logs_emp_date
    ON employee_work_logs (employee_id, work_date)
    INCLUDE (overtime_minutes);

-- (2) 부서 5 재직자 필터용 인덱스 (문제 7 인덱스에 의존하지 않도록)
DROP INDEX IF EXISTS idx_employees_dept_active;
CREATE INDEX idx_employees_dept_active
    ON employees (department_id)
    WHERE employment_status = 'ACTIVE';

ANALYZE employee_work_logs;
ANALYZE employees;

-- 개선 후
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT
    e.employee_id,
    e.employee_no,
    e.full_name,
    sum(w.overtime_minutes) AS total_overtime_minutes
FROM employees AS e
JOIN employee_work_logs AS w
  ON w.employee_id = e.employee_id
WHERE e.department_id = 5
  AND e.employment_status = 'ACTIVE'
  AND w.work_date >= current_date - 30
GROUP BY
    e.employee_id,
    e.employee_no,
    e.full_name
ORDER BY total_overtime_minutes DESC
LIMIT 20;

--  QUERY PLAN
-- Limit  (cost=7548.78..7548.83 rows=20 width=36) (actual rows=20 loops=1)
--  Buffers: shared hit=8417
--  ->  Sort  (cost=7548.78..7551.08 rows=920 width=36) (actual rows=20 loops=1)
--        Sort Key: (sum(w.overtime_minutes)) DESC
--        Sort Method: top-N heapsort  Memory: 27kB
--        Buffers: shared hit=8417
--        ->  GroupAggregate  (cost=7508.20..7524.30 rows=920 width=36) (actual rows=616 loops=1)
--              Group Key: e.employee_id
--              Buffers: shared hit=8417
--              ->  Sort  (cost=7508.20..7510.50 rows=920 width=32) (actual rows=1026 loops=1)
--                    Sort Key: e.employee_id
--                    Sort Method: quicksort  Memory: 97kB
--                    Buffers: shared hit=8417
--                    ->  Nested Loop  (cost=25.35..7462.91 rows=920 width=32) (actual rows=1026 loops=1)
--                          Buffers: shared hit=8417
--                          ->  Bitmap Heap Scan on employees e  (cost=24.92..965.11 rows=2146 width=28) (actual rows=2500 loops=1)
--                                Recheck Cond: ((department_id = 5) AND ((employment_status)::text = 'ACTIVE'::text))
--                                Heap Blocks: exact=908
--                                Buffers: shared hit=912
--                                ->  Bitmap Index Scan on idx_employees_dept_active  (cost=0.00..24.38 rows=2146 width=0) (actual rows=2500 loops=1)
--                                      Index Cond: (department_id = 5)
--                                      Buffers: shared hit=4
--                          ->  Index Only Scan using idx_work_logs_emp_date on employee_work_logs w  (cost=0.43..3.02 rows=1 width=12) (actual rows=0 loops=2500)
--                                Index Cond: ((employee_id = e.employee_id) AND (work_date >= (CURRENT_DATE - 30)))
--                                Heap Fetches: 0
--                                Buffers: shared hit=7505
-- Planning:
--  Buffers: shared hit=17
-- Planning Time: 0.277 ms
-- Execution Time: 8.015 ms

/*
-- [개선 결과 해석]
-- 변경된 Plan Node: Hash Join(Parallel Seq Scan on work_logs) -> Nested Loop + Index Only Scan on idx_work_logs_emp_date
--                   employees 측: Bitmap Index Scan이 idx_employees_dept_active(부서·재직자 전용 인덱스)로 처리됨
-- Buffers 변화: shared hit=6965 read=10 -> shared hit=8417 (총 블록 수는 오히려 증가)
-- Execution Time 변화: 25.814 ms -> 8.015 ms (약 3배 단축)
-- 개선된 이유:
 - 개선 전에는 work_logs에 조인·날짜용 인덱스가 없어 50만 건을 Parallel Seq Scan으로 읽고 Filter로 최근 30일만 남겼다(Rows Removed by Filter: 159,589 × 워커). 이후 Hash Join으로 결합했다.
 - work_logs에 (employee_id, work_date) INCLUDE(overtime_minutes) 복합 인덱스를 만들자 옵티마이저가 Nested Loop로 전환했다. 부서 5 재직자 2,500명을 외부(driving)로 두고, 각 사원의 최근 30일 근무기록을 인덱스로 직접 탐색한다.
 - 이때 안쪽 Index Only Scan의 loops=2500이며, INCLUDE 덕분에 Heap Fetches: 0으로 테이블 접근이 전혀 없다.
 - employees 측 부서·재직자 필터는 이 문제에서 별도로 만든 부분 인덱스 idx_employees_dept_active(department_id 키 + WHERE employment_status='ACTIVE')로 처리되어, 다른 문제의 인덱스에 의존하지 않고 이 쿼리만으로 독립 실행된다.
 - 주목할 점은 총 Buffers가 6965 -> 8417로 오히려 늘었다는 것이다. Nested Loop가 사원 2,500명마다 인덱스를 반복 탐색(loops=2500)해 인덱스 블록 접근이 누적됐기 때문이다. 그럼에도 실행 시간이 3배 빨라진 이유는, 늘어난 블록이 대부분 캐시된 인덱스 블록(shared hit)이고 Heap Fetches=0이라 블록당 처리 비용이 매우 낮으며, 병렬 Hash 구성·해시 빌드 같은 무거운 작업이 사라졌기 때문이다.
 - 따라서 Plan 이름(Hash Join/Nested Loop)이나 Buffers 총량만으로 우열을 판단하지 말고, loops와 블록의 성격(hit/read, Heap Fetches), 실제 실행 시간을 함께 봐야 한다.
*/


--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Tables/ANALYZE테이블명/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employee_work_logs'
ORDER BY indexname desc;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- CREATE INDEX idx_work_logs_emp_date ON day03_tuning.employee_work_logs USING btree (employee_id, work_date) INCLUDE (overtime_minutes)

select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname desc;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- CREATE INDEX idx_employees_dept_active ON day03_tuning.employees USING btree (department_id) WHERE ((employment_status)::text = 'ACTIVE'::text)

-- ############################################################################
-- 문제 10. NOT IN의 NULL 함정과 NOT EXISTS 안티 조인
-- ############################################################################
/*
[문제]
 완료된 교육 이력이 없는 사원을 찾는다.
 서브쿼리에 NULL이 포함된 NOT IN의 결과를 확인하고 정확한 쿼리로 수정한다.

[개선 목표]
 정확성을 먼저 회복하고, NOT EXISTS와 부분 인덱스로 안티 조인을 지원한다.
*/


-- 잘못된 쿼리: 서브쿼리 결과에 NULL이 있어 전체 결과가 0건이 될 수 있다. -> NOT IN 사용
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT e.employee_id, e.employee_no, e.full_name
FROM employees AS e
WHERE e.employee_id NOT IN (
    SELECT t.employee_id
    FROM employee_training AS t
    WHERE t.completion_status = 'COMPLETED'
);

--  QUERY PLAN
-- Seq Scan on employees e  (cost=1028.41..2561.41 rows=25000 width=28) (actual rows=0 loops=1)
--  Filter: (NOT (ANY (employee_id = (hashed SubPlan 1).col1)))
--  Rows Removed by Filter: 50000
--  Buffers: shared hit=1284
--  SubPlan 1
--    ->  Seq Scan on employee_training t  (cost=0.00..938.51 rows=35960 width=8) (actual rows=36001 loops=1)
--          Filter: ((completion_status)::text = 'COMPLETED'::text)
--          Rows Removed by Filter: 9000
--          Buffers: shared hit=376
-- Planning:
--  Buffers: shared hit=6
-- Planning Time: 0.256 ms
-- Execution Time: 20.430 ms

SELECT count(*) AS wrong_result_count
FROM employees AS e
WHERE e.employee_id NOT IN (
    SELECT t.employee_id
    FROM employee_training AS t
    WHERE t.completion_status = 'COMPLETED'
); -- 0이 나옴 

-- 개선안 <<< 이부분은 직접 작성합니다.
DROP INDEX IF EXISTS idx_training_completed_emp;
CREATE INDEX idx_training_completed_emp
    ON employee_training (employee_id)
    WHERE completion_status = 'COMPLETED';

ANALYZE employee_training;

-- 정확한 쿼리 -> NOT EXISTS 사용
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT e.employee_id, e.employee_no, e.full_name
FROM employees AS e
WHERE NOT EXISTS (
    SELECT 1
    FROM employee_training AS t
    WHERE t.employee_id = e.employee_id
      AND t.completion_status = 'COMPLETED'
);

--  QUERY PLAN
-- Hash Anti Join  (cost=1388.49..3108.26 rows=14002 width=28) (actual rows=14000 loops=1)
--  Hash Cond: (e.employee_id = t.employee_id)
--  Buffers: shared hit=1284
--  ->  Seq Scan on employees e  (cost=0.00..1408.00 rows=50000 width=28) (actual rows=50000 loops=1)
--        Buffers: shared hit=908
--  ->  Hash  (cost=938.51..938.51 rows=35998 width=8) (actual rows=36000 loops=1)
--        Buckets: 65536  Batches: 1  Memory Usage: 1919kB
--        Buffers: shared hit=376
--        ->  Seq Scan on employee_training t  (cost=0.00..938.51 rows=35998 width=8) (actual rows=36001 loops=1)
--              Filter: ((completion_status)::text = 'COMPLETED'::text)
--              Rows Removed by Filter: 9000
--              Buffers: shared hit=376
-- Planning:
--  Buffers: shared hit=28 read=1
-- Planning Time: 0.467 ms
-- Execution Time: 19.539 ms

SELECT count(*) AS correct_result_count
FROM employees AS e
WHERE NOT EXISTS (
    SELECT 1
    FROM employee_training AS t
    WHERE t.employee_id = e.employee_id
      AND t.completion_status = 'COMPLETED'
); -- 14000이 나옴 

/*
-- [개선 결과 해석]
-- 변경된 Plan Node: (NOT IN) Seq Scan + hashed SubPlan -> (NOT EXISTS) Hash Anti Join
-- Buffers 변화: shared hit=1284 -> shared hit=1284 (동일)
-- Execution Time 변화: 20.430 ms -> 19.539 ms (사실상 동일)
-- 개선된 이유:
 - 이 문제의 핵심은 성능이 아니라 결과 정확성 회복이다. employee_training에 employee_id가 NULL인 COMPLETED 행이 존재해, NOT IN 서브쿼리 결과에 NULL이 섞인다.
 - SQL의 3값 논리에서 x NOT IN (..., NULL)은 어떤 x에 대해서도 참이 될 수 없어(NULL과의 비교가 UNKNOWN) 전체 결과가 0건이 된다. 실제로 wrong_result_count = 0으로 확인된다.
 - NOT EXISTS는 각 사원마다 완료 이력의 존재 여부만 상관 서브쿼리로 판단하므로 NULL의 영향을 받지 않는다. 옵티마이저는 이를 Hash Anti Join으로 처리해 정확한 미이수자 14,000명을 반환한다(correct_result_count = 14000).
 - 성능(시간·Buffers)은 거의 변하지 않았는데, 이는 두 쿼리 모두 employees 5만 건과 완료 이력 3.6만 건을 대부분 읽어야 하는 구조이기 때문이다. 정확성만 바뀌었을 뿐 읽는 데이터 양은 비슷하다.

 - [부분 인덱스가 사용되지 않은 이유]
   completion_status = 'COMPLETED' 부분 인덱스를 만들었지만 실행 계획에는 쓰이지 않고 employee_training에 Seq Scan이 선택됐다. 완료 이력이 전체 45,000건 중 36,001건으로 대다수(약 80%)를 차지해 선택도가 낮기 때문이다. 안티 조인은 완료 이력 대부분을 읽어 해시 테이블을 만들어야 하므로, 인덱스를 통한 임의 접근보다 순차 스캔 후 해시 조인이 더 저렴하다고 옵티마이저가 판단한 것이다.
   부분 인덱스는 대상 행이 소수일 때(예: PLANNED 등 희소 상태 검색) 효과적이며, 이 경우처럼 대상이 다수면 순차 스캔이 합리적 선택이다.
*/

--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Tables/ANALYZE테이블명/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employee_training'
ORDER BY indexname desc;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- CREATE INDEX idx_training_completed_emp ON day03_tuning.employee_training USING btree (employee_id) WHERE ((completion_status)::text = 'COMPLETED'::text)



--======[최종 제출 내용]=================================================================================
/*
 - -- 개선전 QUERY PLAN << 복붙
 - -- 개선안 <<< 이부분은 직접 작성합니다.
 - -- 개선후 QUERY PLAN << 복붙
 - -- [개선 결과 해석] <<< 이부분은 직접 작성합니다.
 - -- [인덱스 정의 확인] << 복붙


=========[최종 제출 파일]======================================================================
-- day03_쿼리_홍길동.sql
-- Slack > 다이렉트 메시지

*/
