-- 기존 단일 Agent 분석 결과를 Course의 agent_list JSON 구조로 마이그레이션한다.

ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS analysis_agent_list JSON NULL AFTER updated_at,
    ADD COLUMN IF NOT EXISTS analysis_risk_level VARCHAR(20) NULL AFTER analysis_agent_list,
    ADD COLUMN IF NOT EXISTS analysis_summary TEXT NULL AFTER analysis_risk_level;

SET @has_legacy_course_analysis = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'courses'
      AND column_name = 'analysis_agent_code'
);

SET @migrate_legacy_course_analysis = IF(
    @has_legacy_course_analysis > 0,
    'UPDATE courses
        SET analysis_agent_list = CASE
            WHEN analysis_agent_code IS NULL THEN NULL
            ELSE JSON_ARRAY(JSON_OBJECT(
                ''agentCode'', analysis_agent_code,
                ''permissions'', JSON_QUERY(COALESCE(analysis_permissions, ''[]''), ''$''),
                ''excludedPermissions'', JSON_QUERY(COALESCE(analysis_excluded_permissions, ''[]''), ''$'')
            ))
        END
      WHERE analysis_agent_list IS NULL',
    'SELECT 1'
);

PREPARE migrate_legacy_course_analysis_statement FROM @migrate_legacy_course_analysis;
EXECUTE migrate_legacy_course_analysis_statement;
DEALLOCATE PREPARE migrate_legacy_course_analysis_statement;

ALTER TABLE courses
    DROP COLUMN IF EXISTS analysis_agent_code,
    DROP COLUMN IF EXISTS analysis_agent_name,
    DROP COLUMN IF EXISTS analysis_fit_score,
    DROP COLUMN IF EXISTS analysis_permissions,
    DROP COLUMN IF EXISTS analysis_excluded_permissions;
