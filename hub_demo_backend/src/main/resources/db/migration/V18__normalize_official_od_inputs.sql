UPDATE frame_data
SET move_name = REPLACE(
        REPLACE(move_name, 'LPMPHP', 'PP'),
        'LKMKHK', 'KK'
    )
WHERE COALESCE(manual_override, FALSE) = FALSE
  AND source_character_slug IS NOT NULL
  AND (
      move_name LIKE '%LPMPHP%'
      OR move_name LIKE '%LKMKHK%'
  );
