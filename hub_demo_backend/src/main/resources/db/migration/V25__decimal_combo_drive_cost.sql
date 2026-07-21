ALTER TABLE combos
    ALTER COLUMN drive_cost TYPE NUMERIC(2, 1) USING drive_cost::NUMERIC(2, 1),
    ALTER COLUMN drive_cost SET DEFAULT 0.0;
