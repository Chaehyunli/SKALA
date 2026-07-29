-- ============================================================
-- 종합실습3-1 — HR DB 느린 쿼리 성능 튜닝 실습 (LAB A~E)
-- PostgreSQL 11+  |  psql 실행 버전
-- ============================================================
-- ⚠️ psql 사용 안내
--  1) psql은 DBeaver와 달리 "하나의 세션이 계속 유지"되므로,
--     PART 0에서 설정한 SET search_path = hr, public; 이 스크립트가
--     끝날 때까지 그대로 유지됩니다. (DBeaver 버전처럼 스키마를
--     매번 명시할 필요가 없습니다.)
--  2) 실행 방법
--     - 전체 실행: psql -U <user> -d <db> -f 종합실습3-1_HR_성능튜닝_psql버전.sql
--     - 또는 psql 접속 후: \i /경로/종합실습3-1_HR_성능튜닝_psql버전.sql
--     - 한 문장씩 보며 실습하려면 psql 접속 후 이 파일을 열어 SQL 블록을
--       하나씩 복사 → 붙여넣기 → Enter 로 실행하세요.
--  3) \timing on 을 켜두면 각 문장의 총 소요 시간이 EXPLAIN ANALYZE의
--     내부 실행시간과 별도로 psql 하단에 함께 출력되어 비교가 쉽습니다.
--  4) [중요] "튜닝 전" EXPLAIN ANALYZE는 인덱스가 없어서 느린 건지,
--     디스크에서 처음 읽어와서(콜드 캐시) 느린 건지 구분하기 위해
--     같은 쿼리를 1회 더 실행해 캐시를 데운 뒤 결과를 비교하세요.
--     (각 LAB의 STEP 1은 "2번 연속 실행" 후 2번째 값을 기준으로 삼습니다.)
-- ============================================================

\timing on
\pset pager off

-- ================================================================
-- PART 0. 환경 설정 (스키마 / 테이블 / 데이터)
--   - hr 스키마 사용 (LAB B의 reverse_text() 함수가 이 스키마에 생성됨)
--   - hire_date는 FIXED 로직 사용: 20%는 최근 365일 이내, 80%는 그 이전으로 분산
--     → 아래 [LAB C]에서 "최근 365일" 조건이 항상 결과를 반환함
-- ================================================================
\echo '=== PART 0. 환경 설정 시작 ==='

DROP SCHEMA IF EXISTS hr CASCADE;
CREATE SCHEMA hr;
SET search_path = hr, public;

CREATE TABLE locations (
  location_id   SERIAL PRIMARY KEY,
  city          TEXT NOT NULL,
  country       TEXT NOT NULL,
  region        TEXT NOT NULL
);

CREATE TABLE departments (
  department_id   SERIAL PRIMARY KEY,
  department_name TEXT NOT NULL,
  location_id     INT NOT NULL REFERENCES locations(location_id)
);

CREATE TABLE jobs (
  job_id     SERIAL PRIMARY KEY,
  job_title  TEXT NOT NULL,
  min_salary INT NOT NULL,
  max_salary INT NOT NULL
);

CREATE TABLE employees (
  employee_id   BIGSERIAL PRIMARY KEY,
  first_name    TEXT NOT NULL,
  last_name     TEXT NOT NULL,
  email         TEXT UNIQUE NOT NULL,
  phone         TEXT,
  hire_date     DATE NOT NULL,
  salary        INT NOT NULL,
  manager_id    BIGINT NULL,
  department_id INT NOT NULL REFERENCES departments(department_id),
  job_id        INT NOT NULL REFERENCES jobs(job_id),
  status        TEXT NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE job_history (
  employee_id   BIGINT NOT NULL REFERENCES employees(employee_id),
  start_date    DATE NOT NULL,
  end_date      DATE NOT NULL,
  department_id INT NOT NULL REFERENCES departments(department_id),
  job_id        INT NOT NULL REFERENCES jobs(job_id),
  PRIMARY KEY (employee_id, start_date)
);

-- 기준 데이터 적재
INSERT INTO locations(city, country, region)
SELECT
  'City_'||gs::text,
  (ARRAY['KR','US','JP','DE','FR','GB','IN','CN'])[1 + (random()*7)::int],
  (ARRAY['APAC','EMEA','AMER'])[1 + (random()*2)::int]
FROM generate_series(1, 50) gs;

INSERT INTO departments(department_name, location_id)
SELECT
  'Dept_'||gs::text,
  1 + (random() * 49)::int
FROM generate_series(1, 200) gs;

INSERT INTO jobs(job_title, min_salary, max_salary)
SELECT
  'Job_'||gs::text,
  2000 + (random()*1000)::int,
  8000 + (random()*7000)::int
FROM generate_series(1, 40) gs;

-- 대량 employees 생성 (약 50,000건) — hire_date FIXED 로직
WITH nums AS (
  SELECT gs AS n FROM generate_series(1, 50000) gs
)
INSERT INTO employees(
  first_name, last_name, email, phone, hire_date, salary,
  manager_id, department_id, job_id, status
)
SELECT
  'First_'||n,
  'Last_'||n,
  lower('user'||n||'@'||(ARRAY['corp.com','example.com','mail.com','gmail.com','outlook.com'])[1+(random()*4)::int]),
  '010-'||lpad(((random()*9999)::int)::text,4,'0')||'-'||lpad(((random()*9999)::int)::text,4,'0'),
  CASE
    WHEN random() < 0.20
      THEN CURRENT_DATE - ((random() * 364)::int)          -- 최근 365일 (20%)
    ELSE
      CURRENT_DATE - (365 + (random() * 3300)::int)        -- 그 이전 (약 9년) (80%)
  END AS hire_date,
  2000 + (random()*10000)::int,
  CASE WHEN random() < 0.2 THEN NULL ELSE 1 + (random()*200)::int END,
  1 + (random()*199)::int,
  1 + (random()*39)::int,
  CASE WHEN random() < 0.05 THEN 'INACTIVE' ELSE 'ACTIVE' END
FROM nums;

-- job_history 일부 생성 (직무/부서 이동 이력)
INSERT INTO job_history(employee_id, start_date, end_date, department_id, job_id)
SELECT
  e.employee_id,
  e.hire_date,
  e.hire_date + (30 + (random()*900)::int),
  1 + (random()*199)::int,
  1 + (random()*39)::int
FROM employees e
WHERE e.employee_id % 10 = 0;  -- 10명 중 1명만 이력 생성

VACUUM ANALYZE;

-- 검증: 최근 365일 조건에 걸리는 직원 수 (0이 아니어야 정상)
SELECT COUNT(*) AS recent_365_cnt
FROM employees
WHERE hire_date >= CURRENT_DATE - INTERVAL '365 days';

\echo '=== PART 0. 환경 설정 완료 ==='


-- ================================================================
-- PART 1. 느린 쿼리 튜닝 실습 (LAB A~E)
-- ================================================================

-- =========================================
-- [LAB A] 느린 검색 1: 함수 적용 컬럼 조건 (lower(email))
-- =========================================
\echo '--- [LAB A] STEP A1: 튜닝 전 (2번 연속 실행 후 2번째 값 기준) ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT employee_id, email
FROM employees
WHERE lower(email) = 'user1234@corp.com';

EXPLAIN (ANALYZE, BUFFERS)
SELECT employee_id, email
FROM employees
WHERE lower(email) = 'user1234@corp.com';

\echo '--- [LAB A] STEP A2: 인덱스 생성 ---'
CREATE INDEX IF NOT EXISTS idx_employees_lower_email ON employees ((lower(email)));

\echo '--- [LAB A] STEP A3: 튜닝 후 ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT employee_id, email
FROM employees
WHERE lower(email) = 'user1234@corp.com';

-- =========================================
-- [LAB B] 느린 검색 2: 선행 와일드카드 LIKE ('%gmail.com')
-- =========================================
\echo '--- [LAB B] STEP B1: 튜닝 전 (2번 연속 실행 후 2번째 값 기준) ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM employees
WHERE email LIKE '%gmail.com';

EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM employees
WHERE email LIKE '%gmail.com';

\echo '--- [LAB B] STEP B2: reverse 함수 + 함수 기반 인덱스 생성 ---'
CREATE OR REPLACE FUNCTION hr.reverse_text(txt TEXT) RETURNS TEXT
LANGUAGE SQL IMMUTABLE PARALLEL SAFE AS
$$ SELECT reverse($1); $$;

CREATE INDEX IF NOT EXISTS idx_employees_rev_email ON employees ((hr.reverse_text(email)));

\echo '--- [LAB B] STEP B3: 튜닝 후 (선행 와일드카드를 후행 와일드카드로 변환) ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM employees
WHERE hr.reverse_text(email) LIKE hr.reverse_text('%gmail.com');

-- (선택) pg_trgm 확장으로 LIKE 가속 — 원본 이메일 컬럼 그대로 LIKE '%gmail.com' 가속하고 싶다면
-- CREATE EXTENSION IF NOT EXISTS pg_trgm;
-- CREATE INDEX IF NOT EXISTS idx_employees_email_trgm ON employees USING gin (email gin_trgm_ops);
-- EXPLAIN (ANALYZE, BUFFERS)
-- SELECT count(*) FROM employees WHERE email LIKE '%gmail.com';

-- =========================================
-- [LAB C] 느린 조인: 필터/정렬 + 다중 테이블
-- 요구: 최근 365일 입사, 급여 상위 100명, 부서/직무명 포함
-- =========================================
\echo '--- [LAB C] STEP C1: 튜닝 전 (2번 연속 실행 후 2번째 값 기준) ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT e.employee_id, e.hire_date, e.salary, d.department_name, j.job_title
FROM employees e
JOIN departments d ON e.department_id = d.department_id
JOIN jobs j ON e.job_id = j.job_id
WHERE e.hire_date >= CURRENT_DATE - INTERVAL '365 days'
  AND e.status = 'ACTIVE'
ORDER BY e.salary DESC
LIMIT 100;

EXPLAIN (ANALYZE, BUFFERS)
SELECT e.employee_id, e.hire_date, e.salary, d.department_name, j.job_title
FROM employees e
JOIN departments d ON e.department_id = d.department_id
JOIN jobs j ON e.job_id = j.job_id
WHERE e.hire_date >= CURRENT_DATE - INTERVAL '365 days'
  AND e.status = 'ACTIVE'
ORDER BY e.salary DESC
LIMIT 100;

\echo '--- [LAB C] STEP C2: 복합 인덱스 생성 (필터 컬럼 + 정렬 컬럼) ---'
CREATE INDEX IF NOT EXISTS idx_emp_hire_status_salary ON employees (hire_date, status, salary DESC);

\echo '--- [LAB C] STEP C3: 튜닝 후 ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT e.employee_id, e.hire_date, e.salary, d.department_name, j.job_title
FROM employees e
JOIN departments d ON e.department_id = d.department_id
JOIN jobs j ON e.job_id = j.job_id
WHERE e.hire_date >= CURRENT_DATE - INTERVAL '365 days'
  AND e.status = 'ACTIVE'
ORDER BY e.salary DESC
LIMIT 100;

-- =========================================
-- [LAB D] OR 조건 → UNION ALL/IN으로 재작성
-- =========================================
\echo '--- [LAB D] STEP D1: 튜닝 전 (인덱스 없이 OR 조건) ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*)
FROM employees
WHERE department_id = 10
   OR job_id IN (3,4,5);

\echo '--- [LAB D] STEP D2: UNION ALL로 재작성 ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*) FROM (
  SELECT employee_id FROM employees WHERE department_id = 10
  UNION ALL
  SELECT employee_id FROM employees WHERE job_id IN (3,4,5)
) x;

\echo '--- [LAB D] STEP D3: 각 컬럼에 인덱스 생성 ---'
CREATE INDEX IF NOT EXISTS idx_emp_department_id ON employees(department_id);
CREATE INDEX IF NOT EXISTS idx_emp_job_id ON employees(job_id);

\echo '--- [LAB D] STEP D4: 튜닝 후 (Bitmap Index Scan + BitmapOr 확인) ---'
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(*)
FROM employees
WHERE department_id = 10
   OR job_id IN (3,4,5);

-- =========================================
-- [LAB E] 통계 최신화 효과 (Planner 행 수 추정 정확도 개선)
-- =========================================
\echo '--- [LAB E] 통계 갱신 ---'
ANALYZE employees;

SELECT count(*) AS employees_cnt FROM employees;

\echo '=== 실습 종료 ==='
\timing off