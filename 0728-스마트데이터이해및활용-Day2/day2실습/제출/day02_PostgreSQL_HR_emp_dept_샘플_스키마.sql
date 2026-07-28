-- ============================================================
-- PostgreSQL 학습용 HR 샘플 스키마
-- Oracle HR 예제의 구조와 사용 방식을 PostgreSQL에 맞게 재구성
-- 대상 database: skala_db
-- 대상 SCHEMA: hr
-- PostgreSQL 13+ / DBeaver에서 전체 선택 후 한 번에 실행 가능
-- 주의: 기존 hr 스키마와 그 안의 객체를 모두 삭제하고 다시 생성합니다.
-- ============================================================

BEGIN;

DROP SCHEMA IF EXISTS hr CASCADE;
CREATE SCHEMA IF NOT EXISTS hr;
SET search_path TO hr, public;

-- 1. 권역
CREATE TABLE hr.regions (
    region_id   integer      PRIMARY KEY,
    region_name varchar(25)  NOT NULL UNIQUE
);

-- 2. 국가
CREATE TABLE hr.countries (
    country_id   char(2)      PRIMARY KEY,
    country_name varchar(40)  NOT NULL,
    region_id    integer      NOT NULL,
    CONSTRAINT fk_countries_region
        FOREIGN KEY (region_id) REFERENCES hr.regions(region_id)
);

-- 3. 사업장 위치
CREATE TABLE hr.locations (
    location_id    integer      PRIMARY KEY,
    street_address varchar(80),
    postal_code    varchar(20),
    city           varchar(40)  NOT NULL,
    state_province varchar(40),
    country_id     char(2)      NOT NULL,
    CONSTRAINT fk_locations_country
        FOREIGN KEY (country_id) REFERENCES hr.countries(country_id)
);

-- 4. 직무
CREATE TABLE hr.jobs (
    job_id     varchar(10)    PRIMARY KEY,
    job_title  varchar(35)    NOT NULL,
    min_salary numeric(10, 2) NOT NULL,
    max_salary numeric(10, 2) NOT NULL,
    CONSTRAINT ck_jobs_salary
        CHECK (min_salary >= 0 AND max_salary >= min_salary)
);

-- 5. 부서
-- manager_id의 FK는 employees 생성 후 추가합니다.
CREATE TABLE hr.departments (
    department_id   integer      PRIMARY KEY,
    department_name varchar(30)  NOT NULL UNIQUE,
    manager_id      integer,
    location_id     integer,
    CONSTRAINT fk_departments_location
        FOREIGN KEY (location_id) REFERENCES hr.locations(location_id)
);

-- 6. 사원
CREATE TABLE hr.employees (
    employee_id    integer       PRIMARY KEY,
    first_name     varchar(20),
    last_name      varchar(25)   NOT NULL,
    email          varchar(50)   NOT NULL UNIQUE,
    phone_number   varchar(20),
    hire_date      date          NOT NULL,
    job_id         varchar(10)   NOT NULL,
    salary         numeric(10,2) NOT NULL,
    commission_pct numeric(4,2),
    manager_id     integer,
    department_id  integer,
    CONSTRAINT ck_employees_salary CHECK (salary > 0),
    CONSTRAINT ck_employees_commission
        CHECK (commission_pct IS NULL OR commission_pct BETWEEN 0 AND 1),
    CONSTRAINT fk_employees_job
        FOREIGN KEY (job_id) REFERENCES hr.jobs(job_id),
    CONSTRAINT fk_employees_manager
        FOREIGN KEY (manager_id) REFERENCES hr.employees(employee_id),
    CONSTRAINT fk_employees_department
        FOREIGN KEY (department_id) REFERENCES hr.departments(department_id)
);

-- 부서장 FK: departments와 employees의 상호 참조 때문에 나중에 추가
ALTER TABLE hr.departments
    ADD CONSTRAINT fk_departments_manager
    FOREIGN KEY (manager_id) REFERENCES hr.employees(employee_id);

-- 7. 직무 이력
CREATE TABLE hr.job_history (
    employee_id   integer     NOT NULL,
    start_date    date        NOT NULL,
    end_date      date        NOT NULL,
    job_id        varchar(10) NOT NULL,
    department_id integer,
    CONSTRAINT pk_job_history PRIMARY KEY (employee_id, start_date),
    CONSTRAINT ck_job_history_dates CHECK (end_date > start_date),
    CONSTRAINT fk_job_history_employee
        FOREIGN KEY (employee_id) REFERENCES hr.employees(employee_id),
    CONSTRAINT fk_job_history_job
        FOREIGN KEY (job_id) REFERENCES hr.jobs(job_id),
    CONSTRAINT fk_job_history_department
        FOREIGN KEY (department_id) REFERENCES hr.departments(department_id)
);

-- ============================================================
-- 샘플 데이터
-- ============================================================

INSERT INTO regions (region_id, region_name) VALUES
    (1, 'Europe'),
    (2, 'Americas'),
    (3, 'Asia'),
    (4, 'Middle East and Africa');

INSERT INTO countries (country_id, country_name, region_id) VALUES
    ('CA', 'Canada', 2), ('DE', 'Germany', 1),
    ('FR', 'France', 1), ('IT', 'Italy', 1),
    ('JP', 'Japan', 3), ('KR', 'South Korea', 3),
    ('UK', 'United Kingdom', 1), ('US', 'United States', 2),
    ('BR', 'Brazil', 2), ('AU', 'Australia', 3),
    ('IN', 'India', 3), ('SG', 'Singapore', 3),
    ('EG', 'Egypt', 4), ('ZA', 'South Africa', 4);

INSERT INTO locations
    (location_id, street_address, postal_code, city, state_province, country_id)
VALUES
    (1000, '1297 Via Cola di Rie', '00989', 'Rome', NULL, 'IT'),
    (1100, '93091 Calle della Testa', '10934', 'Venice', NULL, 'IT'),
    (1400, '2014 Jabberwocky Rd', '26192', 'Southlake', 'Texas', 'US'),
    (1500, '2011 Interiors Blvd', '99236', 'South San Francisco', 'California', 'US'),
    (1700, '2004 Charade Rd', '98199', 'Seattle', 'Washington', 'US'),
    (1800, '147 Spadina Ave', 'M5V 2L7', 'Toronto', 'Ontario', 'CA'),
    (2000, '40-5 Dohwa-dong', '04167', 'Seoul', 'Seoul', 'KR'),
    (2100, '12 Teheran-ro', '06134', 'Seoul', 'Seoul', 'KR'),
    (2400, '8204 Arthur St', NULL, 'London', NULL, 'UK'),
    (2500, 'Magdalen Centre', 'OX9 9ZB', 'Oxford', 'Oxford', 'UK'),
    (2700, 'Schwanthalerstr. 7031', '80925', 'Munich', 'Bavaria', 'DE'),
    (2800, 'Marunouchi 1-9-1', '100-0005', 'Tokyo', 'Tokyo', 'JP');

INSERT INTO jobs (job_id, job_title, min_salary, max_salary) VALUES
    ('AD_PRES',  'President',                    20000, 40000),
    ('AD_VP',    'Administration Vice President',15000, 30000),
    ('AD_ASST',  'Administration Assistant',      3000,  6000),
    ('FI_MGR',   'Finance Manager',                8200, 16000),
    ('FI_ACCOUNT','Accountant',                    4200,  9000),
    ('AC_MGR',   'Accounting Manager',             8200, 16000),
    ('AC_ACCOUNT','Public Accountant',              4200,  9000),
    ('SA_MAN',   'Sales Manager',                  10000, 20000),
    ('SA_REP',   'Sales Representative',            6000, 12000),
    ('PU_MAN',   'Purchasing Manager',              8000, 15000),
    ('PU_CLERK', 'Purchasing Clerk',                2500,  5500),
    ('ST_MAN',   'Stock Manager',                   5500,  8500),
    ('ST_CLERK', 'Stock Clerk',                     2000,  5000),
    ('SH_CLERK', 'Shipping Clerk',                  2500,  5500),
    ('IT_PROG',  'Programmer',                      4000, 10000),
    ('MK_MAN',   'Marketing Manager',               9000, 15000),
    ('MK_REP',   'Marketing Representative',        4000,  9000),
    ('HR_REP',   'Human Resources Representative',  4000,  9000),
    ('PR_REP',   'Public Relations Representative', 4500, 10500);

-- 부서를 먼저 만들되 manager_id는 NULL로 두고 사원 입력 후 갱신
INSERT INTO departments
    (department_id, department_name, manager_id, location_id)
VALUES
    (10,  'Administration', NULL, 1700),
    (20,  'Marketing',      NULL, 1800),
    (30,  'Purchasing',     NULL, 1700),
    (40,  'Human Resources',NULL, 2400),
    (50,  'Shipping',       NULL, 1500),
    (60,  'IT',             NULL, 1400),
    (70,  'Public Relations',NULL, 2700),
    (80,  'Sales',          NULL, 2500),
    (90,  'Executive',      NULL, 1700),
    (100, 'Finance',        NULL, 1700),
    (110, 'Accounting',     NULL, 1700),
    (120, 'R&D Korea',      NULL, 2000);

-- 최고경영자 → 관리자 → 일반 사원 순서로 입력하여 자기참조 FK 충족
INSERT INTO employees VALUES
    (100, 'Steven',  'King',      'SKING',    '515.123.4567', DATE '2003-06-17', 'AD_PRES', 24000, NULL, NULL, 90);

INSERT INTO employees VALUES
    (101, 'Neena',   'Kochhar',   'NKOCHHAR', '515.123.4568', DATE '2005-09-21', 'AD_VP',   17000, NULL, 100, 90),
    (102, 'Lex',     'De Haan',   'LDEHAAN',  '515.123.4569', DATE '2001-01-13', 'AD_VP',   17000, NULL, 100, 90);

INSERT INTO employees VALUES
    (103, 'Alexander','Hunold',   'AHUNOLD',  '590.423.4567', DATE '2006-01-03', 'IT_PROG',  9000, NULL, 102, 60),
    (108, 'Nancy',   'Greenberg', 'NGREENBE', '515.124.4569', DATE '2002-08-17', 'FI_MGR',   12000, NULL, 101, 100),
    (114, 'Den',     'Raphaely',  'DRAPHEAL', '515.127.4561', DATE '2002-12-07', 'PU_MAN',   11000, NULL, 100, 30),
    (120, 'Matthew', 'Weiss',     'MWEISS',   '650.123.1234', DATE '2004-07-18', 'ST_MAN',    8000, NULL, 100, 50),
    (121, 'Adam',    'Fripp',     'AFRIPP',   '650.123.2234', DATE '2005-04-10', 'ST_MAN',    8200, NULL, 100, 50),
    (122, 'Payam',   'Kaufling',  'PKAUFLIN', '650.123.3234', DATE '2003-05-01', 'ST_MAN',    7900, NULL, 100, 50),
    (145, 'John',    'Russell',   'JRUSSEL',  NULL,           DATE '2004-10-01', 'SA_MAN',   14000, 0.40, 100, 80),
    (146, 'Karen',   'Partners',  'KPARTNER', NULL,           DATE '2005-01-05', 'SA_MAN',   13500, 0.30, 100, 80),
    (147, 'Alberto', 'Errazuriz', 'AERRAZUR', NULL,           DATE '2005-03-10', 'SA_MAN',   12000, 0.30, 100, 80),
    (148, 'Gerald',  'Cambrault', 'GCAMBRAU', NULL,           DATE '2007-10-15', 'SA_MAN',   11000, 0.30, 100, 80),
    (149, 'Eleni',   'Zlotkey',   'EZLOTKEY', NULL,           DATE '2008-01-29', 'SA_MAN',   10500, 0.20, 100, 80),
    (200, 'Jennifer','Whalen',    'JWHALEN',  '515.123.4444', DATE '2003-09-17', 'AD_ASST',   4400, NULL, 101, 10),
    (201, 'Michael', 'Hartstein', 'MHARTSTE', '515.123.5555', DATE '2004-02-17', 'MK_MAN',   13000, NULL, 100, 20),
    (203, 'Susan',   'Mavris',    'SMAVRIS',  '515.123.7777', DATE '2002-06-07', 'HR_REP',    6500, NULL, 101, 40),
    (204, 'Hermann', 'Baer',      'HBAER',    '515.123.8888', DATE '2002-06-07', 'PR_REP',   10000, NULL, 101, 70),
    (205, 'Shelley', 'Higgins',   'SHIGGINS', '515.123.8080', DATE '2002-06-07', 'AC_MGR',   12000, NULL, 101, 110),
    (220, 'Minjun',  'Kim',       'MKIM',     '02.555.1001',  DATE '2019-03-04', 'IT_PROG',  10500, NULL, 102, 120);

INSERT INTO employees VALUES
    (104, 'Bruce',   'Ernst',     'BERNST',   '590.423.4568', DATE '2007-05-21', 'IT_PROG', 6000, NULL, 103, 60),
    (105, 'David',   'Austin',    'DAUSTIN',  '590.423.4569', DATE '2005-06-25', 'IT_PROG', 4800, NULL, 103, 60),
    (106, 'Valli',   'Pataballa', 'VPATABAL', '590.423.4560', DATE '2006-02-05', 'IT_PROG', 4800, NULL, 103, 60),
    (107, 'Diana',   'Lorentz',   'DLORENTZ', '590.423.5567', DATE '2007-02-07', 'IT_PROG', 4200, NULL, 103, 60),
    (109, 'Daniel',  'Faviet',    'DFAVIET',  '515.124.4169', DATE '2002-08-16', 'FI_ACCOUNT', 9000, NULL, 108, 100),
    (110, 'John',    'Chen',      'JCHEN',    '515.124.4269', DATE '2005-09-28', 'FI_ACCOUNT', 8200, NULL, 108, 100),
    (111, 'Ismael',  'Sciarra',   'ISCIARRA', '515.124.4369', DATE '2005-09-30', 'FI_ACCOUNT', 7700, NULL, 108, 100),
    (112, 'Jose Manuel','Urman',  'JMURMAN',  '515.124.4469', DATE '2006-03-07', 'FI_ACCOUNT', 7800, NULL, 108, 100),
    (113, 'Luis',    'Popp',      'LPOPP',    '515.124.4567', DATE '2007-12-07', 'FI_ACCOUNT', 6900, NULL, 108, 100),
    (115, 'Alexander','Khoo',     'AKHOO',    '515.127.4562', DATE '2003-05-18', 'PU_CLERK', 3100, NULL, 114, 30),
    (116, 'Shelli',  'Baida',     'SBAIDA',   '515.127.4563', DATE '2005-12-24', 'PU_CLERK', 2900, NULL, 114, 30),
    (117, 'Sigal',   'Tobias',    'STOBIAS',  '515.127.4564', DATE '2005-07-24', 'PU_CLERK', 2800, NULL, 114, 30),
    (118, 'Guy',     'Himuro',    'GHIMURO',  '515.127.4565', DATE '2006-11-15', 'PU_CLERK', 2600, NULL, 114, 30),
    (119, 'Karen',   'Colmenares','KCOLMENA', '515.127.4566', DATE '2007-08-10', 'PU_CLERK', 2500, NULL, 114, 30),
    (150, 'Peter',   'Tucker',    'PTUCKER',  NULL,           DATE '2005-01-30', 'SA_REP', 10000, 0.30, 145, 80),
    (151, 'David',   'Bernstein', 'DBERNSTE', NULL,           DATE '2005-03-24', 'SA_REP',  9500, 0.25, 145, 80),
    (152, 'Peter',   'Hall',      'PHALL',    NULL,           DATE '2005-08-20', 'SA_REP',  9000, 0.25, 145, 80),
    (153, 'Christopher','Olsen',  'COLSEN',   NULL,           DATE '2006-03-30', 'SA_REP',  8000, 0.20, 145, 80),
    (154, 'Nanette', 'Cambrault', 'NCAMBRAU', NULL,           DATE '2006-12-09', 'SA_REP',  7500, 0.20, 145, 80),
    (155, 'Oliver',  'Tuvault',   'OTUVAULT', NULL,           DATE '2007-11-23', 'SA_REP',  7000, 0.15, 145, 80),
    (202, 'Pat',     'Fay',       'PFAY',     '603.123.6666', DATE '2005-08-17', 'MK_REP',  6000, NULL, 201, 20),
    (206, 'William', 'Gietz',     'WGIETZ',   '515.123.8181', DATE '2002-06-07', 'AC_ACCOUNT',8300,NULL,205,110),
    (221, 'Seoyeon', 'Lee',       'SLEE',     '02.555.1002',  DATE '2020-07-13', 'IT_PROG',  8500, NULL, 220, 120),
    (222, 'Jihoon',  'Park',      'JPARK',    '02.555.1003',  DATE '2021-11-01', 'IT_PROG',  7800, NULL, 220, 120),
    (223, 'Yuna',    'Choi',      'YCHOI',    '02.555.1004',  DATE '2023-02-20', 'MK_REP',   6200, NULL, 201, NULL);

-- 부서장 지정
UPDATE departments
SET manager_id = CASE department_id
    WHEN 10  THEN 200 WHEN 20  THEN 201 WHEN 30  THEN 114
    WHEN 40  THEN 203 WHEN 50  THEN 120 WHEN 60  THEN 103
    WHEN 70  THEN 204 WHEN 80  THEN 145 WHEN 90  THEN 100
    WHEN 100 THEN 108 WHEN 110 THEN 205 WHEN 120 THEN 220
END;

INSERT INTO job_history
    (employee_id, start_date, end_date, job_id, department_id)
VALUES
    (101, DATE '1997-09-21', DATE '2001-10-27', 'AC_ACCOUNT', 110),
    (101, DATE '2001-10-28', DATE '2005-03-15', 'AC_MGR', 110),
    (102, DATE '1993-01-13', DATE '1998-07-24', 'IT_PROG', 60),
    (114, DATE '1998-03-24', DATE '1999-12-31', 'ST_CLERK', 50),
    (122, DATE '1999-01-01', DATE '1999-12-31', 'ST_CLERK', 50),
    (200, DATE '1995-09-17', DATE '2001-06-17', 'AD_ASST', 90),
    (201, DATE '1996-02-17', DATE '1999-12-19', 'MK_REP', 20),
    (220, DATE '2015-01-05', DATE '2019-03-03', 'IT_PROG', 60),
    (223, DATE '2021-01-04', DATE '2023-02-19', 'AD_ASST', 10);

-- 자주 사용하는 FK 열 인덱스(PostgreSQL은 FK 인덱스를 자동 생성하지 않음)
CREATE INDEX idx_countries_region       ON countries(region_id);
CREATE INDEX idx_locations_country      ON locations(country_id);
CREATE INDEX idx_departments_location   ON departments(location_id);
CREATE INDEX idx_departments_manager    ON departments(manager_id);
CREATE INDEX idx_employees_job          ON employees(job_id);
CREATE INDEX idx_employees_manager      ON employees(manager_id);
CREATE INDEX idx_employees_department   ON employees(department_id);
CREATE INDEX idx_job_history_job        ON job_history(job_id);
CREATE INDEX idx_job_history_department ON job_history(department_id);

-- 테이블 설명
COMMENT ON SCHEMA hr IS 'Oracle HR 구조를 참고한 PostgreSQL SQL 학습용 스키마';
COMMENT ON TABLE regions     IS '권역 정보';
COMMENT ON TABLE countries   IS '국가 정보';
COMMENT ON TABLE locations   IS '사업장 위치 정보';
COMMENT ON TABLE departments IS '부서 정보';
COMMENT ON TABLE jobs        IS '직무 및 급여 범위';
COMMENT ON TABLE employees   IS '사원 정보';
COMMENT ON TABLE job_history IS '사원의 과거 직무 이력';

COMMIT;

-- ============================================================
-- 설치 확인용 쿼리
-- ============================================================
SET search_path TO hr, public;

SELECT 'regions' AS table_name, COUNT(*) AS row_count FROM regions
UNION ALL SELECT 'countries',   COUNT(*) FROM countries
UNION ALL SELECT 'locations',   COUNT(*) FROM locations
UNION ALL SELECT 'departments', COUNT(*) FROM departments
UNION ALL SELECT 'jobs',        COUNT(*) FROM jobs
UNION ALL SELECT 'employees',   COUNT(*) FROM employees
UNION ALL SELECT 'job_history', COUNT(*) FROM job_history
ORDER BY table_name;

-- 관계 확인: 사원, 직무, 부서, 관리자
SELECT
    e.employee_id,
    concat_ws(' ', e.first_name, e.last_name) AS employee_name,
    j.job_title,
    d.department_name,
    concat_ws(' ', m.first_name, m.last_name) AS manager_name,
    e.salary
FROM employees e
JOIN jobs j ON j.job_id = e.job_id
LEFT JOIN departments d ON d.department_id = e.department_id
LEFT JOIN employees m ON m.employee_id = e.manager_id
ORDER BY e.employee_id;
