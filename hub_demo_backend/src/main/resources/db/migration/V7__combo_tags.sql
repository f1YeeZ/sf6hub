ALTER TABLE combos
    ADD COLUMN IF NOT EXISTS tags TEXT NOT NULL DEFAULT '';

UPDATE combos
SET tags = type
WHERE (tags IS NULL OR tags = '')
  AND type IS NOT NULL
  AND type <> '';
