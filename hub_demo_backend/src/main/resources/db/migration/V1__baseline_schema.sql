CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(256) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'user',
    admin_permissions TEXT NOT NULL DEFAULT '',
    banned BOOLEAN NOT NULL DEFAULT FALSE,
    ban_reason TEXT,
    banned_until TIMESTAMP NULL,
    last_login_at TIMESTAMP NULL,
    avatar TEXT,
    bio TEXT,
    follower_count BIGINT NOT NULL DEFAULT 0,
    following_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS characters (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    avatar TEXT,
    description TEXT,
    archetype VARCHAR(64),
    official_slug VARCHAR(64),
    official_frame_url TEXT,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_characters_official_slug
    ON characters(official_slug)
    WHERE official_slug IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_characters_display_order ON characters(display_order, id);

CREATE TABLE IF NOT EXISTS frame_data (
    id BIGSERIAL PRIMARY KEY,
    character_id BIGINT NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    move_name VARCHAR(160) NOT NULL,
    startup TEXT,
    active TEXT,
    recovery TEXT,
    on_block TEXT,
    on_hit TEXT,
    cancel TEXT,
    damage TEXT,
    combo_scaling TEXT,
    drive_gain_on_hit TEXT,
    drive_loss_on_block TEXT,
    drive_loss_on_punish_counter TEXT,
    super_art_gain TEXT,
    properties VARCHAR(160),
    miscellaneous TEXT,
    source_url TEXT,
    source_character_slug VARCHAR(64),
    source_lang VARCHAR(16),
    source_synced_at TIMESTAMP,
    display_order INTEGER
);

CREATE INDEX IF NOT EXISTS idx_frame_data_character_order ON frame_data(character_id, display_order, id);
CREATE INDEX IF NOT EXISTS idx_frame_data_source_character ON frame_data(source_character_slug);
CREATE INDEX IF NOT EXISTS idx_frame_data_character_move ON frame_data(character_id, move_name);

CREATE TABLE IF NOT EXISTS combos (
    id BIGSERIAL PRIMARY KEY,
    character_id BIGINT NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    author_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    author VARCHAR(64),
    starter VARCHAR(64) NOT NULL,
    route TEXT NOT NULL,
    combo_text TEXT NOT NULL,
    damage INTEGER NOT NULL DEFAULT 0,
    drive_cost INTEGER NOT NULL DEFAULT 0,
    sa_cost INTEGER NOT NULL DEFAULT 0,
    advantage_frames VARCHAR(50),
    difficulty VARCHAR(32),
    corner_only BOOLEAN NOT NULL DEFAULT FALSE,
    control_type VARCHAR(16) NOT NULL DEFAULT 'classic',
    type VARCHAR(32),
    video_url TEXT,
    training_notes TEXT,
    rejection_reason TEXT,
    difficulty_note TEXT,
    difficulty_calibrated BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_by VARCHAR(40) DEFAULT '',
    reviewed_at TIMESTAMP NULL,
    likes INTEGER NOT NULL DEFAULT 0,
    favorites INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'approved',
    created_at DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE INDEX IF NOT EXISTS idx_combo_status ON combos(status);

CREATE TABLE IF NOT EXISTS combo_likes (
    id BIGSERIAL PRIMARY KEY,
    combo_id BIGINT NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
    username VARCHAR(64) NOT NULL,
    UNIQUE (combo_id, username)
);

CREATE TABLE IF NOT EXISTS combo_favorites (
    id BIGSERIAL PRIMARY KEY,
    combo_id BIGINT NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
    username VARCHAR(64) NOT NULL,
    UNIQUE (combo_id, username)
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64),
    type VARCHAR(32) NOT NULL,
    title VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    target_url TEXT,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    reporter VARCHAR(64),
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(128),
    detail TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    handler_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    handler VARCHAR(64),
    resolution TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    handled_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS user_follows (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_user_follows_not_self CHECK (follower_id <> following_id),
    CONSTRAINT uk_user_follows_pair UNIQUE (follower_id, following_id)
);

CREATE INDEX IF NOT EXISTS idx_user_follows_follower ON user_follows(follower_id);
CREATE INDEX IF NOT EXISTS idx_user_follows_following ON user_follows(following_id);

CREATE TABLE IF NOT EXISTS battle_rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(24) NOT NULL,
    password VARCHAR(16) DEFAULT '',
    game_room_code VARCHAR(32) NOT NULL DEFAULT '',
    max_players INTEGER NOT NULL DEFAULT 2,
    status VARCHAR(20) NOT NULL DEFAULT 'waiting',
    abnormal BOOLEAN NOT NULL DEFAULT FALSE,
    abnormal_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS battle_room_players (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES battle_rooms(id) ON DELETE CASCADE,
    user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    name VARCHAR(40) NOT NULL,
    character VARCHAR(40) NOT NULL,
    avatar TEXT NOT NULL,
    slot INTEGER NOT NULL,
    UNIQUE (room_id, slot)
);

CREATE INDEX IF NOT EXISTS idx_battle_room_players_room_id ON battle_room_players(room_id);

CREATE TABLE IF NOT EXISTS battle_room_messages (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES battle_rooms(id) ON DELETE CASCADE,
    user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    author VARCHAR(40) NOT NULL,
    avatar TEXT DEFAULT '',
    content VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_battle_room_messages_room_id ON battle_room_messages(room_id);

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

CREATE INDEX IF NOT EXISTS idx_frame_sync_logs_created_at ON frame_sync_logs(created_at);

CREATE TABLE IF NOT EXISTS frame_change_history (
    id BIGSERIAL PRIMARY KEY,
    frame_id BIGINT NULL,
    character_id BIGINT NULL,
    move_name VARCHAR(120) NOT NULL DEFAULT '',
    action VARCHAR(30) NOT NULL,
    admin_name VARCHAR(40) NOT NULL DEFAULT '',
    detail TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_frame_change_history_created_at ON frame_change_history(created_at);

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

CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_created_at ON admin_audit_logs(created_at);

CREATE TABLE IF NOT EXISTS announcements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    level VARCHAR(20) NOT NULL DEFAULT 'info',
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(40) NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
