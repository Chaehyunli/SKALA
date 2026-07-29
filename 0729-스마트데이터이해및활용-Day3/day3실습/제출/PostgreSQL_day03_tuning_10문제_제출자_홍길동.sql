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

-- 개선안 - 이부분은 직접작성합니다.




-- 개선 후
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, email
FROM employees
WHERE email LIKE '%@gmail.com';




/*
-- [개선 결과 해석]
-- 변경된 Plan Node:
-- Buffers 변화:
-- Execution Time 변화: 
-- 개선된 이유:
 - 
 - 
 - 
 - 
*/
--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- 

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

/*
-- [개선 결과 해석]
-- 변경된 Plan Node:
-- Buffers 변화:
-- Execution Time 변화: 
-- 개선된 이유:
 - 
 - 
 - 
 - 
*/
--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- 

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

-- 개선 후 B: IN
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, branch_code
FROM employees
WHERE branch_code IN ('B003', 'B004', 'B005');

/*
-- [개선 결과 해석]
-- 변경된 Plan Node:
-- Buffers 변화:
-- Execution Time 변화: 
-- 개선된 이유:
 - 
 - 
 - 
 - 
*/

--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- 

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

-- 비교 B: SARGable 범위 조건 <<< 이부분은 직접 작성합니다.
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, hire_date
FROM employees
WHERE hire_date >= date '2025-01-01'
  AND hire_date <  date '2026-01-01';


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

-- 비교 B: SARGable 범위 조건 <<< 이부분은 직접 작성합니다.
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, hire_date
FROM employees
WHERE hire_date >= date '2025-01-01'
  AND hire_date <  date '2026-01-01';



/*
-- [개선 결과 해석]
-- 변경된 Plan Node:
-- Buffers 변화:
-- Execution Time 변화: 
-- 개선된 이유:
 - 
 - 
 - 
 - 
*/
--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- 

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
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF);
SELECT employee_id, employee_no, full_name, salary
FROM employees
WHERE department_id = 5
  AND job_code = 'DEV'
ORDER BY salary DESC
LIMIT 20;

-- 개선안 <<< 이부분은 직접 작성합니다.




-- 개선 후
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, salary
FROM employees
WHERE department_id = 5
  AND job_code = 'DEV'
--ORDER BY salary DESC --개선 후 별도 Sort가 제거되는지 확인한다
LIMIT 20;

-- 조건 컬럼 department_id를 생략한 비교 쿼리
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_id, employee_no, full_name, salary
FROM employees
WHERE job_code = 'DEV'
ORDER BY salary DESC
LIMIT 20;

/*
-- [개선 결과 해석]
-- 변경된 Plan Node:
-- Buffers 변화:
-- Execution Time 변화: 
-- 개선된 이유:
 - 
 - 
 - 
 - 
*/
--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- 

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

-- 개선안 <<< 이부분은 직접 작성합니다.




-- 개선 후
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT employee_no, full_name, email
FROM employees
WHERE employee_no >= 'EMP040000'
  AND employee_no <  'EMP040051'
ORDER BY employee_no;

/*
-- [개선 결과 해석]
-- 변경된 Plan Node:
-- Buffers 변화:
-- Execution Time 변화: 
-- 개선된 이유:
 - 
 - 
 - 
 - 
*/

--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employees'
ORDER BY indexname;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- 

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

-- 개선안 <<< 이부분은 직접 작성합니다.




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

/*
-- [개선 결과 해석]
-- 변경된 Plan Node:
-- Buffers 변화:
-- Execution Time 변화: 
-- 개선된 이유:
 - 
 - 
 - 
 - 
*/


--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Tables/ANALYZE테이블명/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employee_work_logs'
ORDER BY indexname desc;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- 

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


-- 잘못된 쿼리: 서브쿼리 결과에 NULL이 있어 전체 결과가 0건이 될 수 있다.
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT e.employee_id, e.employee_no, e.full_name
FROM employees AS e
WHERE e.employee_id NOT IN (
    SELECT t.employee_id
    FROM employee_training AS t
    WHERE t.completion_status = 'COMPLETED'
);

SELECT count(*) AS wrong_result_count
FROM employees AS e
WHERE e.employee_id NOT IN (
    SELECT t.employee_id
    FROM employee_training AS t
    WHERE t.completion_status = 'COMPLETED'
);

-- 개선안 <<< 이부분은 직접 작성합니다.





-- 정확한 쿼리
EXPLAIN (ANALYZE, BUFFERS, TIMING OFF)
SELECT e.employee_id, e.employee_no, e.full_name
FROM employees AS e
WHERE NOT EXISTS (
    SELECT 1
    FROM employee_training AS t
    WHERE t.employee_id = e.employee_id
      AND t.completion_status = 'COMPLETED'
);

SELECT count(*) AS correct_result_count
FROM employees AS e
WHERE NOT EXISTS (
    SELECT 1
    FROM employee_training AS t
    WHERE t.employee_id = e.employee_id
      AND t.completion_status = 'COMPLETED'
);

/*
-- [개선 결과 해석]
-- 변경된 Plan Node:
-- Buffers 변화:
-- Execution Time 변화: 
-- 개선된 이유:
 - 
 - 
 - 
 - 
*/

--[인덱스 정의 확인 쿼리 - DBeaver/Schema/day03_tuning/Tables/ANALYZE테이블명/Indexes/해당 인덱스 더블클릭/Access Method 로도 확인가능]
select indexname,indexdef FROM pg_indexes 
WHERE schemaname = 'day03_tuning' AND tablename = 'employee_training'
ORDER BY indexname desc;
--[인덱스 정의 확인] <<< select결과 indexdef컬럼 내용을 복붙한다.
-- 



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
