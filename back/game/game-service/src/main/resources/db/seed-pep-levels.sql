-- Idempotent starter levels for the existing PEP fixed calculation question bank.
-- Run after content-service grade 1-6 seed scripts and game schema.sql.

BEGIN;

INSERT INTO game_level (
    id, code, name, description, grade_level, knowledge_point_id,
    difficulty, question_source, mode, question_count, time_limit_seconds,
    pass_rate, min_players, max_players, base_points, base_experience,
    previous_level_id, sort_order, status, rule_config, created_by
) VALUES
    (95000001, 'PEP-G01-CALC-START', '一年级计算起步', '一年级固定题库综合计算体验关',
     1, 9201010107, 2, 'CURATED', 'SOLO', 5, 300,
     0.8000, 1, 1, 50, 30, NULL, 1010, 'ENABLED',
     '{"stars":{"one":0.8,"two":0.9,"three":1.0},"comboEnergy":10}'::JSONB, 1),
    (95000002, 'PEP-G02-CALC-START', '二年级计算起步', '二年级乘除与混合计算体验关',
     2, 9202010204, 2, 'CURATED', 'SOLO', 5, 300,
     0.8000, 1, 1, 60, 35, NULL, 2010, 'ENABLED',
     '{"stars":{"one":0.8,"two":0.9,"three":1.0},"comboEnergy":10}'::JSONB, 1),
    (95000003, 'PEP-G03-CALC-START', '三年级计算起步', '三年级除法、乘法与分数计算体验关',
     3, 9203010201, 2, 'CURATED', 'SOLO', 5, 300,
     0.8000, 1, 1, 70, 40, NULL, 3010, 'ENABLED',
     '{"stars":{"one":0.8,"two":0.9,"three":1.0},"comboEnergy":10}'::JSONB, 1),
    (95000004, 'PEP-G04-CALC-START', '四年级计算起步', '四年级乘除与数量关系计算体验关',
     4, 9204010401, 3, 'CURATED', 'SOLO', 5, 360,
     0.8000, 1, 1, 80, 45, NULL, 4010, 'ENABLED',
     '{"stars":{"one":0.8,"two":0.9,"three":1.0},"comboEnergy":10}'::JSONB, 1),
    (95000005, 'PEP-G05-CALC-START', '五年级计算起步', '五年级小数乘除计算体验关',
     5, 9205010101, 3, 'CURATED', 'SOLO', 5, 360,
     0.8000, 1, 1, 90, 50, NULL, 5010, 'ENABLED',
     '{"stars":{"one":0.8,"two":0.9,"three":1.0},"comboEnergy":10}'::JSONB, 1),
    (95000006, 'PEP-G06-CALC-START', '六年级计算起步', '六年级分数乘除计算体验关',
     6, 9206010101, 3, 'CURATED', 'SOLO', 5, 360,
     0.8000, 1, 1, 100, 60, NULL, 6010, 'ENABLED',
     '{"stars":{"one":0.8,"two":0.9,"three":1.0},"comboEnergy":10}'::JSONB, 1)
ON CONFLICT (code) DO NOTHING;

INSERT INTO game_level_question (
    level_id, question_id, sequence_no, weight, required
) VALUES
    (95000001, 930101010001, 1, 1.000, TRUE),
    (95000001, 930101010002, 2, 1.000, TRUE),
    (95000001, 930101020001, 3, 1.000, TRUE),
    (95000001, 930101020002, 4, 1.000, TRUE),
    (95000001, 930101020003, 5, 1.000, TRUE),
    (95000002, 930201020001, 1, 1.000, TRUE),
    (95000002, 930201020002, 2, 1.000, TRUE),
    (95000002, 930201020003, 3, 1.000, TRUE),
    (95000002, 930201020004, 4, 1.000, TRUE),
    (95000002, 930201020005, 5, 1.000, TRUE),
    (95000003, 930301020001, 1, 1.000, TRUE),
    (95000003, 930301020002, 2, 1.000, TRUE),
    (95000003, 930301020003, 3, 1.000, TRUE),
    (95000003, 930301020004, 4, 1.000, TRUE),
    (95000003, 930301040001, 5, 1.000, TRUE),
    (95000004, 930401040001, 1, 1.000, TRUE),
    (95000004, 930401040002, 2, 1.000, TRUE),
    (95000004, 930401040003, 3, 1.000, TRUE),
    (95000004, 930401040004, 4, 1.000, TRUE),
    (95000004, 930401060001, 5, 1.000, TRUE),
    (95000005, 930501010001, 1, 1.000, TRUE),
    (95000005, 930501010002, 2, 1.000, TRUE),
    (95000005, 930501010003, 3, 1.000, TRUE),
    (95000005, 930501010004, 4, 1.000, TRUE),
    (95000005, 930501030001, 5, 1.000, TRUE),
    (95000006, 930601010001, 1, 1.000, TRUE),
    (95000006, 930601010002, 2, 1.000, TRUE),
    (95000006, 930601010003, 3, 1.000, TRUE),
    (95000006, 930601010004, 4, 1.000, TRUE),
    (95000006, 930601030001, 5, 1.000, TRUE)
ON CONFLICT (level_id, question_id) DO NOTHING;

COMMIT;

SELECT COUNT(*) AS enabled_starter_level_count
FROM game_level
WHERE code LIKE 'PEP-G%-CALC-START'
  AND status = 'ENABLED';
