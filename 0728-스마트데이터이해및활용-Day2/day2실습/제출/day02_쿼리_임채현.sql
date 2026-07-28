-- ============================================================================
-- PostgreSQL HR 데이터 종합 SQL 실습
-- 문제 페이지와 솔루션 페이지 분리본
-- 대상: day02_postgresql_hr_emp_dept_샘플_스키마.sql로 생성한 hr 샘플 스키마
-- 대상 database: skala_db
-- 대상 SCHEMA: hr
-- 총 39문제: GROUP BY 10 / 서브쿼리 5 / CTE 3 / JOIN 10 /
--              VIEW 5 / MATERIALIZED VIEW 2 / CUBE 2 / ROLLUP 2
-- 사용법
--   1) 아래의 [PAGE 1. 종합실습 문제 페이지]의 문제들을(A ~ H) 보고 각 TODO 아래에 답안을 작성합니다.
--   2) 작성한 쿼리를 DBeaver에서 항목별로 실행하여 결과를 확인합니다.
--   3) 실행결과를 캡쳐합니다. 제출양식 : day02_캡쳐_작성자명.pdf 파일에 모아서 제출합니다.
--   4) 주어진 sql문제파일에 정답 쿼리를 작성해서로 제출합니다. 제출양식 : day02_쿼리_작성자명.sql
--	 5) 제출경로 : 슬렉/다이레트 메세지.
--   
-- 주의: VIEW 및 MATERIALIZED VIEW 문제는 객체를 생성하므로 순서대로 실행하십시오.
-- 		모든 별칭부여는 임의의 명칭으로 가독성 있도록 부여해도 무관합니다(영문,한글 무관)
-- ============================================================================

SET search_path TO hr, public;

-- ############################################################################
-- PAGE 1. 종합실습 문제 페이지
-- ############################################################################


-- ============================================================================
-- A. GROUP BY 종합실습 (10문제)
-- ============================================================================

-- [GROUP BY 1] 부서별 사원 수
-- 문제: HR 데이터를 활용하여 '부서별 사원 수' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: GROUP BY와 필요한 집계 함수를 사용하십시오. department_id가 같은 사원을 그룹화하여 인원수를 계산합니다. 부서 미배정 사원은 '부서 미배정'으로 표시합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    COALESCE(d.department_name, '부서 미배정') AS "부서명(department_name)",
    COUNT(*) AS "부서별 사원 수"
FROM employees e
LEFT JOIN departments d ON d.department_id = e.department_id
GROUP BY e.department_id, d.department_name
ORDER BY "부서별 사원 수" DESC, "부서명(department_name)";

-- [GROUP BY 2] 직무별 사원 수와 평균 급여
-- 문제: HR 데이터를 활용하여 '직무별 사원 수와 평균 급여' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: GROUP BY와 필요한 집계 함수를 사용하십시오. 직무별 인원수와 평균 급여를 동시에 집계합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    j.job_title AS "직무명(job_title)",
    COUNT(*) AS "직무별 사원 수",
    ROUND(AVG(e.salary), 2) AS "직무별 평균 급여"
FROM employees e
JOIN jobs j ON j.job_id = e.job_id
GROUP BY e.job_id, j.job_title
ORDER BY "직무별 평균 급여" DESC, "직무별 사원 수" DESC;

-- [GROUP BY 3] 부서별 급여 합계·평균·최솟값·최댓값
-- 문제: HR 데이터를 활용하여 '부서별 급여 합계·평균·최솟값·최댓값' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: GROUP BY와 필요한 집계 함수를 사용하십시오. 하나의 그룹에 여러 집계 함수를 적용합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    COALESCE(d.department_name, '부서 미배정') AS "부서명(department_name)",
    SUM(e.salary)           AS "부서별 급여 합계",
    ROUND(AVG(e.salary), 2) AS "부서별 급여 평균",
    MIN(e.salary)           AS "부서별 급여 최솟값",
    MAX(e.salary)           AS "부서별 급여 최댓값"
FROM employees e
LEFT JOIN departments d ON d.department_id = e.department_id
GROUP BY e.department_id, d.department_name
ORDER BY "부서별 급여 합계" DESC, "부서명(department_name)";

-- [GROUP BY 4] 관리자별 부하 직원 수
-- 문제: HR 데이터를 활용하여 '관리자별 부하 직원 수' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: GROUP BY와 필요한 집계 함수를 사용하십시오. 사원 테이블을 관리자 기준으로 그룹화합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    m.employee_id AS "관리자 사번(employee_id)",
    concat_ws(' ', m.first_name, m.last_name) AS "관리자명(employee_name)",
    COUNT(*) AS "관리자별 부하 직원 수"
FROM employees e
JOIN employees m ON m.employee_id = e.manager_id
GROUP BY m.employee_id, m.first_name, m.last_name
ORDER BY "관리자별 부하 직원 수" DESC, "관리자 사번(employee_id)";

-- [GROUP BY 5] 입사 연도별 입사자 수
-- 문제: HR 데이터를 활용하여 '입사 연도별 입사자 수' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: GROUP BY와 필요한 집계 함수를 사용하십시오. EXTRACT로 날짜에서 연도만 꺼낸 뒤 그룹화합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    EXTRACT(YEAR FROM e.hire_date)::int AS "입사 연도(hire_year)",
    COUNT(*) AS "입사 연도별 입사자 수"
FROM employees e
GROUP BY EXTRACT(YEAR FROM e.hire_date)
ORDER BY "입사 연도(hire_year)";

-- [GROUP BY 6] 커미션 수령 여부별 인원과 평균 급여
-- 문제: HR 데이터를 활용하여 '커미션 수령 여부별 인원과 평균 급여' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: GROUP BY와 필요한 집계 함수를 사용하십시오. CASE 표현식의 결과도 GROUP BY 기준으로 사용할 수 있습니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    CASE
        WHEN e.commission_pct IS NOT NULL THEN '수령'
        ELSE '미수령'
    END AS "커미션 수령 여부",
    COUNT(*) AS "커미션 수령 여부별 인원",
    ROUND(AVG(e.salary), 2) AS "커미션 수령 여부별 평균 급여"
FROM employees e
GROUP BY
    CASE
        WHEN e.commission_pct IS NOT NULL THEN '수령'
        ELSE '미수령'
    END
ORDER BY "커미션 수령 여부" DESC;

-- [GROUP BY 7] 국가별 근무 사원 수
-- 문제: HR 데이터를 활용하여 '국가별 근무 사원 수' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: GROUP BY와 필요한 집계 함수를 사용하십시오. employees부터 countries까지 여러 테이블을 연결한 뒤 국가별로 집계합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    c.country_name AS "국가명(country_name)",
    COUNT(*) AS "국가별 근무 사원 수"
FROM employees e
JOIN departments d ON d.department_id = e.department_id
JOIN locations l ON l.location_id = d.location_id
JOIN countries c ON c.country_id = l.country_id
GROUP BY c.country_id, c.country_name
ORDER BY "국가별 근무 사원 수" DESC, "국가명(country_name)";

-- [GROUP BY 8] 부서별 직무 종류 수
-- 문제: HR 데이터를 활용하여 '부서별 직무 종류 수' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: GROUP BY와 필요한 집계 함수를 사용하십시오. COUNT(DISTINCT 열)을 사용하면 중복을 제외한 개수를 구할 수 있습니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    COALESCE(d.department_name, '부서 미배정') AS "부서명(department_name)",
    COUNT(DISTINCT e.job_id) AS "부서별 직무 종류 수"
FROM employees e
LEFT JOIN departments d ON d.department_id = e.department_id
GROUP BY e.department_id, d.department_name
ORDER BY "부서별 직무 종류 수" DESC, "부서명(department_name)";

-- [GROUP BY 9] 평균 급여가 8,000 이상인 부서
-- 문제: HR 데이터를 활용하여 '평균 급여가 8,000 이상인 부서' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: GROUP BY와 필요한 집계 함수를 사용하십시오. WHERE는 그룹화 전 행을, HAVING은 그룹화 후 결과를 필터링합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    COALESCE(d.department_name, '부서 미배정') AS "부서명(department_name)",
    ROUND(AVG(e.salary), 2) AS "부서별 평균 급여"
FROM employees e
LEFT JOIN departments d ON d.department_id = e.department_id
GROUP BY e.department_id, d.department_name
HAVING AVG(e.salary) >= 8000
ORDER BY "부서별 평균 급여" DESC;

-- [GROUP BY 10] 부서별 급여 구간 인원수
-- 문제: HR 데이터를 활용하여 '부서별 급여 구간 인원수' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: GROUP BY와 필요한 집계 함수를 사용하십시오. CASE로 만든 급여 구간과 부서를 두 가지 기준으로 그룹화합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    COALESCE(d.department_name, '부서 미배정') AS "부서명(department_name)",
    CASE
        WHEN e.salary >= 10000 THEN '상위(10000+)'
        WHEN e.salary >= 5000  THEN '중위(5000-9999)'
        ELSE '하위(5000미만)'
    END AS "급여 구간",
    COUNT(*) AS "부서별 급여 구간 인원수"
FROM employees e
LEFT JOIN departments d ON d.department_id = e.department_id
GROUP BY
    e.department_id,
    d.department_name,
    CASE
        WHEN e.salary >= 10000 THEN '상위(10000+)'
        WHEN e.salary >= 5000  THEN '중위(5000-9999)'
        ELSE '하위(5000미만)'
    END
ORDER BY "부서명(department_name)", "급여 구간";

-- ============================================================================
-- B. 서브쿼리 종합실습 (5문제)
-- ============================================================================

-- [서브쿼리 1] 전체 평균보다 급여가 높은 사원
-- 문제: HR 데이터를 활용하여 '전체 평균보다 급여가 높은 사원' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: 서브쿼리를 사용해 조건을 해결하십시오. 단일 행 서브쿼리에서 계산한 전체 평균과 각 사원의 급여를 비교합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    e.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)",
    e.salary AS "급여(salary)"
FROM employees e
WHERE e.salary > (SELECT AVG(salary) FROM employees)
ORDER BY "급여(salary)" DESC;

-- [서브쿼리 2] 소속 부서 평균보다 급여가 높은 사원
-- 문제: HR 데이터를 활용하여 '소속 부서 평균보다 급여가 높은 사원' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: 서브쿼리를 사용해 조건을 해결하십시오. 바깥 쿼리의 department_id를 참조하는 상관 서브쿼리입니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    e.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)",
    e.department_id AS "부서 번호(department_id)",
    e.salary AS "급여(salary)"
FROM employees e
WHERE e.salary > (
    SELECT AVG(sub.salary)
    FROM employees sub
    WHERE sub.department_id = e.department_id
)
ORDER BY e.department_id, "급여(salary)" DESC;

-- [서브쿼리 3] 사원이 한 명이라도 존재하는 부서
-- 문제: HR 데이터를 활용하여 '사원이 한 명이라도 존재하는 부서' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: 서브쿼리를 사용해 조건을 해결하십시오. EXISTS는 서브쿼리 결과 행의 존재 여부만 확인합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    d.department_id AS "부서 번호(department_id)",
    d.department_name AS "부서명(department_name)"
FROM departments d
WHERE EXISTS (
    SELECT 1
    FROM employees e
    WHERE e.department_id = d.department_id
)
ORDER BY "부서 번호(department_id)";

-- [서브쿼리 4] 가장 높은 평균 급여를 가진 부서
-- 문제: HR 데이터를 활용하여 '가장 높은 평균 급여를 가진 부서' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: 서브쿼리를 사용해 조건을 해결하십시오. FROM 절 서브쿼리로 부서 평균을 만든 후 가장 큰 평균과 비교합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    dept_avg.department_id AS "부서 번호(department_id)",
    d.department_name AS "부서명(department_name)",
    ROUND(dept_avg.avg_salary, 2) AS "부서별 평균 급여"
FROM (
    SELECT department_id, AVG(salary) AS avg_salary
    FROM employees
    WHERE department_id IS NOT NULL
    GROUP BY department_id
) AS dept_avg
JOIN departments d ON d.department_id = dept_avg.department_id
WHERE dept_avg.avg_salary = (
    SELECT MAX(sub.avg_salary)
    FROM (
        SELECT department_id, AVG(salary) AS avg_salary
        FROM employees
        WHERE department_id IS NOT NULL
        GROUP BY department_id
    ) AS sub
);

-- [서브쿼리 5] 과거 직무 이력이 없는 사원
-- 문제: HR 데이터를 활용하여 '과거 직무 이력이 없는 사원' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: 서브쿼리를 사용해 조건을 해결하십시오. NOT EXISTS는 관련 행이 하나도 없는 대상을 찾을 때 안전합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    e.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)"
FROM employees e
WHERE NOT EXISTS (
    SELECT 1
    FROM job_history jh
    WHERE jh.employee_id = e.employee_id
)
ORDER BY "사번(employee_id)";

-- ============================================================================
-- C. CTE 종합실습 (3문제)
-- ============================================================================

-- [CTE 1] 부서별 평균 급여와 전체 평균 비교
-- 문제: HR 데이터를 활용하여 '부서별 평균 급여와 전체 평균 비교' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: WITH 절(CTE)을 사용해 단계적으로 작성하십시오. WITH 절에서 부서 통계를 먼저 만든 뒤 메인 쿼리에서 사용합니다.
-- TODO: 아래에 SQL을 작성하십시오.

WITH dept_avg AS (
    SELECT
        e.department_id,
        AVG(e.salary) AS dept_avg_salary
    FROM employees e
    WHERE e.department_id IS NOT NULL
    GROUP BY e.department_id
)
SELECT
    da.department_id AS "부서 번호(department_id)",
    d.department_name AS "부서명(department_name)",
    ROUND(da.dept_avg_salary, 2) AS "부서별 평균 급여",
    ROUND((SELECT AVG(salary) FROM employees), 2) AS "전체 평균 급여",
    CASE
        WHEN da.dept_avg_salary >= (SELECT AVG(salary) FROM employees) THEN '전체 평균 이상'
        ELSE '전체 평균 미만'
    END AS "비교 결과"
FROM dept_avg da
JOIN departments d ON d.department_id = da.department_id
ORDER BY "부서별 평균 급여" DESC;

-- [CTE 2] 두 단계 CTE로 부서별 최고 급여자 찾기
-- 문제: HR 데이터를 활용하여 '두 단계 CTE로 부서별 최고 급여자 찾기' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: WITH 절(CTE)을 사용해 단계적으로 작성하십시오. 첫 번째 CTE의 결과를 두 번째 CTE가 다시 참조합니다.
-- TODO: 아래에 SQL을 작성하십시오.

WITH dept_max AS (
    SELECT
        e.department_id,
        MAX(e.salary) AS max_salary
    FROM employees e
    WHERE e.department_id IS NOT NULL
    GROUP BY e.department_id
),
top_earner AS (
    SELECT
        e.employee_id,
        e.first_name,
        e.last_name,
        e.department_id,
        e.salary
    FROM employees e
    JOIN dept_max dm
      ON dm.department_id = e.department_id
     AND dm.max_salary = e.salary
)
SELECT
    te.department_id AS "부서 번호(department_id)",
    d.department_name AS "부서명(department_name)",
    te.employee_id AS "사번(employee_id)",
    concat_ws(' ', te.first_name, te.last_name) AS "사원명(employee_name)",
    te.salary AS "급여(salary)"
FROM top_earner te
JOIN departments d ON d.department_id = te.department_id
ORDER BY "부서 번호(department_id)";

-- [CTE 3] 재귀 CTE로 사원 조직도 조회
-- 문제: HR 데이터를 활용하여 '재귀 CTE로 사원 조직도 조회' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: WITH 절(CTE)을 사용해 단계적으로 작성하십시오. 최고경영자를 시작점으로 manager_id 관계를 반복하여 내려갑니다.
-- TODO: 아래에 SQL을 작성하십시오.

WITH RECURSIVE org_chart AS (
    -- 앵커: 최고경영자 (관리자가 없는 사원)
    SELECT
        e.employee_id,
        e.first_name,
        e.last_name,
        e.manager_id,
        1 AS level
    FROM employees e
    WHERE e.manager_id IS NULL

    UNION ALL

    -- 재귀: 바로 위 단계 사원을 상사로 둔 부하들
    SELECT
        e.employee_id,
        e.first_name,
        e.last_name,
        e.manager_id,
        oc.level + 1
    FROM employees e
    JOIN org_chart oc ON oc.employee_id = e.manager_id
)
SELECT
    oc.level AS "조직 단계(level)",
    oc.employee_id AS "사번(employee_id)",
    concat_ws(' ', oc.first_name, oc.last_name) AS "사원명(employee_name)",
    oc.manager_id AS "관리자 사번(manager_id)"
FROM org_chart oc
ORDER BY oc.level, oc.employee_id;

-- ============================================================================
-- D. JOIN 종합실습 (10문제)
-- ============================================================================

-- [JOIN 1: INNER JOIN] 사원과 부서
-- 문제: HR 데이터를 활용하여 '사원과 부서' 결과를 조회하는 SQL을 작성하십시오.
-- 조건:  INNER JOIN 방식을 활용하십시오. 부서가 배정된 사원만 출력합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    e.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)",
    d.department_id AS "부서 번호(department_id)",
    d.department_name AS "부서명(department_name)"
FROM employees e
INNER JOIN departments d ON d.department_id = e.department_id
ORDER BY "사번(employee_id)";

-- [JOIN 2: INNER JOIN] 사원과 직무
-- 문제: HR 데이터를 활용하여 '사원과 직무' 결과를 조회하는 SQL을 작성하십시오.
-- 조건:  INNER JOIN 방식을 활용하십시오. job_id가 일치하는 직무명을 사원 정보에 결합합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    e.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)",
    j.job_id AS "직무 코드(job_id)",
    j.job_title AS "직무명(job_title)"
FROM employees e
INNER JOIN jobs j ON j.job_id = e.job_id
ORDER BY "사번(employee_id)";

-- [JOIN 3: 4개 테이블 INNER JOIN] 사원의 부서·도시·국가
-- 문제: HR 데이터를 활용하여 '사원의 부서·도시·국가' 결과를 조회하는 SQL을 작성하십시오.
-- 조건:  4개 테이블 INNER JOIN 방식을 활용하십시오. 사원에서 국가까지 FK 관계를 차례로 연결합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    e.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)",
    d.department_name AS "부서명(department_name)",
    l.city AS "도시(city)",
    c.country_name AS "국가명(country_name)"
FROM employees e
INNER JOIN departments d ON d.department_id = e.department_id
INNER JOIN locations l ON l.location_id = d.location_id
INNER JOIN countries c ON c.country_id = l.country_id
ORDER BY "국가명(country_name)", "사번(employee_id)";

-- [JOIN 4: SELF JOIN] 사원과 직속 관리자
-- 문제: HR 데이터를 활용하여 '사원과 직속 관리자' 결과를 조회하는 SQL을 작성하십시오.
-- 조건:  SELF JOIN 방식을 활용하십시오. 동일한 employees 테이블을 사원용 e와 관리자용 m으로 두 번 사용합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    e.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)",
    m.employee_id AS "관리자 사번(manager_id)",
    concat_ws(' ', m.first_name, m.last_name) AS "관리자명(manager_name)"
FROM employees e
INNER JOIN employees m ON m.employee_id = e.manager_id
ORDER BY "사번(employee_id)";

-- [JOIN 5: LEFT OUTER JOIN] 부서 미배정 사원 포함
-- 문제: HR 데이터를 활용하여 '부서 미배정 사원 포함' 결과를 조회하는 SQL을 작성하십시오.
-- 조건:  LEFT OUTER JOIN 방식을 활용하십시오. 왼쪽 employees의 모든 행을 보존하므로 부서가 없는 사원도 나옵니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    e.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)",
    d.department_id AS "부서 번호(department_id)",
    COALESCE(d.department_name, '부서 미배정') AS "부서명(department_name)"
FROM employees e
LEFT OUTER JOIN departments d ON d.department_id = e.department_id
ORDER BY "사번(employee_id)";

-- [JOIN 6: LEFT OUTER JOIN] 사원이 없는 부서 포함
-- 문제: HR 데이터를 활용하여 '사원이 없는 부서 포함' 결과를 조회하는 SQL을 작성하십시오.
-- 조건:  LEFT OUTER JOIN 방식을 활용하십시오. 모든 부서를 보존하고 소속 사원이 없으면 사원 열을 NULL로 표시합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    d.department_id AS "부서 번호(department_id)",
    d.department_name AS "부서명(department_name)",
    e.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)"
FROM departments d
LEFT OUTER JOIN employees e ON e.department_id = d.department_id
ORDER BY "부서 번호(department_id)", "사번(employee_id)";

-- [JOIN 7: RIGHT OUTER JOIN] 모든 직무 포함
-- 문제: HR 데이터를 활용하여 '모든 직무 포함' 결과를 조회하는 SQL을 작성하십시오.
-- 조건:  RIGHT OUTER JOIN 방식을 활용하십시오. 오른쪽 jobs의 모든 행을 보존하여 현재 담당 사원이 없는 직무도 보여줍니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    e.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)",
    j.job_id AS "직무 코드(job_id)",
    j.job_title AS "직무명(job_title)"
FROM employees e
RIGHT OUTER JOIN jobs j ON j.job_id = e.job_id
ORDER BY j.job_id, "사번(employee_id)";

-- [JOIN 8: FULL OUTER JOIN] 모든 부서와 모든 사원
-- 문제: HR 데이터를 활용하여 '모든 부서와 모든 사원' 결과를 조회하는 SQL을 작성하십시오.
-- 조건:  FULL OUTER JOIN 방식을 활용하십시오. 어느 쪽에도 일치 상대가 없는 행까지 모두 보존합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    e.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)",
    d.department_id AS "부서 번호(department_id)",
    d.department_name AS "부서명(department_name)"
FROM employees e
FULL OUTER JOIN departments d ON d.department_id = e.department_id
ORDER BY d.department_id NULLS LAST, e.employee_id NULLS LAST;

-- [JOIN 9: INNER JOIN] 사원의 과거 직무 이력
-- 문제: HR 데이터를 활용하여 '사원의 과거 직무 이력' 결과를 조회하는 SQL을 작성하십시오.
-- 조건:  INNER JOIN 방식을 활용하십시오. job_history를 employees, jobs, departments와 결합합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    jh.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)",
    jh.start_date AS "시작일(start_date)",
    jh.end_date AS "종료일(end_date)",
    j.job_title AS "직무명(job_title)",
    d.department_name AS "부서명(department_name)"
FROM job_history jh
INNER JOIN employees e ON e.employee_id = jh.employee_id
INNER JOIN jobs j ON j.job_id = jh.job_id
INNER JOIN departments d ON d.department_id = jh.department_id
ORDER BY jh.employee_id, jh.start_date;

-- [JOIN 10: 비등가 JOIN] 사원의 급여가 직무 급여 범위에 맞는지 확인
-- 문제: HR 데이터를 활용하여 '사원의 급여가 직무 급여 범위에 맞는지 확인' 결과를 조회하는 SQL을 작성하십시오.
-- 조건:  비등가 JOIN 방식을 활용하십시오. 등호가 아닌 BETWEEN 조건으로 급여와 직무 범위를 결합합니다.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    e.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)",
    j.job_title AS "직무명(job_title)",
    e.salary AS "급여(salary)",
    j.min_salary AS "직무 최소 급여(min_salary)",
    j.max_salary AS "직무 최대 급여(max_salary)",
    CASE
        WHEN e.salary BETWEEN j.min_salary AND j.max_salary THEN '범위 내'
        ELSE '범위 벗어남'
    END AS "급여 적정성"
FROM employees e
JOIN jobs j
  ON e.job_id = j.job_id
 AND e.salary BETWEEN j.min_salary AND j.max_salary
ORDER BY "사번(employee_id)";

-- ============================================================================
-- E. VIEW 종합실습 (5문제)
-- ============================================================================

-- [VIEW 1] 사원 기본 정보 View
-- 문제: HR 데이터를 활용하여 '사원 기본 정보 View'를 제공하는 일반 View를 생성하고 조회하십시오.
-- 조건: 지정된 이름의 일반 View를 생성한 뒤 결과를 조회하십시오.
-- TODO: 아래에 SQL을 작성하십시오.

CREATE OR REPLACE VIEW hr.v_employee_basic AS
SELECT
    e.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)",
    e.email AS "이메일(email)",
    e.phone_number AS "전화번호(phone_number)",
    e.hire_date AS "입사일(hire_date)",
    e.salary AS "급여(salary)"
FROM employees e;

-- 조회
SELECT * FROM hr.v_employee_basic
ORDER BY "사번(employee_id)";

-- [VIEW 2] 사원·부서·직무 통합 View
-- 문제: HR 데이터를 활용하여 '사원·부서·직무 통합 View'를 제공하는 일반 View를 생성하고 조회하십시오.
-- 조건: 지정된 이름의 일반 View를 생성한 뒤 결과를 조회하십시오.
-- TODO: 아래에 SQL을 작성하십시오.

CREATE OR REPLACE VIEW hr.v_employee_detail AS
SELECT
    e.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)",
    j.job_title AS "직무명(job_title)",
    COALESCE(d.department_name, '부서 미배정') AS "부서명(department_name)",
    e.salary AS "급여(salary)"
FROM employees e
INNER JOIN jobs j ON j.job_id = e.job_id
LEFT JOIN departments d ON d.department_id = e.department_id;

-- 조회
SELECT * FROM hr.v_employee_detail
ORDER BY "사번(employee_id)";

-- [VIEW 3] 부서별 급여 통계 View
-- 문제: HR 데이터를 활용하여 '부서별 급여 통계 View'를 제공하는 일반 View를 생성하고 조회하십시오.
-- 조건: 지정된 이름의 일반 View를 생성한 뒤 결과를 조회하십시오.
-- TODO: 아래에 SQL을 작성하십시오.

CREATE OR REPLACE VIEW hr.v_department_salary_stats AS
SELECT
    COALESCE(d.department_name, '부서 미배정') AS "부서명(department_name)",
    COUNT(*) AS "부서별 사원 수",
    SUM(e.salary) AS "부서별 급여 합계",
    ROUND(AVG(e.salary), 2) AS "부서별 평균 급여",
    MIN(e.salary) AS "부서별 최소 급여",
    MAX(e.salary) AS "부서별 최대 급여"
FROM employees e
LEFT JOIN departments d ON d.department_id = e.department_id
GROUP BY e.department_id, d.department_name;

-- 조회
SELECT * FROM hr.v_department_salary_stats
ORDER BY "부서별 급여 합계" DESC;

-- [VIEW 4] 관리자별 부하 직원 View
-- 문제: HR 데이터를 활용하여 '관리자별 부하 직원 View'를 제공하는 일반 View를 생성하고 조회하십시오.
-- 조건: 지정된 이름의 일반 View를 생성한 뒤 결과를 조회하십시오.
-- TODO: 아래에 SQL을 작성하십시오.

CREATE OR REPLACE VIEW hr.v_manager_subordinates AS
SELECT
    m.employee_id AS "관리자 사번(employee_id)",
    concat_ws(' ', m.first_name, m.last_name) AS "관리자명(manager_name)",
    COUNT(*) AS "관리자별 부하 직원 수"
FROM employees e
INNER JOIN employees m ON m.employee_id = e.manager_id
GROUP BY m.employee_id, m.first_name, m.last_name;

-- 조회
SELECT * FROM hr.v_manager_subordinates
ORDER BY "관리자별 부하 직원 수" DESC, "관리자 사번(employee_id)";

-- [VIEW 5] 직무 이력 상세 View
-- 문제: HR 데이터를 활용하여 '직무 이력 상세 View'를 제공하는 일반 View를 생성하고 조회하십시오.
-- 조건: 지정된 이름의 일반 View를 생성한 뒤 결과를 조회하십시오.
-- TODO: 아래에 SQL을 작성하십시오.

CREATE OR REPLACE VIEW hr.v_job_history_detail AS
SELECT
    jh.employee_id AS "사번(employee_id)",
    concat_ws(' ', e.first_name, e.last_name) AS "사원명(employee_name)",
    jh.start_date AS "시작일(start_date)",
    jh.end_date AS "종료일(end_date)",
    (jh.end_date - jh.start_date) AS "재직 일수(days)",
    j.job_title AS "직무명(job_title)",
    COALESCE(d.department_name, '부서 미배정') AS "부서명(department_name)"
FROM job_history jh
INNER JOIN employees e ON e.employee_id = jh.employee_id
INNER JOIN jobs j ON j.job_id = jh.job_id
LEFT JOIN departments d ON d.department_id = jh.department_id;

-- 조회
SELECT * FROM hr.v_job_history_detail
ORDER BY "사번(employee_id)", "시작일(start_date)";

-- ============================================================================
-- F. MATERIALIZED VIEW 종합실습 (2문제)
-- ============================================================================

-- [Materialized View 1] 부서별 급여 요약
-- 문제: HR 데이터를 활용하여 '부서별 급여 요약' Materialized View를 생성하고 조회하십시오.
-- 조건: Materialized View와 UNIQUE INDEX를 생성한 뒤 결과를 조회하십시오.
-- TODO: 아래에 SQL을 작성하십시오.

DROP MATERIALIZED VIEW IF EXISTS hr.mv_department_salary CASCADE;

CREATE MATERIALIZED VIEW hr.mv_department_salary AS
SELECT
    e.department_id AS department_id,
    COALESCE(d.department_name, '부서 미배정') AS department_name,
    COUNT(*) AS employee_count,
    SUM(e.salary) AS total_salary,
    ROUND(AVG(e.salary), 2) AS avg_salary,
    MIN(e.salary) AS min_salary,
    MAX(e.salary) AS max_salary
FROM employees e
LEFT JOIN departments d ON d.department_id = e.department_id
GROUP BY e.department_id, d.department_name
WITH DATA;

-- UNIQUE INDEX (CONCURRENTLY 갱신을 위해 필요)
CREATE UNIQUE INDEX idx_mv_department_salary
    ON hr.mv_department_salary (department_id);

-- 조회
SELECT
    department_name AS "부서명(department_name)",
    employee_count AS "부서별 사원 수",
    total_salary AS "부서별 급여 합계",
    avg_salary AS "부서별 평균 급여",
    min_salary AS "부서별 최소 급여",
    max_salary AS "부서별 최대 급여"
FROM hr.mv_department_salary
ORDER BY total_salary DESC;

-- [Materialized View 2] 국가별 사원 현황
-- 문제: HR 데이터를 활용하여 '국가별 사원 현황' Materialized View를 생성하고 조회하십시오.
-- 조건: Materialized View와 UNIQUE INDEX를 생성한 뒤 결과를 조회하십시오.
-- TODO: 아래에 SQL을 작성하십시오.

DROP MATERIALIZED VIEW IF EXISTS hr.mv_country_employees CASCADE;

CREATE MATERIALIZED VIEW hr.mv_country_employees AS
SELECT
    c.country_id AS country_id,
    c.country_name AS country_name,
    COUNT(*) AS employee_count,
    ROUND(AVG(e.salary), 2) AS avg_salary
FROM employees e
INNER JOIN departments d ON d.department_id = e.department_id
INNER JOIN locations l ON l.location_id = d.location_id
INNER JOIN countries c ON c.country_id = l.country_id
GROUP BY c.country_id, c.country_name
WITH DATA;

-- UNIQUE INDEX
CREATE UNIQUE INDEX idx_mv_country_employees
    ON hr.mv_country_employees (country_id);

-- 조회
SELECT
    country_name AS "국가명(country_name)",
    employee_count AS "국가별 사원 수",
    avg_salary AS "국가별 평균 급여"
FROM hr.mv_country_employees
ORDER BY employee_count DESC, country_name;

-- ============================================================================
-- G. CUBE 종합실습 (2문제)
-- ============================================================================

-- [CUBE 1] 부서와 직무별 급여 합계
-- 문제: HR 데이터를 활용하여 '부서와 직무별 급여 합계' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: GROUP BY CUBE와 GROUPING 함수를 사용해 상세·소계·전체 합계를 구하십시오.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    CASE WHEN GROUPING(d.department_name) = 1 THEN '[전체 부서]'
         ELSE COALESCE(d.department_name, '부서 미배정')
    END AS "부서명(department_name)",
    CASE WHEN GROUPING(j.job_title) = 1 THEN '[전체 직무]'
         ELSE j.job_title
    END AS "직무명(job_title)",
    SUM(e.salary) AS "급여 합계",
    COUNT(*) AS "사원 수",
    CASE
        WHEN GROUPING(d.department_name) = 1 AND GROUPING(j.job_title) = 1 THEN '전체 합계'
        WHEN GROUPING(d.department_name) = 1 THEN '직무별 소계'
        WHEN GROUPING(j.job_title) = 1 THEN '부서별 소계'
        ELSE '상세'
    END AS "집계 수준"
FROM employees e
LEFT JOIN departments d ON d.department_id = e.department_id
INNER JOIN jobs j ON j.job_id = e.job_id
GROUP BY CUBE (d.department_name, j.job_title)
ORDER BY
    GROUPING(d.department_name),
    d.department_name,
    GROUPING(j.job_title),
    j.job_title;

-- [CUBE 2] 입사 연도와 커미션 수령 여부별 인원수
-- 문제: HR 데이터를 활용하여 '입사 연도와 커미션 수령 여부별 인원수' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: GROUP BY CUBE와 GROUPING 함수를 사용해 상세·소계·전체 합계를 구하십시오.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    CASE WHEN GROUPING(EXTRACT(YEAR FROM e.hire_date)) = 1 THEN '[전체 연도]'
         ELSE EXTRACT(YEAR FROM e.hire_date)::text
    END AS "입사 연도(hire_year)",
    CASE WHEN GROUPING(CASE WHEN e.commission_pct IS NOT NULL THEN '수령' ELSE '미수령' END) = 1 THEN '[전체]'
         ELSE CASE WHEN e.commission_pct IS NOT NULL THEN '수령' ELSE '미수령' END
    END AS "커미션 수령 여부",
    COUNT(*) AS "인원수",
    CASE
        WHEN GROUPING(EXTRACT(YEAR FROM e.hire_date)) = 1
         AND GROUPING(CASE WHEN e.commission_pct IS NOT NULL THEN '수령' ELSE '미수령' END) = 1 THEN '전체 합계'
        WHEN GROUPING(EXTRACT(YEAR FROM e.hire_date)) = 1 THEN '커미션별 소계'
        WHEN GROUPING(CASE WHEN e.commission_pct IS NOT NULL THEN '수령' ELSE '미수령' END) = 1 THEN '연도별 소계'
        ELSE '상세'
    END AS "집계 수준"
FROM employees e
GROUP BY CUBE (
    EXTRACT(YEAR FROM e.hire_date),
    CASE WHEN e.commission_pct IS NOT NULL THEN '수령' ELSE '미수령' END
)
ORDER BY
    GROUPING(EXTRACT(YEAR FROM e.hire_date)),
    EXTRACT(YEAR FROM e.hire_date),
    GROUPING(CASE WHEN e.commission_pct IS NOT NULL THEN '수령' ELSE '미수령' END);

-- ============================================================================
-- H. ROLLUP 종합실습 (2문제)
-- ============================================================================

-- [ROLLUP 1] 국가 → 도시 → 부서 계층별 급여 합계
-- 문제: HR 데이터를 활용하여 '국가 → 도시 → 부서 계층별 급여 합계' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: GROUP BY ROLLUP과 GROUPING 함수를 사용해 계층별 상세·소계·전체 합계를 구하십시오.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    CASE WHEN GROUPING(c.country_name) = 1 THEN '[전체 국가]'
         ELSE c.country_name
    END AS "국가명(country_name)",
    CASE WHEN GROUPING(l.city) = 1 THEN '[전체 도시]'
         ELSE l.city
    END AS "도시(city)",
    CASE WHEN GROUPING(d.department_name) = 1 THEN '[전체 부서]'
         ELSE d.department_name
    END AS "부서명(department_name)",
    SUM(e.salary) AS "급여 합계",
    COUNT(*) AS "사원 수",
    CASE
        WHEN GROUPING(c.country_name) = 1 THEN '전체 합계'
        WHEN GROUPING(l.city) = 1 THEN '국가별 소계'
        WHEN GROUPING(d.department_name) = 1 THEN '도시별 소계'
        ELSE '상세'
    END AS "집계 수준"
FROM employees e
INNER JOIN departments d ON d.department_id = e.department_id
INNER JOIN locations l ON l.location_id = d.location_id
INNER JOIN countries c ON c.country_id = l.country_id
GROUP BY ROLLUP (c.country_name, l.city, d.department_name)
ORDER BY
    GROUPING(c.country_name),
    c.country_name,
    GROUPING(l.city),
    l.city,
    GROUPING(d.department_name),
    d.department_name;

-- [ROLLUP 2] 입사 연도 → 부서별 인원 및 급여 합계
-- 문제: HR 데이터를 활용하여 '입사 연도 → 부서별 인원 및 급여 합계' 결과를 조회하는 SQL을 작성하십시오.
-- 조건: GROUP BY ROLLUP과 GROUPING 함수를 사용해 계층별 상세·소계·전체 합계를 구하십시오.
-- TODO: 아래에 SQL을 작성하십시오.

SELECT
    CASE WHEN GROUPING(EXTRACT(YEAR FROM e.hire_date)) = 1 THEN '[전체 연도]'
         ELSE EXTRACT(YEAR FROM e.hire_date)::text
    END AS "입사 연도(hire_year)",
    CASE WHEN GROUPING(d.department_name) = 1 THEN '[전체 부서]'
         ELSE COALESCE(d.department_name, '부서 미배정')
    END AS "부서명(department_name)",
    COUNT(*) AS "인원수",
    SUM(e.salary) AS "급여 합계",
    CASE
        WHEN GROUPING(EXTRACT(YEAR FROM e.hire_date)) = 1 THEN '전체 합계'
        WHEN GROUPING(d.department_name) = 1 THEN '연도별 소계'
        ELSE '상세'
    END AS "집계 수준"
FROM employees e
LEFT JOIN departments d ON d.department_id = e.department_id
GROUP BY ROLLUP (EXTRACT(YEAR FROM e.hire_date), d.department_name)
ORDER BY
    GROUPING(EXTRACT(YEAR FROM e.hire_date)),
    EXTRACT(YEAR FROM e.hire_date),
    GROUPING(d.department_name),
    d.department_name;






