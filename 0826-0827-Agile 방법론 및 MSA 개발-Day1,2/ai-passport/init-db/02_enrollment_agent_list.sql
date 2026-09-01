-- 기존 단일 Agent 컬럼을 agent_list JSON 배열로 마이그레이션한다.
-- 신규 DB에서는 01_init.sql이 이미 agent_list를 생성하므로 안전하게 no-op 된다.

ALTER TABLE enrollments
    ADD COLUMN IF NOT EXISTS agent_list JSON NULL AFTER status;

SET @has_legacy_agent_columns = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'enrollments'
      AND column_name = 'agent_code'
);

SET @migrate_legacy_agents = IF(
    @has_legacy_agent_columns > 0,
    'UPDATE enrollments
        SET agent_list = CASE
            WHEN agent_code IS NULL THEN JSON_ARRAY()
            ELSE JSON_ARRAY(JSON_OBJECT(
                ''agentCode'', agent_code,
                ''permissions'', JSON_QUERY(COALESCE(permissions, ''[]''), ''$''),
                ''excludedPermissions'', JSON_QUERY(COALESCE(excluded_permissions, ''[]''), ''$'')
            ))
        END
      WHERE agent_list IS NULL',
    'SELECT 1'
);

PREPARE migrate_legacy_agents_statement FROM @migrate_legacy_agents;
EXECUTE migrate_legacy_agents_statement;
DEALLOCATE PREPARE migrate_legacy_agents_statement;

UPDATE enrollments
SET agent_list = JSON_ARRAY()
WHERE agent_list IS NULL;

ALTER TABLE enrollments
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'READY_FOR_APPROVAL'
        COMMENT 'READY_FOR_APPROVAL | ACTIVE | REJECTED | EXPIRED',
    MODIFY COLUMN agent_list JSON NOT NULL,
    DROP COLUMN IF EXISTS agent_code,
    DROP COLUMN IF EXISTS permissions,
    DROP COLUMN IF EXISTS excluded_permissions;
