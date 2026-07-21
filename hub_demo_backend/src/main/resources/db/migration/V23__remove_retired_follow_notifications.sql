DELETE FROM notifications
WHERE type NOT IN (
    'combo_review',
    'combo_like',
    'combo_favorite',
    'feedback',
    'system'
);

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS ck_notifications_supported_type;

ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_supported_type CHECK (
        type IN (
            'combo_review',
            'combo_like',
            'combo_favorite',
            'feedback',
            'system'
        )
    );

DROP TABLE IF EXISTS user_follows;

ALTER TABLE users
    DROP COLUMN IF EXISTS follower_count,
    DROP COLUMN IF EXISTS following_count;
