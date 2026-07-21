CREATE TABLE IF NOT EXISTS frame_sync_logs (
    id BIGSERIAL PRIMARY KEY,
    source_name VARCHAR(80) NOT NULL DEFAULT '',
    source_url TEXT NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'success',
    total_characters INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    imported_count INTEGER NOT NULL DEFAULT 0,
    detail TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_frame_sync_logs_created_at
    ON frame_sync_logs(created_at);

CREATE TABLE IF NOT EXISTS frame_change_history (
    id BIGSERIAL PRIMARY KEY,
    frame_id BIGINT NULL,
    character_id BIGINT NULL,
    move_name TEXT NOT NULL DEFAULT '',
    action VARCHAR(30) NOT NULL,
    admin_name VARCHAR(40) NOT NULL DEFAULT '',
    detail TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE frame_change_history
    ALTER COLUMN move_name TYPE TEXT;

CREATE INDEX IF NOT EXISTS idx_frame_change_history_created_at
    ON frame_change_history(created_at);

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NULL,
    admin_name VARCHAR(40) NOT NULL DEFAULT '',
    action VARCHAR(60) NOT NULL,
    target_type VARCHAR(40) NOT NULL DEFAULT '',
    target_id BIGINT NULL,
    detail TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_created_at
    ON admin_audit_logs(created_at);

ALTER TABLE frame_data
    ALTER COLUMN move_name TYPE TEXT;
