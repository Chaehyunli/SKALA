-- ============================================================================
-- PostgreSQL day02 스키마 종합 실습
-- 대상 database: skala_db
-- 대상 스키마: day02
-- 주제: VIEW / MATERIALIZED VIEW / ROLLUP / CUBE
-- 구성: 문제 페이지 5문제 + 솔루션 페이지
-- 전제: day02_postgresql_app_students_enrollments_샘플_스키마.sql 샘플 스키마와 데이터가 먼저 생성되어 있어야 합니다.
-- PostgreSQL 13+ / DBeaver
-- ============================================================================

SET search_path TO day02, public;


-- ############################################################################
-- PAGE 1. 문제 페이지
-- ############################################################################

-- ============================================================================
-- A. VIEW 실습
-- ============================================================================

-- [문제 1] 학생 상세 정보 View
-- 학생 정보를 조회할 때마다 students와 majors를 반복해서 JOIN하지 않도록
-- v_student_detail이라는 일반 View를 생성하십시오.
--
-- [요구사항]
-- ① 모든 학생을 출력해야 합니다.
-- ② 학과가 배정되지 않은 학생도 결과에서 제외하면 안 됩니다.
-- ③ 학과 미배정 학생은 학과명에 '학과 미배정'을 표시하십시오.
-- ④ 다음 열을 출력하십시오.
--    student_id, student_no, student_name, email,
--    major_name, grade, created_at
-- ⑤ View 생성 후 student_no 오름차순으로 조회하십시오.
--
-- TODO: 아래에 SQL을 작성하십시오.



-- [문제 2] 과목별 성적 통계 View
-- 과목별 수강 현황과 성적 통계를 제공하는
-- v_course_score_stats라는 일반 View를 생성하십시오.
--
-- [요구사항]
-- ① 아직 수강생이 없는 과목도 출력할 수 있도록 작성하십시오.
-- ② 다음 열을 출력하십시오.
--    course_id, course_code, course_name, credit,
--    student_count, avg_score, max_score, min_score
-- ③ 평균 성적은 소수점 둘째 자리까지 표시하십시오.
-- ④ View 생성 후 평균 성적이 높은 순서로 조회하십시오.
-- ⑤ 수강생이 없는 과목은 조회 결과의 마지막에 배치하십시오.
--
-- TODO: 아래에 SQL을 작성하십시오.



-- ============================================================================
-- B. MATERIALIZED VIEW 실습
-- ============================================================================

-- [문제 3] 학과별 학생·수강·성적 요약 Materialized View
-- 반복적으로 사용하는 학과별 통계 결과를 실제로 저장하기 위해
-- mv_major_learning_summary라는 Materialized View를 생성하십시오.
--
-- [요구사항]
-- ① 학과가 배정되지 않은 학생도 별도의 그룹으로 집계하십시오.
-- ② 학과 식별값이 NULL이면 major_key에 0을 저장하십시오.
-- ③ 학과 미배정 그룹의 학과명은 '학과 미배정'으로 표시하십시오.
-- ④ 다음 열을 출력하십시오.
--    major_key, major_name, student_count,
--    enrollment_count, avg_score
-- ⑤ 평균 성적은 소수점 둘째 자리까지 표시하십시오.
-- ⑥ 동시 새로고침을 학습할 수 있도록 major_key에 UNIQUE INDEX를 생성하십시오.
-- ⑦ 생성 후 학생 수가 많은 순서로 조회하십시오.
-- ⑧ 일반 새로고침과 동시 새로고침 명령도 주석으로 작성하십시오.
--
-- TODO: 아래에 SQL을 작성하십시오.



-- ============================================================================
-- C. ROLLUP 실습
-- ============================================================================

-- [문제 4] 학과 → 학년별 학생 수 계층 집계
-- 학과와 학년별 학생 수, 학과 소계, 전체 합계를 한 번에 조회하십시오.
--
-- [요구사항]
-- ① ROLLUP을 사용하십시오.
-- ② 집계 계층은 학과 → 학년 순서입니다.
-- ③ 학과별·학년별 상세 행을 출력하십시오.
-- ④ 각 학과의 모든 학년을 합한 '학과 소계'를 출력하십시오.
-- ⑤ 마지막에는 모든 학생을 합한 '전체 합계'를 출력하십시오.
-- ⑥ 실제 NULL인 학과는 '학과 미배정'으로 표시하십시오.
-- ⑦ GROUPING을 사용해 실제 NULL과 소계·합계의 NULL을 구분하십시오.
-- ⑧ 다음 열을 출력하십시오.
--    major_name, grade, student_count
--
-- TODO: 아래에 SQL을 작성하십시오.



-- ============================================================================
-- D. CUBE 실습
-- ============================================================================

-- [문제 5] 학과와 과목별 수강 현황 다차원 집계
-- 학과와 과목의 가능한 모든 조합에 대한 수강 인원과 평균 성적을 조회하십시오.
--
-- [요구사항]
-- ① CUBE를 사용하십시오.
-- ② 다음 네 가지 수준의 결과가 모두 출력되어야 합니다.
--    · 학과별·과목별 상세 집계
--    · 학과별 전체 과목 소계
--    · 과목별 전체 학과 소계
--    · 전체 합계
-- ③ 학과 미배정 학생은 '학과 미배정'으로 표시하십시오.
-- ④ 소계·합계 행은 각각 '모든 학과', '모든 과목'으로 표시하십시오.
-- ⑤ GROUPING을 사용해 실제 NULL과 소계·합계의 NULL을 구분하십시오.
-- ⑥ 다음 열을 출력하십시오.
--    major_name, course_name, enrollment_count, avg_score
-- ⑦ 평균 성적은 소수점 둘째 자리까지 표시하십시오.
--
-- TODO: 아래에 SQL을 작성하십시오.


-- ############################################################################
-- PAGE 2. 솔루션 페이지
-- PAGE 1의 문제를 모두 해결한 후 확인하십시오.
-- ############################################################################

SET search_path TO app;

-- ============================================================================
-- [문제 1 정답] 학생 상세 정보 View
-- ============================================================================

DROP VIEW IF EXISTS v_student_detail CASCADE;

CREATE VIEW v_student_detail AS
SELECT
    s.id AS student_id,
    s.student_no,
    s.name AS student_name,
    s.email,
    COALESCE(m.name, '학과 미배정') AS major_name,
    s.grade,
    s.created_at
FROM students s
LEFT JOIN majors m
       ON m.id = s.major_id;

SELECT *
FROM v_student_detail
ORDER BY student_no;

-- 핵심 해설
-- students를 기준으로 LEFT JOIN하므로 학과가 없는 학생도 결과에 남습니다.
-- COALESCE는 NULL인 학과명을 '학과 미배정'으로 바꿉니다.


-- ============================================================================
-- [문제 2 정답] 과목별 성적 통계 View
-- ============================================================================

DROP VIEW IF EXISTS v_course_score_stats CASCADE;

CREATE VIEW v_course_score_stats AS
SELECT
    c.id AS course_id,
    c.course_code,
    c.name AS course_name,
    c.credit,
    COUNT(e.student_id) AS student_count,
    ROUND(AVG(e.score), 2) AS avg_score,
    MAX(e.score) AS max_score,
    MIN(e.score) AS min_score
FROM courses c
LEFT JOIN enrollments e
       ON e.course_id = c.id
GROUP BY
    c.id,
    c.course_code,
    c.name,
    c.credit;

SELECT *
FROM v_course_score_stats
ORDER BY avg_score DESC NULLS LAST, course_id;

-- 핵심 해설
-- COUNT(*)를 사용하면 수강생이 없는 과목도 1명으로 잘못 계산될 수 있습니다.
-- 따라서 NULL이 될 수 있는 e.student_id를 COUNT하여 실제 수강생만 계산합니다.


-- ============================================================================
-- [문제 3 정답] 학과별 학생·수강·성적 요약 Materialized View
-- ============================================================================

DROP MATERIALIZED VIEW IF EXISTS mv_major_learning_summary CASCADE;

CREATE MATERIALIZED VIEW mv_major_learning_summary AS
SELECT
    COALESCE(m.id, 0) AS major_key,
    COALESCE(m.name, '학과 미배정') AS major_name,
    COUNT(DISTINCT s.id) AS student_count,
    COUNT(e.course_id) AS enrollment_count,
    ROUND(AVG(e.score), 2) AS avg_score
FROM students s
LEFT JOIN majors m
       ON m.id = s.major_id
LEFT JOIN enrollments e
       ON e.student_id = s.id
GROUP BY
    COALESCE(m.id, 0),
    COALESCE(m.name, '학과 미배정')
WITH DATA;

CREATE UNIQUE INDEX ux_mv_major_learning_summary_major_key
    ON mv_major_learning_summary(major_key);

SELECT *
FROM mv_major_learning_summary
ORDER BY student_count DESC, major_key;

-- 원본 데이터 변경 후 일반 새로고침
-- REFRESH MATERIALIZED VIEW mv_major_learning_summary;

-- 조회 차단을 줄이는 동시 새로고침
-- UNIQUE INDEX가 필요하며 트랜잭션 블록 밖에서 실행하는 것이 안전합니다.
-- REFRESH MATERIALIZED VIEW CONCURRENTLY mv_major_learning_summary;

-- 핵심 해설
-- 일반 View와 달리 Materialized View는 조회 결과를 실제로 저장합니다.
-- 원본 데이터가 변경되어도 REFRESH 전까지 기존 결과가 유지됩니다.
-- 학생 한 명이 여러 과목을 수강하므로 학생 수는 COUNT(DISTINCT s.id)로 계산합니다.


-- ============================================================================
-- [문제 4 정답] 학과 → 학년별 학생 수 계층 집계
-- ============================================================================

SELECT
    CASE
        WHEN GROUPING(m.name) = 1 THEN '전체 합계'
        ELSE COALESCE(m.name, '학과 미배정')
    END AS major_name,
    CASE
        WHEN GROUPING(s.grade) = 1 THEN '학과 소계'
        ELSE s.grade::text || '학년'
    END AS grade,
    COUNT(s.id) AS student_count
FROM students s
LEFT JOIN majors m
       ON m.id = s.major_id
GROUP BY ROLLUP(m.name, s.grade)
ORDER BY
    GROUPING(m.name),
    m.name NULLS LAST,
    GROUPING(s.grade),
    s.grade NULLS LAST;

-- 핵심 해설
-- ROLLUP(학과, 학년)은 다음 세 단계의 결과를 생성합니다.
--   (학과, 학년) 상세 → (학과) 소계 → () 전체 합계
-- 학년만을 기준으로 한 소계는 만들지 않습니다.


-- ============================================================================
-- [문제 5 정답] 학과와 과목별 수강 현황 다차원 집계
-- ============================================================================

SELECT
    CASE
        WHEN GROUPING(m.name) = 1 THEN '모든 학과'
        ELSE COALESCE(m.name, '학과 미배정')
    END AS major_name,
    CASE
        WHEN GROUPING(c.name) = 1 THEN '모든 과목'
        ELSE c.name
    END AS course_name,
    COUNT(*) AS enrollment_count,
    ROUND(AVG(e.score), 2) AS avg_score
FROM enrollments e
JOIN students s
     ON s.id = e.student_id
LEFT JOIN majors m
       ON m.id = s.major_id
JOIN courses c
     ON c.id = e.course_id
GROUP BY CUBE(m.name, c.name)
ORDER BY
    GROUPING(m.name),
    m.name NULLS LAST,
    GROUPING(c.name),
    c.name NULLS LAST;

-- 핵심 해설
-- CUBE(학과, 과목)는 다음 네 가지 집계를 모두 생성합니다.
--   (학과, 과목), (학과), (과목), ()
-- ROLLUP과 달리 '과목별 모든 학과 소계'도 함께 생성되는 것이 핵심입니다.


-- ============================================================================
-- 생성된 View와 Materialized View 확인
-- ============================================================================

SELECT
    table_schema,
    table_name
FROM information_schema.views
WHERE table_schema = 'app'
ORDER BY table_name;

SELECT
    schemaname,
    matviewname,
    ispopulated
FROM pg_matviews
WHERE schemaname = 'app'
ORDER BY matviewname;

