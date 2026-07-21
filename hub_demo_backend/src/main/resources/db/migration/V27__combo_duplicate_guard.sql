ALTER TABLE combos
    ADD COLUMN IF NOT EXISTS dedupe_key VARCHAR(32);

CREATE UNIQUE INDEX IF NOT EXISTS ux_combos_active_dedupe_key
    ON combos(dedupe_key)
    WHERE dedupe_key IS NOT NULL
      AND status IN ('pending', 'manual_review', 'approved');
