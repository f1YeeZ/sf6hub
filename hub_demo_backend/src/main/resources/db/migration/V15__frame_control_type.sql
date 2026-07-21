ALTER TABLE frame_data
    ADD COLUMN IF NOT EXISTS control_type VARCHAR(20) NOT NULL DEFAULT 'classic';

UPDATE frame_data
SET control_type = 'classic'
WHERE control_type IS NULL OR control_type = '';

CREATE INDEX IF NOT EXISTS idx_frame_data_character_control
    ON frame_data(character_id, control_type, display_order, id);
