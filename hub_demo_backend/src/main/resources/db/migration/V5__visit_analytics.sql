CREATE TABLE IF NOT EXISTS visit_logs (
    id BIGSERIAL PRIMARY KEY,
    visit_date DATE NOT NULL DEFAULT CURRENT_DATE,
    visitor_id VARCHAR(64) NOT NULL,
    user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    path TEXT NOT NULL,
    referrer TEXT,
    user_agent TEXT,
    ip_hash VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_visit_logs_date ON visit_logs(visit_date);
CREATE INDEX IF NOT EXISTS idx_visit_logs_visitor_date ON visit_logs(visitor_id, visit_date);
CREATE INDEX IF NOT EXISTS idx_visit_logs_path_date ON visit_logs(path, visit_date);
CREATE INDEX IF NOT EXISTS idx_visit_logs_created_at ON visit_logs(created_at);

CREATE TABLE IF NOT EXISTS daily_visit_stats (
    stat_date DATE PRIMARY KEY,
    uv BIGINT NOT NULL DEFAULT 0,
    pv BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
