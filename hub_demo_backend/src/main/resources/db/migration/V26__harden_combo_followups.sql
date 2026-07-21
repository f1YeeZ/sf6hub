WITH ranked_followups AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY followup_combo_id
               ORDER BY created_at DESC, id DESC
           ) AS row_number
    FROM combo_followups
)
DELETE FROM combo_followups
WHERE id IN (
    SELECT id FROM ranked_followups WHERE row_number > 1
);

ALTER TABLE combo_followups
    ADD CONSTRAINT uk_combo_followups_followup UNIQUE (followup_combo_id);

WITH classified_followups AS (
    SELECT combo.id,
           CASE
               WHEN POSITION(',safe-jump,' IN ',' || COALESCE(combo.tags, '') || ',') > 0 THEN 'safe-jump'
               WHEN POSITION(',throw-pressure,' IN ',' || COALESCE(combo.tags, '') || ',') > 0 THEN 'throw-pressure'
               WHEN POSITION(',shimmy,' IN ',' || COALESCE(combo.tags, '') || ',') > 0 THEN 'shimmy'
               WHEN POSITION(',side-switch-pressure,' IN ',' || COALESCE(combo.tags, '') || ',') > 0 THEN 'side-switch-pressure'
               WHEN POSITION(',meaty-strike,' IN ',' || COALESCE(combo.tags, '') || ',') > 0 THEN 'meaty-strike'
               WHEN POSITION(',throw-starter,' IN ',' || COALESCE(combo.tags, '') || ',') > 0 THEN 'throw-pressure'
               WHEN POSITION(',side-switch,' IN ',' || COALESCE(combo.tags, '') || ',') > 0 THEN 'side-switch-pressure'
               ELSE 'meaty-strike'
           END AS pressure_type
    FROM combos combo
    INNER JOIN combo_followups followup ON followup.followup_combo_id = combo.id
)
UPDATE combos combo
SET type = classified.pressure_type,
    tags = classified.pressure_type
FROM classified_followups classified
WHERE combo.id = classified.id;
