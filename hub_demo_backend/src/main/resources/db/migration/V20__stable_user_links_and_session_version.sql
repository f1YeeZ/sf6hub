ALTER TABLE users
    ADD COLUMN IF NOT EXISTS token_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE combo_likes ADD COLUMN IF NOT EXISTS user_id BIGINT;
UPDATE combo_likes item
SET user_id = app_user.id
FROM users app_user
WHERE item.user_id IS NULL AND item.username = app_user.username;
ALTER TABLE combo_likes
    ADD CONSTRAINT fk_combo_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
CREATE UNIQUE INDEX IF NOT EXISTS ux_combo_likes_combo_user ON combo_likes(combo_id, user_id);
CREATE INDEX IF NOT EXISTS idx_combo_likes_user ON combo_likes(user_id);

ALTER TABLE combo_favorites ADD COLUMN IF NOT EXISTS user_id BIGINT;
UPDATE combo_favorites item
SET user_id = app_user.id
FROM users app_user
WHERE item.user_id IS NULL AND item.username = app_user.username;
ALTER TABLE combo_favorites
    ADD CONSTRAINT fk_combo_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
CREATE UNIQUE INDEX IF NOT EXISTS ux_combo_favorites_combo_user ON combo_favorites(combo_id, user_id);
CREATE INDEX IF NOT EXISTS idx_combo_favorites_user ON combo_favorites(user_id);

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS user_id BIGINT;
UPDATE notifications item
SET user_id = app_user.id
FROM users app_user
WHERE item.user_id IS NULL AND item.username = app_user.username;
ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON notifications(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_combos_admin_filter
    ON combos(status, character_id, control_type, submitted_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_reports_combo_status
    ON reports(target_type, status, target_id);
CREATE INDEX IF NOT EXISTS idx_visit_logs_dedupe
    ON visit_logs(visitor_id, path, created_at DESC);
