/*
===============================================================================
 PostgreSQL day03_tuning 종합 실습
 - 50,000명 사원 데이터 + 500,000건 근무 기록
 - 느린 쿼리 진단 및 개선 문제 10개
 - 각 문제에 문제 SQL, 개선 SQL, 실행 계획 비교, 해설을 함께 수록
===============================================================================

[권장 환경]
  - PostgreSQL 14 이상
  - pg_trgm 확장을 설치할 수 있는 권한
  - 테스트 DB에서 실행

[실행 방법]
  1. 이 파일 전체를 한 번 실행하여 스키마와 데이터를 만든다.
  2. 각 문제의 "개선 전" EXPLAIN을 실행하고 결과(Query Plain - Execution Time, Buffers, Plan Node 등)를 저장한다.
  3. 해당 문제의 개선 SQL을 구현 및 실행한다.
  4. "개선 후" EXPLAIN을 실행하여 (Query Plain - Execution Time, Buffers, Plan Node 등)을 비교한다.
  5. 캐시 영향을 줄이기 위해 같은 쿼리를 2회 이상 실행하길 권장한다.

[주의]
  - EXPLAIN (ANALYZE ...)는 쿼리를 실제 실행한다.
  - 실행 시간은 PC 사양, PostgreSQL 설정, 캐시 상태에 따라 달라진다.
  - 목표는 특정 밀리초를 맞추는 것이 아니라 실행 계획과 읽은 블록 수의 변화를 해석하는 것이다.
===============================================================================
*/


-- ============================================================================
-- 0. 실습 환경 초기화 - SQL스크립트 실행시 샘플 데이터가 많아서 PC성능에 따라 시간이 지체될 수 있습니다.
-- ============================================================================

DROP SCHEMA IF EXISTS day03_tuning CASCADE;
CREATE SCHEMA day03_tuning;
COMMENT ON SCHEMA day03_tuning IS
    'PostgreSQL SQL 튜닝 종합 실습용 스키마 - day03_tuning';

SET search_path TO day03_tuning, public;

-- 문제 3의 선행 조건이다.
-- pg_trgm은 trusted extension이므로 데이터베이스 CREATE 권한이 있으면 설치할 수 있다.
-- 권한 오류가 발생하면 데이터베이스 소유자 또는 postgres 계정으로 한 번만 실행한다.
-- "extension pg_trgm is not available" 오류가 발생하면 PostgreSQL 확장 파일 설치 상태를 확인한다.
CREATE EXTENSION IF NOT EXISTS pg_trgm
WITH SCHEMA public;



-- ============================================================================
-- 1. DDL-테이블 생성 
-- ============================================================================

CREATE TABLE departments (
    department_id   integer PRIMARY KEY,
    department_name varchar(50) NOT NULL,
    location_name   varchar(50) NOT NULL
);

CREATE TABLE employees (
    employee_id       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_no       varchar(12) NOT NULL,
    first_name        varchar(30) NOT NULL,
    last_name         varchar(30) NOT NULL,
    full_name         varchar(61)
                      GENERATED ALWAYS AS (last_name || first_name) STORED,
    email             varchar(120) NOT NULL,
    department_id     integer NOT NULL
                      REFERENCES departments(department_id),
    job_code          varchar(20) NOT NULL,
    branch_code       varchar(10) NOT NULL,
    hire_date         date NOT NULL,
    salary            numeric(12, 0) NOT NULL,
    employment_status varchar(10) NOT NULL
                      CHECK (employment_status IN ('ACTIVE', 'LEAVE', 'INACTIVE')),
    phone              varchar(20),
    created_at         timestamptz NOT NULL DEFAULT current_timestamp
);

CREATE TABLE employee_work_logs (
    work_log_id     bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id     bigint NOT NULL REFERENCES employees(employee_id),
    work_date       date NOT NULL,
    work_minutes    integer NOT NULL,
    overtime_minutes integer NOT NULL,
    work_type       varchar(12) NOT NULL
);

CREATE TABLE employee_training (
    training_id      bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id      bigint REFERENCES employees(employee_id),
    course_code      varchar(20) NOT NULL,
    completion_status varchar(12) NOT NULL
                      CHECK (completion_status IN ('COMPLETED', 'PLANNED')),
    completed_at     date
);

COMMENT ON TABLE employees IS
    '5만 명 인사 정보: 단일/복합/부분/표현식/커버링 인덱스 실습';
COMMENT ON TABLE employee_work_logs IS
    '50만 건 근무 기록: 조인, loops, 복합 인덱스 실습';
COMMENT ON TABLE employee_training IS
    'NOT IN의 NULL 함정과 NOT EXISTS 안티 조인 실습';


-- ============================================================================
-- 2. DML-기준 데이터 입력
-- ============================================================================

INSERT INTO departments (department_id, department_name, location_name)
SELECT
    g,
    (ARRAY[
        '경영기획', '인사', '재무', '법무', '개발1',
        '개발2', '데이터', '보안', '인프라', '품질',
        '제품기획', '디자인', '국내영업', '해외영업', '마케팅',
        '고객지원', '구매', '물류', '연구소', '감사'
    ])[g],
    (ARRAY['서울', '부산', '대전', '광주', '판교'])[1 + ((g - 1) % 5)]
FROM generate_series(1, 20) AS s(g);


-- 50,000명 사원 생성
-- employee_id = 1234의 이메일은 user1234@corp.com이 되도록 구성했다.
INSERT INTO employees (
    employee_no,
    first_name,
    last_name,
    email,
    department_id,
    job_code,
    branch_code,
    hire_date,
    salary,
    employment_status,
    phone,
    created_at
)
SELECT
    'EMP' || lpad(g::text, 6, '0'),
    (ARRAY[
        '민준', '서준', '도윤', '예준', '시우',
        '하준', '지호', '주원', '지후', '준우',
        '서연', '서윤', '지우', '하윤', '민서',
        '지민', '채원', '수아', '지아', '다은'
    ])[1 + ((g * 13) % 20)],
    (ARRAY['김', '이', '박', '최', '정', '강', '조', '윤', '장', '임'])
        [1 + ((g * 7) % 10)],
    CASE
        WHEN g % 100 = 0 THEN 'user' || g || '@gmail.com'
        ELSE 'user' || g || '@corp.com'
    END,
    1 + (g % 20),
    (ARRAY[
        'DEV', 'DBA', 'DATA', 'SECURITY', 'SALES',
        'HR', 'FINANCE', 'QA', 'PM', 'SUPPORT'
    ])[1 + ((g * 7 + g / 20) % 10)],
    'B' || lpad((1 + (g % 100))::text, 3, '0'),
    current_date - ((g * 37) % 3650),
    30000000 + ((g::bigint * 7919) % 90000000),
    CASE
        WHEN g % 25 = 0 THEN 'LEAVE'
        WHEN g % 10 = 0 THEN 'INACTIVE'
        ELSE 'ACTIVE'
    END,
    '010-' || lpad(((g * 17) % 10000)::text, 4, '0')
           || '-' || lpad(((g * 43) % 10000)::text, 4, '0'),
    current_timestamp
        - make_interval(days => ((g * 37) % 3650))
        - make_interval(mins => (g % 1440))
FROM generate_series(1, 50000) AS s(g);


-- 500,000건 근무 기록 생성
INSERT INTO employee_work_logs (
    employee_id,
    work_date,
    work_minutes,
    overtime_minutes,
    work_type
)
SELECT
    1 + ((g - 1) % 50000),
    current_date - ((g * 37) % 730),
    420 + ((g * 17) % 121),
    (g * 13) % 121,
    CASE
        WHEN g % 20 = 0 THEN 'REMOTE'
        WHEN g % 9 = 0 THEN 'FIELD'
        ELSE 'OFFICE'
    END
FROM generate_series(1, 500000) AS s(g);


-- 45,000건 교육 이력 + NOT IN의 NULL 함정을 만드는 1건
INSERT INTO employee_training (
    employee_id,
    course_code,
    completion_status,
    completed_at
)
SELECT
    g,
    'COURSE-' || lpad((1 + (g % 30))::text, 2, '0'),
    CASE WHEN g % 5 = 0 THEN 'PLANNED' ELSE 'COMPLETED' END,
    CASE
        WHEN g % 5 = 0 THEN NULL
        ELSE current_date - ((g * 11) % 720)
    END
FROM generate_series(1, 45000) AS s(g);

INSERT INTO employee_training (
    employee_id,
    course_code,
    completion_status,
    completed_at
)
VALUES (
    NULL,
    'COURSE-NULL-DEMO',
    'COMPLETED',
    current_date
);

/*ANALYZE는 인덱스를 만드는 명령이 아니라, 옵티마이저에게 현재 데이터의 규모와 분포를 알려주는 명령입니다. 
 * 인덱스 생성 전에 실행하면 정확한 기준 실행계획을 만들고, 생성 후의 성능 개선을 공정하게 비교할 수 있습니다.*/
--ANALYZE를 인덱스 생성 전에 실행하는 이유는 PostgreSQL 옵티마이저가 현재 데이터의 분포와 규모를 정확히 파악하도록 통계정보를 갱신하기 위해서입니다.
ANALYZE departments;
ANALYZE employees;
ANALYZE employee_work_logs;
ANALYZE employee_training;

--인덱스 생성 후에도 ANALYZE가 필요한가?
--
--일반적인 컬럼 인덱스는 생성 즉시 사용할 수 있으므로, 인덱스를 인식시키기 위해 반드시 ANALYZE를 다시 실행할 필요는 없습니다.
--
--CREATE INDEX idx_employees_department
--ON employees(department_id);
--
--하지만 다음 경우에는 다시 실행하는 것이 좋습니다.
--
--대량 입력·수정·삭제가 있었을 때
--데이터 분포가 크게 달라졌을 때
--함수나 표현식 인덱스를 생성했을 때
--확장 통계 객체를 생성했을 때
--실행계획의 예상 행 수가 실제 행 수와 크게 다를 때


-- ============================================================================
-- 3. DQL-데이터 확인
-- ============================================================================

SELECT count(*) AS employee_count
FROM employees;

SELECT
    (SELECT count(*) FROM departments) AS department_count,
    (SELECT count(*) FROM employees) AS employee_count,
    (SELECT count(*) FROM employee_work_logs) AS work_log_count,
    (SELECT count(*) FROM employee_training) AS training_count;

SELECT
    employment_status,
    count(*) AS employee_count
FROM employees
GROUP BY employment_status
ORDER BY employment_status;

SELECT *
FROM employees
WHERE employee_id = 1234;