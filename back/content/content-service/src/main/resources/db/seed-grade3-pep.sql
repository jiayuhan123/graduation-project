-- content_db 三年级人教版基础计算内容种子数据
-- 教材范围：人教版新教材三年级上、下册；只保留直接服务于数值计算的单元。
-- 目录参考：https://basic.smartedu.cn/syncClassroom
-- 使用：psql -v ON_ERROR_STOP=1 -d content_db -f content-service/src/main/resources/db/seed-grade3-pep.sql
-- 建议先按顺序执行 seed-grade1-pep.sql、seed-grade2-pep.sql。

BEGIN;

-- 1. 计算内容模块：5 个。排除观察物体、长度、线和角、图形面积、统计、实践与复习。
WITH seed (id, code, name, grade_level, semester, learning_theme, description, sort_order) AS (
    VALUES
        (91030102::BIGINT, 'PEP-G03-S01-U02', '混合运算', 3::SMALLINT, 1::SMALLINT, '数与运算',
         '掌握同级、两级和含小括号的混合运算顺序，并解决计算问题。', 20),
        (91030104::BIGINT, 'PEP-G03-S01-U04', '多位数乘一位数', 3::SMALLINT, 1::SMALLINT, '数与运算',
         '掌握多位数乘一位数的口算、笔算、估算及常见数量关系。', 40),
        (91030106::BIGINT, 'PEP-G03-S01-U06', '分数的初步认识与简单计算', 3::SMALLINT, 1::SMALLINT, '数与运算',
         '在初步认识分数的基础上进行大小比较和同分母分数简单加减。', 60),
        (91030202::BIGINT, 'PEP-G03-S02-U02', '除数是一位数的除法', 3::SMALLINT, 2::SMALLINT, '数与运算',
         '掌握一位数除两、三位数的口算、估算、笔算和实际应用。', 20),
        (91030206::BIGINT, 'PEP-G03-S02-U06', '小数的初步认识与简单计算', 3::SMALLINT, 2::SMALLINT, '数与运算',
         '在初步认识小数的基础上进行大小比较和一位小数简单加减。', 60)
)
INSERT INTO content_module (
    id, code, name, grade_level, semester, learning_theme, publisher,
    edition_description, description, sort_order, status, created_by
)
SELECT id, code, name, grade_level, semester, learning_theme, '人民教育出版社',
       '人教版新教材，根据2022年版课程标准修订', description, sort_order, 1, 0
FROM seed
ON CONFLICT (code) DO NOTHING;

-- 2. 原子知识点：34 个。
WITH seed (id, module_code, code, name, description, difficulty_level, sort_order) AS (
    VALUES
        -- 上册 U02：混合运算（6）
        (9203010201::BIGINT, 'PEP-G03-S01-U02', 'KP-PEP-G03-S01-U02-01', '同级混合运算', '按从左到右的顺序计算只含加减或只含乘除的算式。', 2::SMALLINT, 10),
        (9203010202::BIGINT, 'PEP-G03-S01-U02', 'KP-PEP-G03-S01-U02-02', '两级混合运算', '掌握先乘除后加减的运算顺序。', 2::SMALLINT, 20),
        (9203010203::BIGINT, 'PEP-G03-S01-U02', 'KP-PEP-G03-S01-U02-03', '含小括号的混合运算', '先计算小括号内的算式，再计算括号外。', 3::SMALLINT, 30),
        (9203010204::BIGINT, 'PEP-G03-S01-U02', 'KP-PEP-G03-S01-U02-04', '同级运算实际问题', '用同级混合运算解决两步实际问题。', 3::SMALLINT, 40),
        (9203010205::BIGINT, 'PEP-G03-S01-U02', 'KP-PEP-G03-S01-U02-05', '两级运算实际问题', '用乘除与加减两级运算解决实际问题。', 3::SMALLINT, 50),
        (9203010206::BIGINT, 'PEP-G03-S01-U02', 'KP-PEP-G03-S01-U02-06', '用不同算式解决同一问题', '能够比较分步、综合等不同解题思路。', 4::SMALLINT, 60),

        -- 上册 U04：多位数乘一位数（9）
        (9203010401::BIGINT, 'PEP-G03-S01-U04', 'KP-PEP-G03-S01-U04-01', '整十整百数乘一位数口算', '利用数的组成口算整十、整百数乘一位数。', 2::SMALLINT, 10),
        (9203010402::BIGINT, 'PEP-G03-S01-U04', 'KP-PEP-G03-S01-U04-02', '多位数乘一位数不进位笔算', '相同数位对齐，从个位起逐位相乘。', 2::SMALLINT, 20),
        (9203010403::BIGINT, 'PEP-G03-S01-U04', 'KP-PEP-G03-S01-U04-03', '多位数乘一位数一次进位笔算', '正确处理某一数位乘积满十的进位。', 3::SMALLINT, 30),
        (9203010404::BIGINT, 'PEP-G03-S01-U04', 'KP-PEP-G03-S01-U04-04', '多位数乘一位数连续进位笔算', '逐位计算并正确处理连续进位。', 4::SMALLINT, 40),
        (9203010405::BIGINT, 'PEP-G03-S01-U04', 'KP-PEP-G03-S01-U04-05', '0乘任何数', '掌握0和任何数相乘都得0。', 1::SMALLINT, 50),
        (9203010406::BIGINT, 'PEP-G03-S01-U04', 'KP-PEP-G03-S01-U04-06', '因数中间或末尾有0的乘法', '正确计算因数中间或末尾有0的多位数乘法。', 3::SMALLINT, 60),
        (9203010407::BIGINT, 'PEP-G03-S01-U04', 'KP-PEP-G03-S01-U04-07', '多位数乘一位数估算', '把多位数看作接近的整十、整百数估算乘积。', 3::SMALLINT, 70),
        (9203010408::BIGINT, 'PEP-G03-S01-U04', 'KP-PEP-G03-S01-U04-08', '单价数量总价关系', '运用单价乘数量等于总价解决问题。', 3::SMALLINT, 80),
        (9203010409::BIGINT, 'PEP-G03-S01-U04', 'KP-PEP-G03-S01-U04-09', '速度时间路程关系', '运用速度乘时间等于路程解决问题。', 3::SMALLINT, 90),

        -- 上册 U06：分数简单计算（5）
        (9203010601::BIGINT, 'PEP-G03-S01-U06', 'KP-PEP-G03-S01-U06-01', '简单分数大小比较', '比较同分母或分子为1的简单分数大小。', 2::SMALLINT, 10),
        (9203010602::BIGINT, 'PEP-G03-S01-U06', 'KP-PEP-G03-S01-U06-02', '同分母分数加法', '分母不变，分子相加，并理解结果含义。', 2::SMALLINT, 20),
        (9203010603::BIGINT, 'PEP-G03-S01-U06', 'KP-PEP-G03-S01-U06-03', '同分母分数减法', '分母不变，分子相减，并理解结果含义。', 2::SMALLINT, 30),
        (9203010604::BIGINT, 'PEP-G03-S01-U06', 'KP-PEP-G03-S01-U06-04', '1减几分之几', '把1改写成与减数同分母的分数后计算。', 3::SMALLINT, 40),
        (9203010605::BIGINT, 'PEP-G03-S01-U06', 'KP-PEP-G03-S01-U06-05', '分数简单计算实际问题', '用同分母分数加减解决简单实际问题。', 3::SMALLINT, 50),

        -- 下册 U02：除数是一位数的除法（9）
        (9203020201::BIGINT, 'PEP-G03-S02-U02', 'KP-PEP-G03-S02-U02-01', '整十整百数除以一位数口算', '利用数的组成和乘除关系口算。', 2::SMALLINT, 10),
        (9203020202::BIGINT, 'PEP-G03-S02-U02', 'KP-PEP-G03-S02-U02-02', '除数是一位数的除法估算', '把被除数看作接近且便于口算的数进行估算。', 3::SMALLINT, 20),
        (9203020203::BIGINT, 'PEP-G03-S02-U02', 'KP-PEP-G03-S02-U02-03', '两位数除以一位数笔算', '从最高位除起，正确确定每一位上的商。', 3::SMALLINT, 30),
        (9203020204::BIGINT, 'PEP-G03-S02-U02', 'KP-PEP-G03-S02-U02-04', '三位数除以一位数笔算', '掌握三位数除以一位数的一般笔算方法。', 3::SMALLINT, 40),
        (9203020205::BIGINT, 'PEP-G03-S02-U02', 'KP-PEP-G03-S02-U02-05', '被除数首位不够除', '首位不够除时从前两位除起并确定商的位数。', 4::SMALLINT, 50),
        (9203020206::BIGINT, 'PEP-G03-S02-U02', 'KP-PEP-G03-S02-U02-06', '商中间有0的除法', '某一位不够商1时在该位商0占位。', 4::SMALLINT, 60),
        (9203020207::BIGINT, 'PEP-G03-S02-U02', 'KP-PEP-G03-S02-U02-07', '商末尾有0的除法', '被除数末位不够商1时在商的末位写0。', 4::SMALLINT, 70),
        (9203020208::BIGINT, 'PEP-G03-S02-U02', 'KP-PEP-G03-S02-U02-08', '除法验算', '用商乘除数加余数等于被除数进行验算。', 3::SMALLINT, 80),
        (9203020209::BIGINT, 'PEP-G03-S02-U02', 'KP-PEP-G03-S02-U02-09', '除法两步实际问题', '用连除、乘除混合等方法解决两步问题。', 4::SMALLINT, 90),

        -- 下册 U06：小数简单计算（5）
        (9203020601::BIGINT, 'PEP-G03-S02-U06', 'KP-PEP-G03-S02-U06-01', '一位小数大小比较', '先比较整数部分，再比较十分位。', 2::SMALLINT, 10),
        (9203020602::BIGINT, 'PEP-G03-S02-U06', 'KP-PEP-G03-S02-U06-02', '一位小数加法', '小数点对齐后按整数加法方法计算。', 2::SMALLINT, 20),
        (9203020603::BIGINT, 'PEP-G03-S02-U06', 'KP-PEP-G03-S02-U06-03', '一位小数减法', '小数点对齐后按整数减法方法计算。', 2::SMALLINT, 30),
        (9203020604::BIGINT, 'PEP-G03-S02-U06', 'KP-PEP-G03-S02-U06-04', '整数与一位小数加减', '把整数小数点后的数位补0后对齐计算。', 3::SMALLINT, 40),
        (9203020605::BIGINT, 'PEP-G03-S02-U06', 'KP-PEP-G03-S02-U06-05', '小数加减实际问题', '用一位小数加减解决价格、长度等简单问题。', 3::SMALLINT, 50)
)
INSERT INTO knowledge_point (
    id, module_id, parent_id, code, name, description, difficulty_level, sort_order, status, created_by
)
SELECT seed.id, module.id, NULL, seed.code, seed.name, seed.description,
       seed.difficulty_level, seed.sort_order, 1, 0
FROM seed
JOIN content_module module ON module.code = seed.module_code
ON CONFLICT (code) DO NOTHING;

-- 3. 三年级专项错因：8 个；低年级通用错因继续复用。
WITH seed (id, code, name, category, applicable_question_type, description, explanation_template, remediation_hint) AS (
    VALUES
        (94000025::BIGINT, 'ORDER_OF_OPERATIONS_ERROR', '混合运算顺序错误', 'CALCULATION', 'ARITHMETIC', '没有按同级从左到右或先乘除后加减计算。', '先标出运算顺序，再逐步计算。', '练习判断每道混合算式的第一步。'),
        (94000026::BIGINT, 'PARENTHESES_PRIORITY_ERROR', '小括号优先级错误', 'CALCULATION', 'ARITHMETIC', '没有先计算小括号内的算式。', '小括号会改变运算顺序，应先算括号里面。', '先圈出括号并单独计算括号内结果。'),
        (94000027::BIGINT, 'MULTI_DIGIT_CARRY_ERROR', '多位数乘法进位错误', 'CARRY', 'VERTICAL', '多位数乘一位数时漏加或错加进位。', '每一位相乘后都要加上前一位的进位。', '在竖式旁标记每次进位数。'),
        (94000028::BIGINT, 'ZERO_FACTOR_PLACE_ERROR', '含0因数处理错误', 'PLACE_VALUE', 'VERTICAL', '因数中间或末尾有0时数位处理错误。', '先确认0所在数位，再按数位对齐计算。', '专项练习中间有0和末尾有0的乘法。'),
        (94000029::BIGINT, 'DIVISION_QUOTIENT_PLACE_ERROR', '除法商的数位错误', 'PLACE_VALUE', 'VERTICAL', '没有从最高位除起或商的位置写错。', '除到被除数的哪一位，商就写在那一位上。', '先判断商有几位，再列竖式。'),
        (94000030::BIGINT, 'ZERO_IN_QUOTIENT_ERROR', '商中间或末尾漏0', 'PLACE_VALUE', 'VERTICAL', '某一位不够商1时没有写0占位。', '这一位不够除也要在商中写0占位。', '练习商中间和末尾有0的除法。'),
        (94000031::BIGINT, 'SIMPLE_FRACTION_RULE_ERROR', '简单分数加减规则错误', 'CALCULATION', 'ARITHMETIC', '同分母分数加减时错误改变分母。', '同分母分数相加减，分母不变，只计算分子。', '用分数条验证计算结果。'),
        (94000032::BIGINT, 'DECIMAL_ALIGNMENT_ERROR', '小数点未对齐', 'FORMAT', 'VERTICAL', '小数加减时按末位而不是小数点对齐。', '小数点对齐就是相同数位对齐。', '列竖式时先写小数点，再填写数字。')
)
INSERT INTO error_type (
    id, code, name, category, applicable_question_type, description, explanation_template, remediation_hint, status
)
SELECT id, code, name, category, applicable_question_type, description, explanation_template, remediation_hint, 1
FROM seed
ON CONFLICT (code) DO NOTHING;

-- 4. 固定联调题：20 道，每个计算模块 4 道。
WITH seed (id, code, question_type, stem, answer_json, analysis, difficulty, estimated_seconds) AS (
    VALUES
        (930301020001::BIGINT, 'Q-PEP-G03-S01-U02-001', 'ARITHMETIC', '36 ÷ 6 × 4 = ?', '{"value":24}', '同级运算从左到右：36除以6等于6，6乘4等于24。', 2::SMALLINT, 15),
        (930301020002::BIGINT, 'Q-PEP-G03-S01-U02-002', 'ARITHMETIC', '18 + 5 × 6 = ?', '{"value":48}', '先算5乘6等于30，再加18等于48。', 2::SMALLINT, 15),
        (930301020003::BIGINT, 'Q-PEP-G03-S01-U02-003', 'ARITHMETIC', '(42 - 18) ÷ 6 = ?', '{"value":4}', '先算括号内42减18等于24，再除以6等于4。', 3::SMALLINT, 18),
        (930301020004::BIGINT, 'Q-PEP-G03-S01-U02-004', 'FILL_BLANK', '4盒球，每盒8个，又放入6个，一共有多少个？', '{"value":38}', '先算4乘8等于32，再加6等于38。', 3::SMALLINT, 25),

        (930301040001::BIGINT, 'Q-PEP-G03-S01-U04-001', 'ARITHMETIC', '300 × 6 = ?', '{"value":1800}', '3个百乘6等于18个百，即1800。', 2::SMALLINT, 12),
        (930301040002::BIGINT, 'Q-PEP-G03-S01-U04-002', 'VERTICAL', '列竖式计算：248 × 4 = ?', '{"value":992}', '逐位相乘并处理进位，结果是992。', 3::SMALLINT, 30),
        (930301040003::BIGINT, 'Q-PEP-G03-S01-U04-003', 'VERTICAL', '列竖式计算：306 × 7 = ?', '{"value":2142}', '因数中间有0仍要保持数位，结果是2142。', 4::SMALLINT, 35),
        (930301040004::BIGINT, 'Q-PEP-G03-S01-U04-004', 'FILL_BLANK', '每本练习册12元，买8本需要多少元？', '{"value":96}', '单价乘数量等于总价，12乘8等于96。', 3::SMALLINT, 22),

        (930301060001::BIGINT, 'Q-PEP-G03-S01-U06-001', 'FILL_BLANK', '比较大小：3/8 ? 5/8。应填 -1、0、1 中的哪一个？', '{"value":-1}', '同分母分数比较分子，3小于5，所以3/8小于5/8。', 2::SMALLINT, 15),
        (930301060002::BIGINT, 'Q-PEP-G03-S01-U06-002', 'ARITHMETIC', '2/7 + 3/7 = ?', '{"text":"5/7"}', '分母不变，分子2加3等于5。', 2::SMALLINT, 15),
        (930301060003::BIGINT, 'Q-PEP-G03-S01-U06-003', 'ARITHMETIC', '6/9 - 2/9 = ?', '{"text":"4/9"}', '分母不变，分子6减2等于4。', 2::SMALLINT, 15),
        (930301060004::BIGINT, 'Q-PEP-G03-S01-U06-004', 'ARITHMETIC', '1 - 3/5 = ?', '{"text":"2/5"}', '把1看作5/5，5/5减3/5等于2/5。', 3::SMALLINT, 18),

        (930302020001::BIGINT, 'Q-PEP-G03-S02-U02-001', 'ARITHMETIC', '600 ÷ 3 = ?', '{"value":200}', '6个百除以3等于2个百。', 2::SMALLINT, 12),
        (930302020002::BIGINT, 'Q-PEP-G03-S02-U02-002', 'VERTICAL', '列竖式计算：468 ÷ 3 = ?', '{"value":156}', '从百位除起，逐位计算，商是156。', 3::SMALLINT, 35),
        (930302020003::BIGINT, 'Q-PEP-G03-S02-U02-003', 'VERTICAL', '列竖式计算：804 ÷ 4 = ?', '{"value":201}', '十位不够商1时商0占位，结果是201。', 4::SMALLINT, 38),
        (930302020004::BIGINT, 'Q-PEP-G03-S02-U02-004', 'FILL_BLANK', '728本书平均放在7个书架上，每个书架放多少本？', '{"value":104}', '728除以7等于104。', 3::SMALLINT, 28),

        (930302060001::BIGINT, 'Q-PEP-G03-S02-U06-001', 'FILL_BLANK', '比较大小：3.7 ? 3.5。应填 -1、0、1 中的哪一个？', '{"value":1}', '整数部分相同，十分位7大于5，所以3.7大于3.5。', 2::SMALLINT, 12),
        (930302060002::BIGINT, 'Q-PEP-G03-S02-U06-002', 'ARITHMETIC', '2.6 + 1.8 = ?', '{"value":4.4}', '小数点对齐计算，结果是4.4。', 2::SMALLINT, 15),
        (930302060003::BIGINT, 'Q-PEP-G03-S02-U06-003', 'ARITHMETIC', '7.2 - 3.5 = ?', '{"value":3.7}', '小数点对齐计算，结果是3.7。', 2::SMALLINT, 15),
        (930302060004::BIGINT, 'Q-PEP-G03-S02-U06-004', 'VERTICAL', '列竖式计算：10 - 6.4 = ?', '{"value":3.6}', '把10写成10.0后小数点对齐，结果是3.6。', 3::SMALLINT, 22)
)
INSERT INTO question (
    id, code, question_mode, question_type, stem, options, standard_answer, analysis,
    generation_rule, difficulty, estimated_seconds, status, created_by, reviewed_by, reviewed_at
)
SELECT id, code, 'FIXED', question_type, stem, NULL, answer_json::JSONB, analysis,
       NULL, difficulty, estimated_seconds, 'ENABLED', 0, 0, CURRENT_TIMESTAMP
FROM seed
ON CONFLICT (code) DO NOTHING;

WITH seed (question_code, knowledge_point_code) AS (
    VALUES
        ('Q-PEP-G03-S01-U02-001', 'KP-PEP-G03-S01-U02-01'),
        ('Q-PEP-G03-S01-U02-002', 'KP-PEP-G03-S01-U02-02'),
        ('Q-PEP-G03-S01-U02-003', 'KP-PEP-G03-S01-U02-03'),
        ('Q-PEP-G03-S01-U02-004', 'KP-PEP-G03-S01-U02-05'),
        ('Q-PEP-G03-S01-U04-001', 'KP-PEP-G03-S01-U04-01'),
        ('Q-PEP-G03-S01-U04-002', 'KP-PEP-G03-S01-U04-04'),
        ('Q-PEP-G03-S01-U04-003', 'KP-PEP-G03-S01-U04-06'),
        ('Q-PEP-G03-S01-U04-004', 'KP-PEP-G03-S01-U04-08'),
        ('Q-PEP-G03-S01-U06-001', 'KP-PEP-G03-S01-U06-01'),
        ('Q-PEP-G03-S01-U06-002', 'KP-PEP-G03-S01-U06-02'),
        ('Q-PEP-G03-S01-U06-003', 'KP-PEP-G03-S01-U06-03'),
        ('Q-PEP-G03-S01-U06-004', 'KP-PEP-G03-S01-U06-04'),
        ('Q-PEP-G03-S02-U02-001', 'KP-PEP-G03-S02-U02-01'),
        ('Q-PEP-G03-S02-U02-002', 'KP-PEP-G03-S02-U02-04'),
        ('Q-PEP-G03-S02-U02-003', 'KP-PEP-G03-S02-U02-06'),
        ('Q-PEP-G03-S02-U02-004', 'KP-PEP-G03-S02-U02-09'),
        ('Q-PEP-G03-S02-U06-001', 'KP-PEP-G03-S02-U06-01'),
        ('Q-PEP-G03-S02-U06-002', 'KP-PEP-G03-S02-U06-02'),
        ('Q-PEP-G03-S02-U06-003', 'KP-PEP-G03-S02-U06-03'),
        ('Q-PEP-G03-S02-U06-004', 'KP-PEP-G03-S02-U06-04')
)
INSERT INTO question_knowledge_point (question_id, knowledge_point_id, is_primary, weight)
SELECT question.id, knowledge_point.id, TRUE, 1.0000
FROM seed
JOIN question ON question.code = seed.question_code
JOIN knowledge_point ON knowledge_point.code = seed.knowledge_point_code
ON CONFLICT (question_id, knowledge_point_id) DO NOTHING;

COMMIT;

SELECT COUNT(*) AS grade3_pep_module_count FROM content_module WHERE code LIKE 'PEP-G03-%';
SELECT COUNT(*) AS grade3_pep_knowledge_point_count FROM knowledge_point WHERE code LIKE 'KP-PEP-G03-%';
SELECT COUNT(*) AS grade3_pep_test_question_count FROM question WHERE code LIKE 'Q-PEP-G03-%';
SELECT COUNT(*) AS grade3_specific_error_type_count FROM error_type WHERE id BETWEEN 94000025 AND 94000032;
