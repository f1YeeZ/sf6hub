UPDATE frame_data
SET move_name = REGEXP_REPLACE(
        REGEXP_REPLACE(
            move_name,
            '4[[:space:]]*溜[[:space:]]*め[[:space:]]*([+＋][[:space:]]*)?6',
            'CHARGE4 + 6',
            'g'
        ),
        '2[[:space:]]*溜[[:space:]]*め[[:space:]]*([+＋][[:space:]]*)?8',
        'CHARGE2 + 8',
        'g'
    )
WHERE COALESCE(manual_override, FALSE) = FALSE
  AND source_character_slug IS NOT NULL
  AND (
      move_name ~ '4[[:space:]]*溜[[:space:]]*め'
      OR move_name ~ '2[[:space:]]*溜[[:space:]]*め'
  );
