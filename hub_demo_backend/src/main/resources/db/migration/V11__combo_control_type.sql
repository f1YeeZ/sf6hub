ALTER TABLE combos
    ADD COLUMN IF NOT EXISTS control_type VARCHAR(16) NOT NULL DEFAULT 'classic';

UPDATE combos
SET control_type = 'classic'
WHERE control_type IS NULL OR control_type = '';
