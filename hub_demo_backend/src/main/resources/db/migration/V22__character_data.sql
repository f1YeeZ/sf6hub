CREATE TABLE IF NOT EXISTS character_data (
    character_id BIGINT PRIMARY KEY REFERENCES characters(id) ON DELETE CASCADE,
    hp VARCHAR(40),
    throw_range VARCHAR(40),
    forward_walk_speed VARCHAR(40),
    back_walk_speed VARCHAR(40),
    forward_dash_speed VARCHAR(40),
    back_dash_speed VARCHAR(40),
    forward_dash_distance VARCHAR(40),
    back_dash_distance VARCHAR(40),
    jump_speed VARCHAR(40),
    forward_jump_distance VARCHAR(40),
    back_jump_distance VARCHAR(40),
    fastest_normal VARCHAR(40),
    source_name VARCHAR(80),
    source_url TEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO character_data (
    character_id, hp, throw_range, forward_walk_speed, back_walk_speed,
    forward_dash_speed, back_dash_speed, forward_dash_distance, back_dash_distance,
    jump_speed, forward_jump_distance, back_jump_distance, fastest_normal,
    source_name, source_url
)
SELECT
    c.id, seed.hp, seed.throw_range, seed.forward_walk_speed, seed.back_walk_speed,
    seed.forward_dash_speed, seed.back_dash_speed, seed.forward_dash_distance, seed.back_dash_distance,
    seed.jump_speed, seed.forward_jump_distance, seed.back_jump_distance, seed.fastest_normal,
    'SuperCombo / FAT',
    'https://wiki.supercombo.gg/w/Street_Fighter_6/' || seed.wiki_slug || '/Frame_data'
FROM (
    VALUES
        ('aki', 'A.K.I.', '10000', '0.8', '0.0452', '0.032', '19', '23', '1.300', '1.079', '4+40+3', '2.00', '1.60', '4f'),
        ('gouki_akuma', 'Akuma', '9000', '0.8', '0.052', '0.035', '19', '23', '1.352', '0.923', '4+38+3', '1.9', '1.52', '4f'),
        ('alex', 'Alex', '10500', '0.85', '0.042', '0.027', '22', '25', '1.0', '0.75', '4+38+3', '1.9', '1.52', '4f'),
        ('blanka', 'Blanka', '10000', '0.9', '0.047', '0.032', '19', '23', '1.578', '1.169', '4+38+3', '1.9', '1.52', '4f'),
        ('cviper', 'C.Viper', '10000', '0.8', '0.0452', '0.031', '21', '23', '1.5', '0.8', '4+38+3', '1.9 (3.00)', '1.52', '4f'),
        ('cammy', 'Cammy', '10000', '0.8', '0.0505', '0.033', '18', '23', '1.32', '1.002', '4+38+3', '1.9', '1.52', '4f'),
        ('chunli', 'Chun-Li', '10000', '0.8', '0.05', '0.035', '19', '25', '1.508', '1.211', '4+42+3', '2.1', '1.68', '4f'),
        ('deejay', 'Dee_Jay', '10000', '0.8', '0.043', '0.032', '19', '23', '1.5', '0.9', '4+38+3', '1.9', '1.52', '4f'),
        ('dhalsim', 'Dhalsim', '10000', '0.8', '0.028', '0.025', '25', '23', '1.467', '1', '4+68+3', '2.04', '1.768', '4f'),
        ('ehonda', 'E.Honda', '10500', '0.9', '0.045', '0.025', '19', '23', '1.058', '0.601', '4+38+3', '1.9', '1.52', '4f'),
        ('ed', 'Ed', '10000', '0.8', '0.0475', '0.032', '19', '23', '1.348', '0.803', '4+38+3', '1.9', '1.52', '4f'),
        ('elena', 'Elena', '10000', '0.8', '0.048', '0.033', '20', '23', '1.43', '1.139', '4+38+3', '1.9', '1.52', '4f'),
        ('guile', 'Guile', '10000', '0.8', '0.043', '0.032', '21', '23', '1.567', '0.74', '4+38+3', '1.9', '1.52', '4f'),
        ('ingrid', 'Ingrid', '10000', '0.8', '0.047', '0.032', '20', '23', '1.3', '0.9', '4+38+3', '1.9', '1.52', '4f'),
        ('jamie', 'Jamie', '10000', '0.8', '0.048', '0.035', '19', '23', '1.5', '0.85', '4+38+3', '1.9', '1.52', '4f'),
        ('jp', 'JP', '10000', '0.8', '0.037', '0.025', '22', '23', '1.454', '1.003', '4+38+3', '1.9', '1.52', '4f'),
        ('juri', 'Juri', '10000', '0.8', '0.047', '0.032', '22', '23', '1.903', '1.114', '4+38+3', '1.9', '1.52', '4f'),
        ('ken', 'Ken', '10000', '0.8', '0.047', '0.032', '19', '23', '1.322', '0.923', '4+38+3', '1.9', '1.52', '4f'),
        ('kimberly', 'Kimberly', '10000', '0.8', '0.0505 (0.0561)', '0.033 (0.0366)', '18', '23', '1.409', '0.893', '4+38+3', '1.9', '1.52', '4f'),
        ('lily', 'Lily', '10000', '0.8', '0.042', '0.027', '21', '24', '1.261', '0.94', '5+39+3', '1.95', '1.56', '4f'),
        ('luke', 'Luke', '10000', '0.8', '0.047', '0.032', '19', '23', '1.467', '0.751', '4+38+3', '1.9', '1.52', '4f'),
        ('vega_mbison', 'M.Bison', '10000', '0.8', '0.048', '0.0312', '19', '23', '1.54', '0.7537', '4+38+3', '1.9', '1.52', '4f'),
        ('mai', 'Mai', '10000', '0.8', '0.05', '0.035', '18', '23', '1.45', '0.9', '4+38+3', '1.9', '1.52', '4f'),
        ('manon', 'Manon', '10000', '0.8', '0.0452', '0.031', '21', '25', '1.499', '1.254', '4+38+3', '1.9', '1.52', '4f'),
        ('marisa', 'Marisa', '10500', '0.9', '0.039', '0.027', '22', '25', '1.4', '0.9', '4+38+3', '1.9', '1.52', '4f'),
        ('rashid', 'Rashid', '10000', '0.8', '0.045', '0.032', '18', '25', '1.2 (1.86)', '1.1 (1.905)', '4+38+3', '1.9 (5.07)', '1.52 (3.63)', '4f'),
        ('ryu', 'Ryu', '10000', '0.8', '0.047', '0.032', '19', '23', '1.252', '0.923', '4+38+3', '1.9', '1.52', '4f'),
        ('sagat', 'Sagat', '10000', '0.85', '0.039', '0.027', '23', '23', '1.4', '0.9', '4+38+3', '1.9', '1.52', '4f'),
        ('terry', 'Terry', '10000', '0.8', '0.048', '0.032', '19', '23', '1.5', '0.985', '4+38+3', '1.9', '1.52', '4f'),
        ('zangief', 'Zangief', '11000', '1.02', '0.0364', '0.025', '22', '25', '1.006', '0.712', '5+38+3', '1.725', '1.406', '4f')
) AS seed(
    official_slug, wiki_slug, hp, throw_range, forward_walk_speed, back_walk_speed,
    forward_dash_speed, back_dash_speed, forward_dash_distance, back_dash_distance,
    jump_speed, forward_jump_distance, back_jump_distance, fastest_normal
)
JOIN characters c ON c.official_slug = seed.official_slug
ON CONFLICT (character_id) DO NOTHING;
