ALTER TABLE characters
    ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0;

WITH ordered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS row_number
    FROM characters
)
UPDATE characters
SET display_order = ordered.row_number
FROM ordered
WHERE characters.id = ordered.id
  AND characters.display_order = 0;

CREATE INDEX IF NOT EXISTS idx_characters_display_order
    ON characters(display_order, id);
