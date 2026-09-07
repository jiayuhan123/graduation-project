-- content_db 六年级人教版基础计算内容种子数据
-- 教材范围：现行人教版六年级上、下册；排除位置、图形、统计、综合实践、数学广角和复习。
-- 目录参考：https://basic.smartedu.cn/syncClassroom
-- 使用：psql -v ON_ERROR_STOP=1 -d content_db -f content-service/src/main/resources/db/seed-grade6-pep.sql
-- 建议先按年级顺序执行低年级种子脚本。

BEGIN;

-- 1. 计算内容模块：6 个。
WITH seed (id, code, name, grade_level, semester, learning_theme, description, sort_order) AS (
    VALUES
        (91060101::BIGINT, 'PEP-G06-S01-U01', '分数乘法', 6::SMALLINT, 1::SMALLINT, '数与运算', '掌握分数乘整数、分数乘分数、分数小数混合、简算和应用。', 10),
        (91060103::BIGINT, 'PEP-G06-S01-U03', '分数除法', 6::SMALLINT, 1::SMALLINT, '数与运算', '掌握倒数、分数除法、混合运算和分数除法实际问题。', 30),
        (91060104::BIGINT, 'PEP-G06-S01-U04', '比的计算与应用', 6::SMALLINT, 1::SMALLINT, '比与比例', '掌握求比值、化简比和按比分配。', 40),
        (91060106::BIGINT, 'PEP-G06-S01-U06', '百分数（一）', 6::SMALLINT, 1::SMALLINT, '数与运算', '掌握百分数互化和求百分率、变化率等计算。', 60),
        (91060202::BIGINT, 'PEP-G06-S02-U02', '百分数（二）', 6::SMALLINT, 2::SMALLINT, '数与运算', '运用百分数计算折扣、成数、税率、利率和购物问题。', 20),
        (91060204::BIGINT, 'PEP-G06-S02-U04', '比例的计算与应用', 6::SMALLINT, 2::SMALLINT, '比与比例', '掌握比例性质、解比例、正反比例判断和比例应用。', 40)
)
INSERT INTO content_module (
    id, code, name, grade_level, semester, learning_theme, publisher,
    edition_description, description, sort_order, status, created_by
)
SELECT id, code, name, grade_level, semester, learning_theme, '人民教育出版社',
       '现行人教版义务教育教科书', description, sort_order, 1, 0
FROM seed ON CONFLICT (code) DO NOTHING;

-- 2. 原子知识点：39 个。
WITH seed (id, module_code, code, name, description, difficulty_level, sort_order) AS (
    VALUES
        -- 上册 U01：分数乘法（7）
        (9206010101::BIGINT, 'PEP-G06-S01-U01', 'KP-PEP-G06-S01-U01-01', '分数乘整数', '分子与整数相乘，能约分的先约分。', 2::SMALLINT, 10),
        (9206010102::BIGINT, 'PEP-G06-S01-U01', 'KP-PEP-G06-S01-U01-02', '分数乘分数', '分子相乘作分子，分母相乘作分母并约分。', 3::SMALLINT, 20),
        (9206010103::BIGINT, 'PEP-G06-S01-U01', 'KP-PEP-G06-S01-U01-03', '分数乘小数', '选择把小数化分数或直接约分进行计算。', 3::SMALLINT, 30),
        (9206010104::BIGINT, 'PEP-G06-S01-U01', 'KP-PEP-G06-S01-U01-04', '分数四则混合运算', '遵循四则运算顺序并及时约分。', 4::SMALLINT, 40),
        (9206010105::BIGINT, 'PEP-G06-S01-U01', 'KP-PEP-G06-S01-U01-05', '分数乘法简便计算', '把整数乘法运算律推广到分数。', 3::SMALLINT, 50),
        (9206010106::BIGINT, 'PEP-G06-S01-U01', 'KP-PEP-G06-S01-U01-06', '求一个数的几分之几', '用这个数乘指定分数求部分量。', 3::SMALLINT, 60),
        (9206010107::BIGINT, 'PEP-G06-S01-U01', 'KP-PEP-G06-S01-U01-07', '连续求一个数的几分之几', '按数量关系连续进行分数乘法。', 4::SMALLINT, 70),

        -- 上册 U03：分数除法（8）
        (9206010301::BIGINT, 'PEP-G06-S01-U03', 'KP-PEP-G06-S01-U03-01', '求一个数的倒数', '乘积为1的两个数互为倒数，0没有倒数。', 2::SMALLINT, 10),
        (9206010302::BIGINT, 'PEP-G06-S01-U03', 'KP-PEP-G06-S01-U03-02', '分数除以整数', '转化为乘这个整数的倒数并约分。', 3::SMALLINT, 20),
        (9206010303::BIGINT, 'PEP-G06-S01-U03', 'KP-PEP-G06-S01-U03-03', '一个数除以分数', '转化为乘除数的倒数。', 3::SMALLINT, 30),
        (9206010304::BIGINT, 'PEP-G06-S01-U03', 'KP-PEP-G06-S01-U03-04', '分数乘除混合运算', '把除法转化成乘法后按顺序计算。', 4::SMALLINT, 40),
        (9206010305::BIGINT, 'PEP-G06-S01-U03', 'KP-PEP-G06-S01-U03-05', '已知一个数的几分之几求这个数', '用部分量除以对应分率求单位1。', 3::SMALLINT, 50),
        (9206010306::BIGINT, 'PEP-G06-S01-U03', 'KP-PEP-G06-S01-U03-06', '已知增减分率求原数', '根据现量对应的分率列除法或方程。', 4::SMALLINT, 60),
        (9206010307::BIGINT, 'PEP-G06-S01-U03', 'KP-PEP-G06-S01-U03-07', '和差中的分数关系问题', '根据总量和两个量的分率关系求各量。', 4::SMALLINT, 70),
        (9206010308::BIGINT, 'PEP-G06-S01-U03', 'KP-PEP-G06-S01-U03-08', '用抽象的1解决工程问题', '把工作总量看作1，用工作效率和时间关系列式。', 5::SMALLINT, 80),

        -- 上册 U04：比（4）
        (9206010401::BIGINT, 'PEP-G06-S01-U04', 'KP-PEP-G06-S01-U04-01', '求比值', '用比的前项除以后项求比值。', 2::SMALLINT, 10),
        (9206010402::BIGINT, 'PEP-G06-S01-U04', 'KP-PEP-G06-S01-U04-02', '化简整数比', '依据比的基本性质把整数比化成最简整数比。', 2::SMALLINT, 20),
        (9206010403::BIGINT, 'PEP-G06-S01-U04', 'KP-PEP-G06-S01-U04-03', '化简分数比和小数比', '先转成整数比，再化成最简整数比。', 3::SMALLINT, 30),
        (9206010404::BIGINT, 'PEP-G06-S01-U04', 'KP-PEP-G06-S01-U04-04', '按比分配', '先求总份数，再按各部分所占份数分配总量。', 4::SMALLINT, 40),

        -- 上册 U06：百分数（一）（7）
        (9206010601::BIGINT, 'PEP-G06-S01-U06', 'KP-PEP-G06-S01-U06-01', '小数化百分数', '小数点向右移动两位并添百分号。', 2::SMALLINT, 10),
        (9206010602::BIGINT, 'PEP-G06-S01-U06', 'KP-PEP-G06-S01-U06-02', '百分数化小数', '去掉百分号并把小数点向左移动两位。', 2::SMALLINT, 20),
        (9206010603::BIGINT, 'PEP-G06-S01-U06', 'KP-PEP-G06-S01-U06-03', '分数与百分数互化', '通过小数或分母为100的分数完成互化。', 3::SMALLINT, 30),
        (9206010604::BIGINT, 'PEP-G06-S01-U06', 'KP-PEP-G06-S01-U06-04', '求百分率', '比较量除以标准量再化成百分数。', 3::SMALLINT, 40),
        (9206010605::BIGINT, 'PEP-G06-S01-U06', 'KP-PEP-G06-S01-U06-05', '求一个数比另一个数多或少百分之几', '先求差，再除以作为单位1的数量。', 4::SMALLINT, 50),
        (9206010606::BIGINT, 'PEP-G06-S01-U06', 'KP-PEP-G06-S01-U06-06', '求比一个数多或少百分之几的数', '用单位1乘一加或一减变化率。', 4::SMALLINT, 60),
        (9206010607::BIGINT, 'PEP-G06-S01-U06', 'KP-PEP-G06-S01-U06-07', '连续增减百分数', '每次变化都以上一次结果为新的单位1。', 5::SMALLINT, 70),

        -- 下册 U02：百分数（二）（5）
        (9206020201::BIGINT, 'PEP-G06-S02-U02', 'KP-PEP-G06-S02-U02-01', '折扣计算', '现价等于原价乘折扣率，并能逆求原价或折扣。', 3::SMALLINT, 10),
        (9206020202::BIGINT, 'PEP-G06-S02-U02', 'KP-PEP-G06-S02-U02-02', '成数计算', '把成数转化为百分数计算增减量或现量。', 3::SMALLINT, 20),
        (9206020203::BIGINT, 'PEP-G06-S02-U02', 'KP-PEP-G06-S02-U02-03', '税率计算', '应纳税额等于计税金额乘税率。', 3::SMALLINT, 30),
        (9206020204::BIGINT, 'PEP-G06-S02-U02', 'KP-PEP-G06-S02-U02-04', '利率计算', '利息等于本金乘利率乘存期。', 4::SMALLINT, 40),
        (9206020205::BIGINT, 'PEP-G06-S02-U02', 'KP-PEP-G06-S02-U02-05', '购物优惠方案比较', '分别计算不同优惠方案的实际支出并比较。', 4::SMALLINT, 50),

        -- 下册 U04：比例（8）
        (9206020401::BIGINT, 'PEP-G06-S02-U04', 'KP-PEP-G06-S02-U04-01', '判断比例是否成立', '比较两个比的比值或交叉乘积。', 2::SMALLINT, 10),
        (9206020402::BIGINT, 'PEP-G06-S02-U04', 'KP-PEP-G06-S02-U04-02', '比例的基本性质', '比例中两个外项积等于两个内项积。', 2::SMALLINT, 20),
        (9206020403::BIGINT, 'PEP-G06-S02-U04', 'KP-PEP-G06-S02-U04-03', '解整数比例', '利用比例基本性质转化为方程求未知项。', 3::SMALLINT, 30),
        (9206020404::BIGINT, 'PEP-G06-S02-U04', 'KP-PEP-G06-S02-U04-04', '解分数小数比例', '先进行必要的互化，再利用比例性质求解。', 4::SMALLINT, 40),
        (9206020405::BIGINT, 'PEP-G06-S02-U04', 'KP-PEP-G06-S02-U04-05', '判断正比例', '两种相关量的比值一定时成正比例。', 3::SMALLINT, 50),
        (9206020406::BIGINT, 'PEP-G06-S02-U04', 'KP-PEP-G06-S02-U04-06', '判断反比例', '两种相关量的乘积一定时成反比例。', 3::SMALLINT, 60),
        (9206020407::BIGINT, 'PEP-G06-S02-U04', 'KP-PEP-G06-S02-U04-07', '用正比例解决问题', '根据比值一定列比例求未知量。', 4::SMALLINT, 70),
        (9206020408::BIGINT, 'PEP-G06-S02-U04', 'KP-PEP-G06-S02-U04-08', '用反比例解决问题', '根据乘积一定列等式求未知量。', 4::SMALLINT, 80)
)
INSERT INTO knowledge_point (
    id, module_id, parent_id, code, name, description, difficulty_level, sort_order, status, created_by
)
SELECT seed.id, module.id, NULL, seed.code, seed.name, seed.description,
       seed.difficulty_level, seed.sort_order, 1, 0
FROM seed JOIN content_module module ON module.code = seed.module_code
ON CONFLICT (code) DO NOTHING;

-- 3. 六年级专项错因：10 个。
WITH seed (id, code, name, category, applicable_question_type, description, explanation_template, remediation_hint) AS (
    VALUES
        (94000052::BIGINT, 'FRACTION_MULTIPLICATION_RULE_ERROR', '分数乘法规则错误', 'CALCULATION', 'ARITHMETIC', '分数相乘时错误通分或错误计算分子分母。', '分数相乘是分子乘分子、分母乘分母，能约分先约分。', '用约分标记分解每一步。'),
        (94000053::BIGINT, 'RECIPROCAL_ERROR', '倒数判断错误', 'CONCEPT', 'FILL_BLANK', '颠倒带分数或小数时未先转化，或认为0有倒数。', '先化成分数再交换分子分母，0没有倒数。', '分类练习整数、分数、小数的倒数。'),
        (94000054::BIGINT, 'FRACTION_DIVISION_RULE_ERROR', '分数除法规则错误', 'CALCULATION', 'ARITHMETIC', '除法转乘法时没有使用除数的倒数。', '被除数不变，除号变乘号，除数变倒数。', '固定写出三步转换再计算。'),
        (94000055::BIGINT, 'UNIT_ONE_IDENTIFICATION_ERROR', '单位1识别错误', 'CONCEPT', 'FILL_BLANK', '分数或百分数问题中选错标准量。', '“谁的几分之几或百分之几”中的谁通常是单位1。', '先圈出比较标准，再画数量关系。'),
        (94000056::BIGINT, 'RATIO_SIMPLIFICATION_ERROR', '化简比错误', 'CALCULATION', 'FILL_BLANK', '只改变比的一项或把化简比写成比值。', '比的前项和后项同时乘或除以相同非0数。', '区分最简整数比与比值两种答案。'),
        (94000057::BIGINT, 'PERCENT_CONVERSION_ERROR', '百分数互化错误', 'PLACE_VALUE', 'FILL_BLANK', '百分号与小数点移动没有同步处理。', '化成百分数乘100并添百分号，反向则除以100。', '用双向箭头记忆互化规则。'),
        (94000058::BIGINT, 'PERCENT_BASE_QUANTITY_ERROR', '百分数标准量选择错误', 'CONCEPT', 'FILL_BLANK', '求变化率时分母选成比较量或差。', '百分之几的标准是谁，除数就用谁。', '用“差÷单位1”模板练习。'),
        (94000059::BIGINT, 'DISCOUNT_RATE_ERROR', '折扣成数转化错误', 'CALCULATION', 'FILL_BLANK', '把八折或二成等转化成错误百分数。', '八折是80%，二成是20%。', '先完成折扣、成数、百分数互化表。'),
        (94000060::BIGINT, 'TAX_INTEREST_FORMULA_ERROR', '税率利率公式错误', 'CALCULATION', 'FILL_BLANK', '税额与总额混淆，或利息漏乘存期。', '税额等于计税额乘税率；利息等于本金乘利率乘存期。', '列出已知量和单位后再代公式。'),
        (94000061::BIGINT, 'PROPORTION_CROSS_PRODUCT_ERROR', '比例交叉乘积错误', 'CALCULATION', 'FILL_BLANK', '内项外项识别或交叉相乘错误。', '先标出两个外项和两个内项，再令乘积相等。', '在比例式上画交叉线后列方程。')
)
INSERT INTO error_type (
    id, code, name, category, applicable_question_type, description, explanation_template, remediation_hint, status
)
SELECT id, code, name, category, applicable_question_type, description, explanation_template, remediation_hint, 1
FROM seed ON CONFLICT (code) DO NOTHING;

-- 4. 固定联调题：24 道。
WITH seed (id, code, question_type, stem, answer_json, analysis, difficulty, estimated_seconds) AS (
    VALUES
        (930601010001::BIGINT, 'Q-PEP-G06-S01-U01-001', 'ARITHMETIC', '5/8 × 24 = ?', '{"value":15}', '24与8约分后计算。', 2::SMALLINT, 18),
        (930601010002::BIGINT, 'Q-PEP-G06-S01-U01-002', 'ARITHMETIC', '7/12 × 18/35 = ?', '{"text":"3/10"}', '交叉约分后得到3/10。', 3::SMALLINT, 22),
        (930601010003::BIGINT, 'Q-PEP-G06-S01-U01-003', 'ARITHMETIC', '3/5 × 0.25 = ?', '{"value":0.15}', '3/5等于0.6，0.6乘0.25等于0.15。', 3::SMALLINT, 22),
        (930601010004::BIGINT, 'Q-PEP-G06-S01-U01-004', 'FILL_BLANK', '80的3/5是多少？', '{"value":48}', '80乘3/5等于48。', 3::SMALLINT, 20),

        (930601030001::BIGINT, 'Q-PEP-G06-S01-U03-001', 'FILL_BLANK', '2又1/3的倒数是多少？', '{"text":"3/7"}', '先化成7/3，再交换分子分母。', 3::SMALLINT, 18),
        (930601030002::BIGINT, 'Q-PEP-G06-S01-U03-002', 'ARITHMETIC', '5/6 ÷ 10/9 = ?', '{"text":"3/4"}', '乘10/9的倒数9/10并约分。', 3::SMALLINT, 22),
        (930601030003::BIGINT, 'Q-PEP-G06-S01-U03-003', 'ARITHMETIC', '3/4 ÷ 2 × 8/9 = ?', '{"text":"1/3"}', '除以2转成乘1/2，再从左到右计算。', 4::SMALLINT, 25),
        (930601030004::BIGINT, 'Q-PEP-G06-S01-U03-004', 'FILL_BLANK', '一个数的3/8是27，这个数是多少？', '{"value":72}', '27除以3/8等于72。', 3::SMALLINT, 22),

        (930601040001::BIGINT, 'Q-PEP-G06-S01-U04-001', 'FILL_BLANK', '求比值：18:24。', '{"value":0.75}', '18除以24等于0.75。', 2::SMALLINT, 15),
        (930601040002::BIGINT, 'Q-PEP-G06-S01-U04-002', 'FILL_BLANK', '把42:56化成最简整数比。', '{"text":"3:4"}', '前后项同时除以14。', 2::SMALLINT, 18),
        (930601040003::BIGINT, 'Q-PEP-G06-S01-U04-003', 'FILL_BLANK', '把0.6:1.5化成最简整数比。', '{"text":"2:5"}', '同时乘10得到6:15，再除以3。', 3::SMALLINT, 20),
        (930601040004::BIGINT, 'Q-PEP-G06-S01-U04-004', 'FILL_BLANK', '把84按3:4分配，较小的一份是多少？', '{"value":36}', '总份数7，每份12，较小部分3份是36。', 4::SMALLINT, 25),

        (930601060001::BIGINT, 'Q-PEP-G06-S01-U06-001', 'FILL_BLANK', '0.375化成百分数是多少？答案填写百分号前的数。', '{"value":37.5}', '小数点向右移动两位并添百分号。', 2::SMALLINT, 15),
        (930601060002::BIGINT, 'Q-PEP-G06-S01-U06-002', 'FILL_BLANK', '3/8化成百分数是多少？答案填写百分号前的数。', '{"value":37.5}', '3除以8等于0.375，即37.5%。', 3::SMALLINT, 18),
        (930601060003::BIGINT, 'Q-PEP-G06-S01-U06-003', 'FILL_BLANK', '40是50的百分之几？答案填写百分号前的数。', '{"value":80}', '40除以50等于0.8，即80%。', 3::SMALLINT, 20),
        (930601060004::BIGINT, 'Q-PEP-G06-S01-U06-004', 'FILL_BLANK', '60比50多百分之几？答案填写百分号前的数。', '{"value":20}', '差10除以单位1的量50等于20%。', 4::SMALLINT, 22),

        (930602020001::BIGINT, 'Q-PEP-G06-S02-U02-001', 'FILL_BLANK', '一件商品原价240元，打八五折后多少元？', '{"value":204}', '240乘85%等于204。', 3::SMALLINT, 22),
        (930602020002::BIGINT, 'Q-PEP-G06-S02-U02-002', 'FILL_BLANK', '某产量为400吨，增产二成后是多少吨？', '{"value":480}', '二成是20%，400乘120%等于480。', 3::SMALLINT, 22),
        (930602020003::BIGINT, 'Q-PEP-G06-S02-U02-003', 'FILL_BLANK', '计税金额5000元，税率3%，应纳税额多少元？', '{"value":150}', '5000乘3%等于150。', 3::SMALLINT, 22),
        (930602020004::BIGINT, 'Q-PEP-G06-S02-U02-004', 'FILL_BLANK', '本金10000元，年利率2%，存2年，利息多少元？', '{"value":400}', '10000乘2%乘2等于400。', 4::SMALLINT, 25),

        (930602040001::BIGINT, 'Q-PEP-G06-S02-U04-001', 'JUDGEMENT', '判断：3:5与12:20可以组成比例。', '{"value":true}', '3乘20等于5乘12，比例成立。', 2::SMALLINT, 16),
        (930602040002::BIGINT, 'Q-PEP-G06-S02-U04-002', 'FILL_BLANK', '解比例：x:12 = 5:3。x = ?', '{"value":20}', '3x等于12乘5，所以x等于20。', 3::SMALLINT, 22),
        (930602040003::BIGINT, 'Q-PEP-G06-S02-U04-003', 'FILL_BLANK', '4支笔12元，照这样买10支需要多少元？', '{"value":30}', '总价与数量成正比例，单价为3元。', 4::SMALLINT, 25),
        (930602040004::BIGINT, 'Q-PEP-G06-S02-U04-004', 'FILL_BLANK', '一项工程，6人做10天完成，若效率相同，12人几天完成？', '{"value":5}', '人数与天数成反比例，总工作量为60人天。', 4::SMALLINT, 28)
)
INSERT INTO question (
    id, code, question_mode, question_type, stem, options, standard_answer, analysis,
    generation_rule, difficulty, estimated_seconds, status, created_by, reviewed_by, reviewed_at
)
SELECT id, code, 'FIXED', question_type, stem, NULL, answer_json::JSONB, analysis,
       NULL, difficulty, estimated_seconds, 'ENABLED', 0, 0, CURRENT_TIMESTAMP
FROM seed ON CONFLICT (code) DO NOTHING;

WITH seed (question_code, knowledge_point_code) AS (
    VALUES
        ('Q-PEP-G06-S01-U01-001', 'KP-PEP-G06-S01-U01-01'), ('Q-PEP-G06-S01-U01-002', 'KP-PEP-G06-S01-U01-02'),
        ('Q-PEP-G06-S01-U01-003', 'KP-PEP-G06-S01-U01-03'), ('Q-PEP-G06-S01-U01-004', 'KP-PEP-G06-S01-U01-06'),
        ('Q-PEP-G06-S01-U03-001', 'KP-PEP-G06-S01-U03-01'), ('Q-PEP-G06-S01-U03-002', 'KP-PEP-G06-S01-U03-03'),
        ('Q-PEP-G06-S01-U03-003', 'KP-PEP-G06-S01-U03-04'), ('Q-PEP-G06-S01-U03-004', 'KP-PEP-G06-S01-U03-05'),
        ('Q-PEP-G06-S01-U04-001', 'KP-PEP-G06-S01-U04-01'), ('Q-PEP-G06-S01-U04-002', 'KP-PEP-G06-S01-U04-02'),
        ('Q-PEP-G06-S01-U04-003', 'KP-PEP-G06-S01-U04-03'), ('Q-PEP-G06-S01-U04-004', 'KP-PEP-G06-S01-U04-04'),
        ('Q-PEP-G06-S01-U06-001', 'KP-PEP-G06-S01-U06-01'), ('Q-PEP-G06-S01-U06-002', 'KP-PEP-G06-S01-U06-03'),
        ('Q-PEP-G06-S01-U06-003', 'KP-PEP-G06-S01-U06-04'), ('Q-PEP-G06-S01-U06-004', 'KP-PEP-G06-S01-U06-05'),
        ('Q-PEP-G06-S02-U02-001', 'KP-PEP-G06-S02-U02-01'), ('Q-PEP-G06-S02-U02-002', 'KP-PEP-G06-S02-U02-02'),
        ('Q-PEP-G06-S02-U02-003', 'KP-PEP-G06-S02-U02-03'), ('Q-PEP-G06-S02-U02-004', 'KP-PEP-G06-S02-U02-04'),
        ('Q-PEP-G06-S02-U04-001', 'KP-PEP-G06-S02-U04-01'), ('Q-PEP-G06-S02-U04-002', 'KP-PEP-G06-S02-U04-03'),
        ('Q-PEP-G06-S02-U04-003', 'KP-PEP-G06-S02-U04-07'), ('Q-PEP-G06-S02-U04-004', 'KP-PEP-G06-S02-U04-08')
)
INSERT INTO question_knowledge_point (question_id, knowledge_point_id, is_primary, weight)
SELECT question.id, knowledge_point.id, TRUE, 1.0000
FROM seed JOIN question ON question.code = seed.question_code
JOIN knowledge_point ON knowledge_point.code = seed.knowledge_point_code
ON CONFLICT (question_id, knowledge_point_id) DO NOTHING;

COMMIT;

SELECT COUNT(*) AS grade6_pep_module_count FROM content_module WHERE code LIKE 'PEP-G06-%';
SELECT COUNT(*) AS grade6_pep_knowledge_point_count FROM knowledge_point WHERE code LIKE 'KP-PEP-G06-%';
SELECT COUNT(*) AS grade6_pep_test_question_count FROM question WHERE code LIKE 'Q-PEP-G06-%';
SELECT COUNT(*) AS grade6_specific_error_type_count FROM error_type WHERE id BETWEEN 94000052 AND 94000061;
